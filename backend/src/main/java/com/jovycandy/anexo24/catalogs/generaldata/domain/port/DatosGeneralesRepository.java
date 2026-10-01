package com.jovycandy.anexo24.catalogs.generaldata.domain.port;

import com.jovycandy.anexo24.catalogs.generaldata.domain.model.DatosGenerales;

import java.util.Optional;

/** Puerto de consulta read-only de los datos generales empresariales. */
public interface DatosGeneralesRepository {
    Optional<DatosGenerales> find();
}
