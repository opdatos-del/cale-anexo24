package com.jovycandy.anexo24.operations.pediments.domain.port;

import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoError;
import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoFila;

import java.util.List;

/** Consulta reglas de negocio read-only para un lote de pedimentos. */
public interface PedimentoReglasRepository {
    List<PedimentoError> validar(List<PedimentoFila> filas);
}
