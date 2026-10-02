#!/usr/bin/env python3
"""Pruebas del gate de deriva runtime ↔ permisos SQL.

Ejecuta scripts/check-runtime-sql-permissions.py contra fixtures PASS y FAIL:

- pass         -> coincidencia exacta -> PASS
- missing      -> falta un grant en ANEXO24_DEV -> FAIL missing
- extra        -> grant sobrante en ANEXO24_DEV -> FAIL extra
- missing_cale -> falta un grant en CALE_IMMEX -> FAIL missing
- schema_grant -> GRANT EXECUTE ON SCHEMA:: -> FAIL schema_grant
- table_grant  -> GRANT de DML directo -> FAIL grant_directo
- verify_mismatch -> listas de 07-runtime-security-verify.sql desalineadas -> FAIL
- historical_allowed_duplicate -> duplicado histórico canónico -> PASS
- historical_extra_sp -> SP fuera del contrato -> FAIL
- historical_schema_execute -> schema EXECUTE -> FAIL
- historical_table_select -> DML directo -> FAIL
- direct_runtime_user_grant -> grant directo a anexo24_app -> FAIL
- historical_fixed_role -> ADD MEMBER a rol fijo -> FAIL

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
VERIFY_FILE = "07-runtime-security-verify.sql"
REPO_SCAN = FIXTURES / "repo_scan"


def ejecutar(caso: str, sql_root: Path | None = None, base: str | None = None) -> tuple[int, str]:
    directorio = FIXTURES / caso
    base_dir = FIXTURES / base if base else directorio
    resultado = subprocess.run(
        [
            sys.executable,
            str(SCANNER),
            "--java-root", str(JAVA_FIXTURE),
            "--app-script", str(base_dir / APP_FILE),
            "--cale-script", str(base_dir / CALE_FILE),
            "--verify-script", str(base_dir / VERIFY_FILE),
            "--sql-root", str(sql_root or directorio),
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
    revisar("pass verify07 APP", "verify_script=2|missing=0|extra=0" in out, out)
    revisar("pass verify07 CALE", "verify_script=1|missing=0|extra=0" in out, out)

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

    rc, out = ejecutar("verify_mismatch")
    revisar("verify_mismatch rc=1", rc == 1, out)
    revisar("verify_mismatch detectado",
            "script=07-runtime-security-verify.sql" in out and "missing|database=ANEXO24_DEV" in out, out)

    rc, out = ejecutar("historical_allowed_duplicate", sql_root=REPO_SCAN / "historical_allowed_duplicate", base="pass")
    revisar("historical_allowed_duplicate rc=0", rc == 0, out)
    revisar("historical_allowed_duplicate findings=0", "RUNTIME_PERMISSION_REPO_SCAN|" in out and "findings=0" in out, out)

    rc, out = ejecutar("historical_extra_sp", sql_root=REPO_SCAN / "historical_extra_sp", base="pass")
    revisar("historical_extra_sp rc=1", rc == 1, out)
    revisar("historical_extra_sp detectado", "execute_sp_fuera_contrato" in out, out)

    rc, out = ejecutar("historical_schema_execute", sql_root=REPO_SCAN / "historical_schema_execute", base="pass")
    revisar("historical_schema_execute rc=1", rc == 1, out)
    revisar("historical_schema_execute detectado", "schema_execute" in out, out)

    rc, out = ejecutar("historical_table_select", sql_root=REPO_SCAN / "historical_table_select", base="pass")
    revisar("historical_table_select rc=1", rc == 1, out)
    revisar("historical_table_select detectado", "grant_no_execute" in out, out)

    rc, out = ejecutar("direct_runtime_user_grant", sql_root=REPO_SCAN / "direct_runtime_user_grant", base="pass")
    revisar("direct_runtime_user_grant rc=1", rc == 1, out)
    revisar("direct_runtime_user_grant detectado", "execute_directo_usuario" in out, out)

    rc, out = ejecutar("historical_fixed_role", sql_root=REPO_SCAN / "historical_fixed_role", base="pass")
    revisar("historical_fixed_role rc=1", rc == 1, out)
    revisar("historical_fixed_role detectado", "rol_fijo_add_member" in out, out)

    if fallos:
        for fallo in fallos:
            print(f"RESULT_FAIL|{fallo}")
        print("RUNTIME_PERMISSION_TESTS|FAIL")
        return 1
    print("RUNTIME_PERMISSION_TESTS|PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
