package com.jovycandy.anexo24.catalogs.materials.domain.model;

import java.math.BigDecimal;

/**
 * Material del catálogo de insumos (Módulo C).
 *
 * <p>Lectura parametrizada de {@code dbo.material}; solo las columnas
 * necesarias para el catálogo de la aplicación (RF-010).</p>
 *
 * @param materialkey   identificador del material
 * @param clave         clave interna del material
 * @param descripcion   descripción comercial
 * @param fraccion      fracción arancelaria
 * @param unidad        unidad comercial
 * @param unidadt       unidad tarifa
 * @param tipomaterial  tipo de material
 * @param tipo          tipo corto
 * @param factorUM      factor de conversión unidad
 * @param igie          IGIE aplicable
 */
public record Material(
        BigDecimal materialkey,
        String clave,
        String descripcion,
        String fraccion,
        String unidad,
        String unidadt,
        String tipomaterial,
        String tipo,
        BigDecimal factorUM,
        BigDecimal igie) {
}