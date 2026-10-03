#!/usr/bin/env python3
"""Gate de contexto de base para los comandos versionados.

Los SP `dbo.*` deben declarar explícitamente `USE [CALE_IMMEX]` y los
`app24.*` deben declarar `USE [ANEXO24_DEV]`. Sin ese pragma, el SP se
crea en el `USE` heredado por la herramienta que aplique el script y
resulta invisible desde una conexión JDBC que use el nombre de la BD
correcto. Esto fue la causa raíz de un bug de visibilidad que invalidó
el IT de confirmación de productos.

Uso:
    python scripts/check-command-database-context.py

Sale con código 1 si encuentra violaciones; 0 si todo está alineado.
Sólo usa la librería estándar (STANDARD_LIBRARY_ONLY).
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
COMMANDS = REPO_ROOT / "infra" / "sql" / "procedures" / "commands"

CALE_USE = re.compile(r"^\s*USE\s+\[?CALE_IMMEX\]?\s*;?\s*$", re.IGNORECASE | re.MULTILINE)
APP_USE = re.compile(r"^\s*USE\s+\[?ANEXO24_DEV\]?\s*;?\s*$", re.IGNORECASE | re.MULTILINE)
PROC_HEADER = re.compile(
    r"\bCREATE\s+(?:OR\s+ALTER\s+)?PROCEDURE\s+(?P<schema>[A-Za-z0-9_]+)\.(?P<name>[A-Za-z0-9_]+)",
    re.IGNORECASE,
)


def find_first_use(text: str, pattern: re.Pattern[str]) -> int:
    """Devuelve el offset del primer match del patrón o -1 si no existe."""
    match = pattern.search(text)
    return match.start() if match else -1


def main() -> int:
    fallos: list[str] = []
    archivos = sorted(COMMANDS.glob("*.sql"))
    for archivo in archivos:
        texto = archivo.read_text(encoding="utf-8", errors="replace")
        match = PROC_HEADER.search(texto)
        if not match:
            continue
        schema = match.group("schema").lower()
        if schema == "dbo":
            if find_first_use(texto, CALE_USE) < 0:
                fallos.append(
                    f"missing_cale_use|archivo={archivo.relative_to(REPO_ROOT).as_posix()}"
                )
        elif schema == "app24":
            if find_first_use(texto, APP_USE) < 0:
                fallos.append(
                    f"missing_app_use|archivo={archivo.relative_to(REPO_ROOT).as_posix()}"
                )
        else:
            fallos.append(
                f"schema_desconocido|archivo={archivo.relative_to(REPO_ROOT).as_posix()}|schema={schema}"
            )

    if fallos:
        for fallo in fallos:
            print(f"DATABASE_CONTEXT_GATE|FAIL|{fallo}")
        return 1
    print(f"DATABASE_CONTEXT_GATE|PASS|archivos={len(archivos)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())