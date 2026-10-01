package com.jovycandy.anexo24.catalogs.generaldata.application.query;

import com.jovycandy.anexo24.catalogs.generaldata.domain.model.DatosGenerales;
import com.jovycandy.anexo24.catalogs.generaldata.domain.port.DatosGeneralesRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Caso de uso de consulta única de datos generales, sin edición. */
@Service
public class ObtenerDatosGeneralesUseCase {
    private final DatosGeneralesRepository repository;

    public ObtenerDatosGeneralesUseCase(DatosGeneralesRepository repository) {
        this.repository = repository;
    }

    /**
     * Obtiene el único registro empresarial permitido por el contrato legacy.
     *
     * @return datos generales cuando existe una fila; vacío cuando la fuente está vacía
     */
    public Optional<DatosGenerales> ejecutar() {
        return repository.find();
    }
}
