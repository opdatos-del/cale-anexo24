#!/usr/bin/env python3
"""Gate de deriva runtime ↔ permisos SQL (RUNTIME IDENTITY HARDENING).

Comprueba que los `GRANT EXECUTE` por objeto de los scripts versionados cubren
exactamente los stored procedures invocados desde Java, sin faltantes ni
sobrantes:

- ANEXO24_DEV: SP `app24.*` de Java  ==  04-app-runtime-permissions.sql
- CALE_IMMEX:  SP `dbo.*` de Java    ==  05-cale-immex-runtime-permissions.sql

No es una migración de datos ni de base: sólo valida la metadata de seguridad
de la identidad runtime (`anexo24_app`).

Reglas duras (cualquier aparición falla el gate):
- `GRANT EXECUTE ON SCHEMA::...` (nada de schema-level).
- `GRANT` de DML/DDL directo (SELECT/INSERT/UPDATE/DELETE/ALTER/CONTROL/...).
- `ALTER ROLE db_owner|db_datareader|db_datawriter|db_ddladmin ADD MEMBER`.

`SystemStatusController` (`SELECT 1` técnico) no se considera: la extracción
sólo mira archivos que invocan SP vía `prepareCall("{call ...")` y toma los
literales cualificados `"<esquema>.<SP>"` (constantes, ternarios o métodos
auxiliares). Limitación deliberadamente fail-closed: un literal de SP dentro
de un archivo con `prepareCall` se exige aunque no llegue a ejecutarse.

Uso:
    python scripts/check-runtime-sql-permissions.py
    python scripts/check-runtime-sql-permissions.py --java-root R --app-script F --cale-script F

Salida (parseable):
    RUNTIME_PERMISSION|ANEXO24_DEV|java=<n>|script=<n>|missing=<n>|extra=<n>
    RUNTIME_PERMISSION_GATE|PASS
    RUNTIME_PERMISSION_GATE|FAIL|database=<db>|missing=<...>|extra=<...>|issue=<...>

Sale con código 1 si hay cualquier violación; 0 si todo está alineado.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
APP_SCHEMA = "app24"
CALE_SCHEMA = "dbo"

LITERAL_SP = re.compile(r'"((?:app24|dbo)\.[A-Za-z0-9_]+)"')

GRANT_OBJ = re.compile(
    r"GRANT\s+EXECUTE\s+ON\s+OBJECT::([A-Za-z0-9_]+)\.([A-Za-z0-9_]+)\s+TO\s+([A-Za-z0-9_]+)",
    re.IGNORECASE,
)
SCHEMA_GRANT = re.compile(r"GRANT\s+EXECUTE\s+ON\s+SCHEMA::", re.IGNORECASE)
DML_GRANT = re.compile(
    r"\bGRANT\s+(SELECT|INSERT|UPDATE|DELETE|MERGE|ALTER|CONTROL|REFERENCES|TAKE\s+OWNERSHIP|VIEW\s+DEFINITION)\b",
    re.IGNORECASE,
)
FIXED_ROLE_ADD = re.compile(
    r"ALTER\s+ROLE\s+(db_owner|db_datareader|db_datawriter|db_ddladmin)\s+ADD\s+MEMBER",
    re.IGNORECASE,
)


def sin_comentarios(texto: str) -> str:
    """Elimina comentarios SQL (-- y /* */) preservando el resto del texto."""
    texto = re.sub(r"/\*.*?\*/", " ", texto, flags=re.DOTALL)
    return re.sub(r"--[^\n]*", " ", texto)


def sin_comentarios_java(texto: str) -> str:
    """Elimina comentarios Java (// y /* */) preservando el resto del texto."""
    texto = re.sub(r"/\*.*?\*/", " ", texto, flags=re.DOTALL)
    return re.sub(r"//[^\n]*", " ", texto)


def sp_invocados_por_java(java_root: Path) -> tuple[set[str], set[str], list[str]]:
    """Devuelve (set_cale, set_app, errores) de SP consumidos por los adapters.

    Sólo se inspeccionan archivos que construyen llamadas con `prepareCall` y
    `{call`; de ellos se toman los literales cualificados `app24.*` / `dbo.*`
    (cubren constantes, ternarios y métodos auxiliares que devuelven el nombre).
    """
    cale: set[str] = set()
    app: set[str] = set()
    errores: list[str] = []
    for archivo in sorted(java_root.rglob("*.java")):
        texto = sin_comentarios_java(archivo.read_text(encoding="utf-8", errors="replace"))
        if "prepareCall" not in texto:
            continue
        if "{call" not in texto:
            errores.append(f"prepareCall_sin_call|archivo={archivo.name}")
            continue
        for valor in sorted(set(LITERAL_SP.findall(texto))):
            esquema, _, sp = valor.partition(".")
            if esquema == APP_SCHEMA:
                app.add(sp)
            elif esquema == CALE_SCHEMA:
                cale.add(sp)
    return cale, app, errores


def grants_de_script(archivo: Path, esquema: str, rol: str) -> set[str]:
    """Extrae los objetos con GRANT EXECUTE ON OBJECT::<esquema>.<SP> TO <rol>."""
    texto = sin_comentarios(archivo.read_text(encoding="utf-8", errors="replace"))
    encontrados = set()
    for coincidencia in GRANT_OBJ.finditer(texto):
        if coincidencia.group(1).lower() == esquema and coincidencia.group(3) == rol:
            encontrados.add(coincidencia.group(2))
    return encontrados


def violaciones_estructurales(archivo: Path) -> list[str]:
    """Detecta schema grants, DML directo o memberships fijas prohibidas."""
    texto = sin_comentarios(archivo.read_text(encoding="utf-8", errors="replace"))
    problemas: list[str] = []
    if SCHEMA_GRANT.search(texto):
        problemas.append(f"schema_grant|archivo={archivo.name}")
    for coincidencia in DML_GRANT.finditer(texto):
        problemas.append(f"grant_directo_{coincidencia.group(1).upper()}|archivo={archivo.name}")
    for coincidencia in FIXED_ROLE_ADD.finditer(texto):
        problemas.append(f"rol_fijo_add_member_{coincidencia.group(1).lower()}|archivo={archivo.name}")
    return problemas


def comparar(db: str, esperados: set[str], presentes: set[str]) -> tuple[str, list[str]]:
    faltantes = sorted(esperados - presentes)
    sobrantes = sorted(presentes - esperados)
    linea = (
        f"RUNTIME_PERMISSION|{db}|java={len(esperados)}|script={len(presentes)}"
        f"|missing={len(faltantes)}|extra={len(sobrantes)}"
    )
    return linea, faltantes, sobrantes


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Gate de deriva runtime ↔ permisos SQL.")
    parser.add_argument("--java-root", type=Path, default=REPO_ROOT / "backend" / "src" / "main" / "java")
    parser.add_argument("--app-script", type=Path, default=REPO_ROOT / "infra" / "sql" / "04-app-runtime-permissions.sql")
    parser.add_argument("--cale-script", type=Path, default=REPO_ROOT / "infra" / "sql" / "05-cale-immex-runtime-permissions.sql")
    args = parser.parse_args(argv)

    fallos: list[str] = []
    if not args.java_root.is_dir():
        fallos.append(f"java_root_inexistente|ruta={args.java_root}")
        cale_java: set[str] = set()
        app_java: set[str] = set()
    else:
        cale_java, app_java, errores_java = sp_invocados_por_java(args.java_root)
        fallos.extend(errores_java)

    app_script: set[str] = set()
    cale_script: set[str] = set()
    for archivo, destino, esquema, rol in (
        (args.app_script, "app_script", APP_SCHEMA, "app24_runtime"),
        (args.cale_script, "cale_script", CALE_SCHEMA, "cale_immex_runtime"),
    ):
        if not archivo.is_file():
            fallos.append(f"script_inexistente|archivo={archivo.name}")
            continue
        fallos.extend(violaciones_estructurales(archivo))
        if destino == "app_script":
            app_script = grants_de_script(archivo, esquema, rol)
        else:
            cale_script = grants_de_script(archivo, esquema, rol)

    linea_app, faltan_app, sobran_app = comparar("ANEXO24_DEV", app_java, app_script)
    linea_cale, faltan_cale, sobran_cale = comparar("CALE_IMMEX", cale_java, cale_script)
    print(linea_app)
    print(linea_cale)

    for db, faltantes, sobrantes in (
        ("ANEXO24_DEV", faltan_app, sobran_app),
        ("CALE_IMMEX", faltan_cale, sobran_cale),
    ):
        if faltantes:
            fallos.append(f"missing|database={db}|{' ,'.join(faltantes)}")
        if sobrantes:
            fallos.append(f"extra|database={db}|{', '.join(sobrantes)}")

    if fallos:
        for fallo in fallos:
            print(f"RUNTIME_PERMISSION_GATE|FAIL|{fallo}")
        return 1
    print("RUNTIME_PERMISSION_GATE|PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
