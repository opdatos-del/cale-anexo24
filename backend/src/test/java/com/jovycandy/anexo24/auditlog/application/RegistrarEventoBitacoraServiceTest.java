package com.jovycandy.anexo24.auditlog.application;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.auditlog.domain.port.BitacoraEventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Pruebas del caso de uso interno append-only de Bitácora. */
@ExtendWith(MockitoExtension.class)
class RegistrarEventoBitacoraServiceTest {

    @Mock
    private BitacoraEventoRepository repository;

    @Test
    void registraEventoUnaVezEnElPuerto() {
        BitacoraEvento evento = eventoValido();
        RegistrarEventoBitacoraService service = new RegistrarEventoBitacoraService(repository);

        service.registrar(evento);

        verify(repository).registrar(evento);
    }

    @Test
    void modeloRechazaEventoInvalidoAntesDePersistir() {
        assertThatIllegalArgumentException().isThrownBy(() -> new BitacoraEvento(
                1L,
                BitacoraModulo.SEGURIDAD,
                BitacoraAccion.LOGIN_OK,
                BitacoraResultado.EXITO,
                "a".repeat(501),
                null));

        verifyNoInteractions(repository);
    }

    private BitacoraEvento eventoValido() {
        return new BitacoraEvento(
                42L,
                BitacoraModulo.SEGURIDAD,
                BitacoraAccion.LOGIN_OK,
                BitacoraResultado.EXITO,
                "Acceso autenticado",
                "req-01");
    }
}
