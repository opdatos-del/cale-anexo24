#!/usr/bin/env python3
"""Gate SP-FIRST: detecta SQL funcional inline y acceso JDBC directo en Java productivo.

Política del proyecto: DATABASE_ACCESS_POLICY = SP_FIRST_STRICT.
Todo acceso funcional a SQL Server pasa por Stored Procedure. Java no contiene
SQL funcional inline ni accede a tablas/vistas directamente.

Uso:
    python scripts/check-inline-sql.py [root]
    # root por defecto: backend/src/main/java

Sale con código 1 si encuentra violaciones; 0 si todo está limpio.
Sólo usa la librería estándar (STANDARD_LIBRARY_ONLY).

Limitaciones conocidas (documentadas en docs/04-arquitectura/auditoria-sp-first.md):
- No reconstruye SQL concatenado entre literales ("SEL" + "ECT ...").
- No interpreta variables ni constantes referenciadas.
- Las invocaciones de SP con `prepareCall` / `jdbcTemplate.call` se consideran
  legítimas y no se marcan.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

SQL_KEYWORDS = re.compile(
    r"\b(SELECT|INSERT|UPDATE|DELETE|MERGE|WITH|FROM|JOIN|ALTER|CREATE|DROP|TRUNCATE)\b",
    re.IGNORECASE,
)
JDBC_DIRECT = re.compile(
    r"\.\s*(query|queryForObject|queryForList|update|batchUpdate|execute)\s*\("
)
JDBC_STATEMENT = re.compile(r"\.\s*(createStatement|prepareStatement)\s*\(")

# Invocación legítima de SP a través de JdbcTemplate.query(PreparedStatementCreator, mapper).
SP_CALL_HINT = re.compile(r"prepareCall")

# Allowlist mínima, explícita y por método: nombre de archivo -> métodos -> reglas.
# Un archivo allowlisted NO habilita SQL arbitrario; sólo el método y literal exactos.
ALLOWLIST: dict[str, dict[str, dict[str, object]]] = {
    "SystemStatusController.java": {
        "checkDatabase": {"sql": {"SELECT 1"}, "jdbc": True},
    },
}

DEFAULT_ROOT = "backend/src/main/java"


def looks_like_sql(literal: str) -> bool:
    """Heurística: sólo literales que parecen una sentencia SQL, no verbos HTTP."""
    s = literal.strip()
    if not s or not SQL_KEYWORDS.search(s):
        return False
    # Un literal de una sola palabra (p. ej. el verbo HTTP "DELETE" de CORS) no
    # es SQL. Se exige más de un token o un operador/paréntesis.
    return len(s.split()) >= 2 or bool(re.search(r"[;,()=*]", s))


def analyze(text: str) -> tuple[str, list[tuple[str, int]]]:
    """Devuelve (código con strings/comentarios en blanco, literales con offset).

    `code` conserva la misma longitud que `text` para que los índices de los
    matches coincidan con los del archivo original.
    """
    code: list[str] = []
    literals: list[tuple[str, int]] = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            j = text.find("\n", i)
            end = n if j == -1 else j
            code.append(" " * (end - i))
            i = end
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "*":
            j = text.find("*/", i + 2)
            end = n if j == -1 else j + 2
            code.append(" " * (end - i))
            i = end
            continue
        if text.startswith('"""', i):
            j = text.find('"""', i + 3)
            end = n if j == -1 else j + 3
            literals.append((text[i + 3 : j if j != -1 else n], i + 3))
            code.append(" " * (end - i))
            i = end
            continue
        if c == '"':
            j, buf = i + 1, []
            while j < n:
                if text[j] == "\\":
                    if j + 1 < n:
                        buf.append(text[j + 1])
                    j += 2
                    continue
                if text[j] == '"':
                    break
                buf.append(text[j])
                j += 1
            end = j + 1
            literals.append(("".join(buf), i + 1))
            code.append(" " * (end - i))
            i = end
            continue
        if c == "'":
            j = i + 1
            while j < n and text[j] != "'":
                if text[j] == "\\":
                    j += 1
                j += 1
            end = j + 1
            code.append(" " * (end - i))
            i = end
            continue
        code.append(c)
        i += 1
    return "".join(code), literals


def method_span(code: str, name: str) -> tuple[int, int] | None:
    """Devuelve el rango [inicio, fin) del cuerpo del método `name`, o None."""
    match = re.search(r"\b" + re.escape(name) + r"\s*\(", code)
    if not match:
        return None
    brace = code.find("{", match.end())
    if brace == -1:
        return None
    depth = 0
    for idx in range(brace, len(code)):
        if code[idx] == "{":
            depth += 1
        elif code[idx] == "}":
            depth -= 1
            if depth == 0:
                return (match.start(), idx + 1)
    return (match.start(), len(code))


def in_span(pos: int, span: tuple[int, int] | None) -> bool:
    return span is not None and span[0] <= pos < span[1]


def check_file(path: Path) -> list[tuple[str, str]]:
    text = path.read_text(encoding="utf-8", errors="replace")
    code, literals = analyze(text)
    rules = ALLOWLIST.get(path.name, {})
    spans = {name: method_span(code, name) for name in rules}
    violations: list[tuple[str, str]] = []

    def allowed_sql(literal: str, pos: int) -> bool:
        for name, rule in rules.items():
            if literal in set(rule.get("sql", set())) and in_span(pos, spans.get(name)):
                return True
        return False

    def allowed_jdbc(pos: int) -> bool:
        for name, rule in rules.items():
            if rule.get("jdbc") and in_span(pos, spans.get(name)):
                return True
        return False

    for literal, pos in literals:
        stripped = literal.strip()
        if not looks_like_sql(stripped):
            continue
        if allowed_sql(stripped, pos):
            continue
        violations.append(("INLINE_SQL", stripped[:120]))

    for match in JDBC_DIRECT.finditer(code):
        if allowed_jdbc(match.start()):
            continue
        window = code[match.start() : match.start() + 400].split(";", 1)[0]
        if SP_CALL_HINT.search(window):
            continue
        violations.append(("DIRECT_JDBC", match.group(0).strip()))

    for match in JDBC_STATEMENT.finditer(code):
        if allowed_jdbc(match.start()):
            continue
        violations.append(("DIRECT_JDBC", match.group(0).strip()))

    return violations


def iter_java_files(root: Path):
    return sorted(p for p in root.rglob("*.java") if p.is_file())


def main(argv: list[str]) -> int:
    root = Path(argv[1]) if len(argv) > 1 else Path(DEFAULT_ROOT)
    if not root.exists():
        print(f"SP_FIRST_GATE_SKIPPED|root no existe: {root}")
        return 0

    total = 0
    for path in iter_java_files(root):
        for kind, snippet in check_file(path):
            total += 1
            try:
                rel = path.relative_to(Path.cwd()).as_posix()
            except ValueError:
                rel = path.as_posix()
            print(f"VIOLATION|{kind}|{rel}|{snippet}")

    if total:
        print(f"SP_FIRST_GATE|FAIL|violations={total}")
        return 1
    print("SP_FIRST_GATE|PASS|violations=0")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
