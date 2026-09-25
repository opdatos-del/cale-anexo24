package com.jovycandy.anexo24.billing.domain.port;

import com.jovycandy.anexo24.billing.domain.model.PlantillaFacturacion;

import java.util.Optional;

public interface PlantillaFacturacionRepository {
    Optional<PlantillaFacturacion> findActive();
}
