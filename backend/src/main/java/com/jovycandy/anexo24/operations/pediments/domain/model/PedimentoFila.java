package com.jovycandy.anexo24.operations.pediments.domain.model;

import java.util.Map;

/** Fila normalizada del contrato de staging de pedimentos. */
public record PedimentoFila(String hoja, int numero, Map<String, String> datos) {
}
