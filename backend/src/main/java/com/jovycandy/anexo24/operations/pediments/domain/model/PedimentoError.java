package com.jovycandy.anexo24.operations.pediments.domain.model;

/** Error de validación sin persistir el valor empresarial completo. */
public record PedimentoError(String hoja, Integer fila, String columna, String valorEnmascarado,
                             String codigo, String mensaje) {
}
