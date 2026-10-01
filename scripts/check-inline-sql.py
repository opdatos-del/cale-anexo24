#!/usr/bin/env python3
"""Gate SP-FIRST: detecta SQL funcional inline y acceso JDBC directo en Java productivo.

Política del proyecto: DATABASE_ACCESS_POLICY = SP_FIRST_STRICT.
Todo acceso funcional a SQL Server pasa por Stored Procedure. Java no contiene
SQL funcional inline ni accede a tablas/vistas directamente.

Uso:
    python scripts/check-inline-sql.py [root]
    # root por defecto: backend/src/main/java

Sale con código 1 si encuentra violaciones; 0 si todo está limpio.
No requiere dependencias externas.
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

# Una invocación de SP con JdbcTemplate.query(PreparedStatementCreator, mapper) es
# legítima: no es SQL funcional inline.
SP_CALL_HINT = re.compile(r"prepareCall")


def looks_like_sql(literal: str) -> bool:
    """Heurística: sólo literales que parecen una sentencia SQL, no verbos HTTP."""
    s = literal.strip()
    if not s or not SQL_KEYWORDS.search(s):
        return False
    # Un literal de una sola palabra (p. ej. el verbo HTTP "DELETE" de CORS) no
    # es SQL. Se exige más de un token o un operador/paréntesis.
    return len(s.split()) >= 2 or bool(re.search(r"[;,()=*]", s))

# Allowlist mínima y explícita: por nombre de archivo.
# - sql: literales exactos permitidos.
# - jdbc: permite acceso JDBC directo en ese archivo.
ALLOWLIST: dict[str, dict[str, object]] = {
    "SystemStatusController.java": {"sql": {"SELECT 1"}, "jdbc": True},
}

DEFAULT_ROOT = "backend/src/main/java"


def analyze(text: str) -> tuple[str, list[str]]:
    """Devuelve (código sin strings ni comentarios, lista de literales de texto)."""
    code: list[str] = []
    literals: list[str] = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            j = text.find("\n", i)
            i = n if j == -1 else j
            code.append(" ")
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "*":
            j = text.find("*/", i + 2)
            i = n if j == -1 else j + 2
            code.append(" ")
            continue
        if text.startswith('"""', i):
            j = text.find('"""', i + 3)
            literals.append(text[i + 3 : j if j != -1 else n])
            code.append(' "" ')
            i = n if j == -1 else j + 3
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
            literals.append("".join(buf))
            code.append(' "" ')
            i = j + 1
            continue
        if c == "'":
            j = i + 1
            while j < n and text[j] != "'":
                if text[j] == "\\":
                    j += 1
                j += 1
            i = j + 1
            code.append(" ")
            continue
        code.append(c)
        i += 1
    return "".join(code), literals


def check_file(path: Path) -> list[tuple[str, str]]:
    text = path.read_text(encoding="utf-8", errors="replace")
    code, literals = analyze(text)
    rules = ALLOWLIST.get(path.name, {})
    allowed_sql = set(rules.get("sql", set()))  # type: ignore[arg-type]
    allow_jdbc = bool(rules.get("jdbc"))
    violations: list[tuple[str, str]] = []

    for literal in literals:
        stripped = literal.strip()
        if not looks_like_sql(stripped):
            continue
        if stripped in allowed_sql:
            continue
        violations.append(("INLINE_SQL", stripped[:120]))

    if not allow_jdbc:
        for match in JDBC_DIRECT.finditer(code):
            window = code[match.start() : match.start() + 400].split(";", 1)[0]
            if SP_CALL_HINT.search(window):
                continue
            violations.append(("DIRECT_JDBC", match.group(0).strip()))
        for match in JDBC_STATEMENT.finditer(code):
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
                rel = path.relative_to(Path.cwd())
            except ValueError:
                rel = path
            print(f"VIOLATION|{kind}|{rel}|{snippet}")

    if total:
        print(f"SP_FIRST_GATE|FAIL|violations={total}")
        return 1
    print("SP_FIRST_GATE|PASS|violations=0")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
