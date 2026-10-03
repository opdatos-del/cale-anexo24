# Regeneración del borrador DOCX del reporte de estadía

## Requisitos

- Python 3 con `python-docx` (`python -m pip install python-docx`).
- Node con dependencias del frontend instaladas (para el render de diagramas).
- Plantilla institucional **R-ADC-08-07 Reporte de estadía Nivel ING DGS.docx**
  (no se versiona en este repositorio; solicitar el archivo oficial).

## Pasos

1. Colocar la plantilla en una ruta local (por ejemplo `tmp/plantilla.docx`).
2. Renderizar los diagramas HTML a PNG:

   ```bash
   python -m http.server 4600 --bind 127.0.0.1        # terminal A (raíz del repo)
   node scripts/report/render-diagrams.mjs             # terminal B
   ```

   Genera `docs/08-reporte-estadia/anexos/figuras/arquitectura-general.png` y
   `pipeline-carga-validada.png`.

3. Verificar que existan las capturas sanitizadas en
   `docs/08-reporte-estadia/anexos/capturas/` (ver `anexos-candidatos.md`).
4. Generar el DOCX:

   ```bash
   python scripts/report/generate_docx.py --template tmp/plantilla.docx
   ```

   Salida por defecto:
   `docs/08-reporte-estadia/R-ADC-08-07_Reporte_Estadia_Borrador_V1.docx`.

5. Abrir en Word y actualizar el campo de tabla de contenido (F9) la primera
   vez. El índice se genera automáticamente con los estilos Heading 1/2.

## Fuentes

- Contenido: Markdown consolidados en `docs/08-reporte-estadia/`.
- Capturas: `docs/08-reporte-estadia/anexos/capturas/` (sanitizadas).
- Fixtures: `docs/08-reporte-estadia/anexos/fixtures/` (100 % sintéticos).
- Diagramas: `diagramas/*.html` (no se modifican; sólo se renderizan).

## Notas

- El script no contiene rutas absolutas; todo se resuelve desde la raíz del
  repositorio.
- `ACADEMIC_DRAFT=YES` y `FINAL_BUSINESS_CONCLUSION_PENDING=YES` quedan
  registrados en las propiedades del documento.
- La portada conserva los campos de nombres de estudiantes y asesores como
  “pendiente de completar” hasta que exista el dato real (no se inventan).
