package com.jovycandy.anexo24.savedqueries.application;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardada;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardadaAlcance;
import com.jovycandy.anexo24.savedqueries.domain.port.ConsultaGuardadaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultaGuardadaServiceTest {
    private final ConsultaGuardadaRepository repository = mock(ConsultaGuardadaRepository.class);
    private final ConsultaGuardadaService service = new ConsultaGuardadaService(
            repository, new ConsultaGuardadaCriteriosValidator(), new AuthenticatedUserContext(),
            mock(RegistrarEventoBitacoraService.class));

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void listaConsultasAisladasPorActorAutenticado() {
        authenticate(101L);
        service.listar(null);
        authenticate(202L);
        service.listar(null);
        verify(repository).listar(101L, null);
        verify(repository).listar(202L, null);
    }

    @Test
    void creaSoloConIdentidadDelContextoYFiltrosEstructurados() {
        authenticate(101L);
        LocalDateTime now = LocalDateTime.parse("2026-10-01T00:00:00");
        ConsultaGuardada created = new ConsultaGuardada(8L, "Octubre", null, ConsultaGuardadaAlcance.ENTRADAS,
                "{\"from\":\"2026-10-01\",\"to\":\"2026-10-31\"}", now, now);
        when(repository.crear(101L, "Octubre", null, ConsultaGuardadaAlcance.ENTRADAS,
                "{\"from\":\"2026-10-01\",\"to\":\"2026-10-31\"}")).thenReturn(created);
        ObjectNode criteria = JsonMapper.builder().build().createObjectNode().put("from", "2026-10-01").put("to", "2026-10-31");

        assertThat(service.crear("Octubre", null, ConsultaGuardadaAlcance.ENTRADAS, criteria, null)).isEqualTo(created);
        verify(repository).crear(101L, "Octubre", null, ConsultaGuardadaAlcance.ENTRADAS,
                "{\"from\":\"2026-10-01\",\"to\":\"2026-10-31\"}");
    }

    private void authenticate(long userId) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUserPrincipal(userId, "usuario" + userId), "token", List.of());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
