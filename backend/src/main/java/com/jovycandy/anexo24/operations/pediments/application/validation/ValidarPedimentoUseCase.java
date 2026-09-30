package com.jovycandy.anexo24.operations.pediments.application.validation;

import com.jovycandy.anexo24.operations.pediments.domain.model.CargaPedimentoArchivo;
import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoError;
import com.jovycandy.anexo24.operations.pediments.domain.port.PedimentoReglasRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

import java.util.List;


/** Agrega reglas de negocio read-only al resultado de staging sin mutar datos legacy. */
@Service
public class ValidarPedimentoUseCase {
    private final PedimentoReglasRepository repository;

    public ValidarPedimentoUseCase(PedimentoReglasRepository repository) {
        this.repository = repository;
    }

    /**
     * Valida el lote completo en una consulta batch y evita duplicar errores ya producidos.
     *
     * @param archivo resultado estructural del parser
     * @return archivo con errores estructurales y de negocio combinados
     */
    public CargaPedimentoArchivo ejecutar(CargaPedimentoArchivo archivo) {
        if (archivo.filas().isEmpty()) return archivo;
        List<PedimentoError> errores = new ArrayList<>(archivo.errores());
        java.util.Set<String> existentes = errores.stream().map(this::clave).collect(java.util.stream.Collectors.toSet());
        for (PedimentoError error : repository.validar(archivo.filas())) {
            if (existentes.add(clave(error))) errores.add(error);
        }
        return new CargaPedimentoArchivo(archivo.nombre(), archivo.hash(), archivo.versionPlantilla(),
                archivo.columnas(), archivo.filas(), List.copyOf(errores), archivo.fallida());
    }

    private String clave(PedimentoError error) {
        return String.valueOf(error.hoja()) + "|" + error.fila() + "|" + error.columna() + "|" + error.codigo();
    }
}
