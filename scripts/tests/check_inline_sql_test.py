#!/usr/bin/env python3
"""Pruebas del gate SP-FIRST.

Ejecuta scripts/check-inline-sql.py contra fixtures PASS y FAIL y valida el
resultado. Sin dependencias externas (STANDARD_LIBRARY_ONLY).

Uso:
    python3 scripts/tests/check_inline_sql_test.py
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]  # raíz del repo
SCANNER = ROOT / "scripts" / "check-inline-sql.py"
FIXTURES = HERE / "inline_sql" / "fixtures"

EXPECTED_FAIL_FILES = {
    "SelectFromTable.java",
    "InlineUpdate.java",
    "JdbcOnly.java",
    "StringBuilderSql.java",
    "MergeStatement.java",
    "InsertStatement.java",
    "DeleteStatement.java",
    "ViewSelect.java",
    "TextBlockSql.java",
    # Allowlist bypass: archivo allowlisted con SQL arbitrario adicional.
    "allowlist/SystemStatusController.java",
}


def run(root: Path) -> tuple[int, str]:
    result = subprocess.run(
        [sys.executable, str(SCANNER), str(root)],
        capture_output=True,
        text=True,
    )
    return result.returncode, result.stdout


def main() -> int:
    failures: list[str] = []

    rc_pass, out_pass = run(FIXTURES / "pass")
    if rc_pass != 0:
        failures.append(f"PASS esperado pero rc={rc_pass}\n{out_pass}")

    rc_fail, out_fail = run(FIXTURES / "fail")
    if rc_fail != 1:
        failures.append(f"FAIL esperado pero rc={rc_fail}\n{out_fail}")
    for name in EXPECTED_FAIL_FILES:
        if name not in out_fail:
            failures.append(f"Falta violación para {name}")

    if failures:
        print("CHECKER_TESTS|FAIL")
        for failure in failures:
            print(failure)
        return 1
    print("CHECKER_TESTS|PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
