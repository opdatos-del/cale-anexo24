#!/usr/bin/env python3
"""Pruebas del gate de deriva runtime ↔ permisos SQL.

Ejecuta scripts/check-runtime-sql-permissions.py contra fixtures PASS y FAIL:

- pass         -> coincidencia exacta -> PASS
- missing      -> falta un grant en ANEXO24_DEV -> FAIL missing
- extra        -> grant sobrante en ANEXO24_DEV -> FAIL extra
- missing_cale -> falta un grant en CALE_IMMEX -> FAIL missing
- schema_grant -> GRANT EXECUTE ON SCHEMA:: -> FAIL schema_grant
- table_grant  -> GRANT de DML directo -> FAIL grant_directo

Sin dependencias externas (STANDARD_LIBRARY_ONLY).

Uso:
    python scripts/tests/check_runtime_sql_permissions_test.py
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
SCANNER = ROOT / "scripts" / "check-runtime-sql-permissions.py"
FIXTURES = HERE / "runtime_permissions" / "fixtures"
JAVA_FIXTURE = FIXTURES / "java"
APP_FILE = "04-app-runtime-permissions.sql"
CALE_FILE = "05-cale-immex-runtime-permissions.sql"


def ejecutar(caso: str) -> tuple[int, str]:
    directorio = FIXTURES / caso
    resultado = subprocess.run(
        [
            sys.executable,
            str(SCANNER),
            "--java-root", str(JAVA_FIXTURE),
            "--app-script", str(directorio / APP_FILE),
            "--cale-script", str(directorio / CALE_FILE),
        ],
        capture_output=True,
        text=True,
    )
    return resultado.returncode, resultado.stdout


def main() -> int:
    fallos: list[str] = []

    def revisar(nombre: str, condicion: bool, detalle: str) -> None:
        if condicion:
            print(f"OK   {nombre}")
        else:
            fallos.append(f"{nombre} :: {detalle.strip()}")

    rc, out = ejecutar("pass")
    revisar("pass rc=0", rc == 0, out)
    revisar("pass marker", "RUNTIME_PERMISSION_GATE|PASS" in out, out)
    revisar("pass coincidencia exacta", "java=2|script=2|missing=0|extra=0" in out, out)

    rc, out = ejecutar("missing")
    revisar("missing rc=1", rc == 1, out)
    revisar("missing detecta APP24_Q_DEMO_ERRORES",
            "missing|database=ANEXO24_DEV" in out and "APP24_Q_DEMO_ERRORES" in out, out)

    rc, out = ejecutar("extra")
    revisar("extra rc=1", rc == 1, out)
    revisar("extra detecta APP24_Q_DEMO_EXTRA",
            "extra|database=ANEXO24_DEV" in out and "APP24_Q_DEMO_EXTRA" in out, out)

    rc, out = ejecutar("missing_cale")
    revisar("missing_cale rc=1", rc == 1, out)
    revisar("missing_cale detecta CALE_IMMEX",
            "missing|database=CALE_IMMEX" in out and "APP24_Q_DEMO_LISTAR" in out, out)

    rc, out = ejecutar("schema_grant")
    revisar("schema_grant rc=1", rc == 1, out)
    revisar("schema_grant detectado", "schema_grant" in out, out)

    rc, out = ejecutar("table_grant")
    revisar("table_grant rc=1", rc == 1, out)
    revisar("table_grant detectado", "grant_directo_SELECT" in out, out)

    if fallos:
        for fallo in fallos:
            print(f"RESULT_FAIL|{fallo}")
        print("RUNTIME_PERMISSION_TESTS|FAIL")
        return 1
    print("RUNTIME_PERMISSION_TESTS|PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
