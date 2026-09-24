package com.jovycandy.anexo24.administration.profiles.infrastructure.persistence;

import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilPermisosDetalle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/** Pruebas del contrato que distingue perfil sin permisos de perfil inexistente. */
@ExtendWith(MockitoExtension.class)
class PerfilPermisosConsultaJdbcAdapterTest {
    @Mock private JdbcTemplate appJdbcTemplate;
    private PerfilPermisosConsultaJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PerfilPermisosConsultaJdbcAdapter(appJdbcTemplate);
    }

    @Test
    void filaDePerfilSinActividadDevuelveDetalleConPermisosVacios() {
        // El mapper se ejecuta por JdbcTemplate; se simula el resultado ya mapeado del SP.
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("items", List.of(
                filaPerfilSinPermisos())));

        Optional<PerfilPermisosDetalle> resultado = adapter.findPermissionsByProfileId(7L);

        assertThat(resultado).isPresent();
        assertThat(resultado.orElseThrow().perfilId()).isEqualTo(7L);
        assertThat(resultado.orElseThrow().permisos()).isEmpty();
    }

    @Test
    void resultadoVacioIndicaPerfilInexistente() {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("items", List.of()));

        assertThat(adapter.findPermissionsByProfileId(99L)).isEmpty();
    }

    private Object filaPerfilSinPermisos() {
        try {
            var constructor = Class.forName(PerfilPermisosConsultaJdbcAdapter.class.getName() + "$FilaPermiso")
                    .getDeclaredConstructor(Long.class, String.class, String.class, Long.class, String.class,
                            String.class, String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(7L, "SIN_PERMISOS", "ACTIVO", null, null, null, null, null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
