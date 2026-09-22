package com.jovycandy.anexo24.auditlog.api;

import com.jovycandy.anexo24.auditlog.api.dto.BitacoraRegistroDto;
import com.jovycandy.anexo24.auditlog.application.query.ListarBitacoraUseCase;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraRegistro;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del endpoint read-only de Bitácora. */
@ExtendWith(MockitoExtension.class)
class BitacoraControllerTest {

    @Mock
    private ListarBitacoraUseCase listarBitacoraUseCase;

    @Test
    void convierteOffsetDateTimeAInstantYPreservaPaginaDto() {
        OffsetDateTime desde = OffsetDateTime.parse("2026-09-22T07:00:00-06:00");
        OffsetDateTime hasta = OffsetDateTime.parse("2026-09-22T08:00:00-06:00");
        BitacoraRegistro registro = new BitacoraRegistro(
                9L,
                Instant.parse("2026-09-22T13:30:00Z"),
                42L,
                "operador",
                BitacoraModulo.SEGURIDAD,
                BitacoraAccion.LOGIN_OK,
                null,
                BitacoraResultado.EXITO,
                "req-123");
        Pagina<BitacoraRegistro> pagina = new Pagina<>(List.of(registro), 7L, 2, 10);
        when(listarBitacoraUseCase.ejecutar(
                desde.toInstant(), hasta.toInstant(), 42L, BitacoraModulo.SEGURIDAD,
                BitacoraResultado.EXITO, "req-123", 2, 10)).thenReturn(pagina);

        ResponseEntity<Pagina<BitacoraRegistroDto>> resultado = controller().listar(
                desde, hasta, 42L, BitacoraModulo.SEGURIDAD, BitacoraResultado.EXITO,
                "req-123", 2, 10);

        verify(listarBitacoraUseCase).ejecutar(
                desde.toInstant(), hasta.toInstant(), 42L, BitacoraModulo.SEGURIDAD,
                BitacoraResultado.EXITO, "req-123", 2, 10);
        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).isNotNull();
        assertThat(resultado.getBody().total()).isEqualTo(7L);
        assertThat(resultado.getBody().pagina()).isEqualTo(2);
        assertThat(resultado.getBody().tamano()).isEqualTo(10);
        assertThat(resultado.getBody().items()).containsExactly(BitacoraRegistroDto.from(registro));
    }

    @Test
    void exigePermisoBitacoraConsultar() throws NoSuchMethodException {
        Method listar = BitacoraController.class.getDeclaredMethod(
                "listar",
                OffsetDateTime.class,
                OffsetDateTime.class,
                Long.class,
                BitacoraModulo.class,
                BitacoraResultado.class,
                String.class,
                int.class,
                int.class);

        PreAuthorize autorizacion = listar.getAnnotation(PreAuthorize.class);

        assertThat(autorizacion).isNotNull();
        assertThat(autorizacion.value()).isEqualTo("hasAuthority('BITACORA_CONSULTAR')");
    }

    private BitacoraController controller() {
        return new BitacoraController(listarBitacoraUseCase);
    }
}
