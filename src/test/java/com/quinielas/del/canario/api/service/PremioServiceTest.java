package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.PremioResponse;
import com.quinielas.del.canario.api.entity.GanadorQuiniela;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.repository.GanadorQuinielaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PremioServiceTest {

    @Mock
    private GanadorQuinielaRepository ganadorRepo;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PremioService premioService;

    @Test
    void listarMisPremios_deberiaDevolverTodosLosPremiosDelJugador() {
        User user = new User();
        user.setId(42L);

        // Preparar quiniela / cierre / jugadas mínimas para el mapeo del DTO
        com.quinielas.del.canario.api.entity.Quiniela quiniela = new com.quinielas.del.canario.api.entity.Quiniela();
        quiniela.setId(10L);
        quiniela.setNombre("PruebaQuiniela");

        com.quinielas.del.canario.api.entity.CierreQuiniela cierre = new com.quinielas.del.canario.api.entity.CierreQuiniela();
        cierre.setQuiniela(quiniela);

        com.quinielas.del.canario.api.entity.Jugada jugada1 = new com.quinielas.del.canario.api.entity.Jugada();
        jugada1.setId(100L);
        jugada1.setUsuario(user);
        jugada1.setQuiniela(quiniela);

        com.quinielas.del.canario.api.entity.Jugada jugada2 = new com.quinielas.del.canario.api.entity.Jugada();
        jugada2.setId(101L);
        jugada2.setUsuario(user);
        jugada2.setQuiniela(quiniela);

        GanadorQuiniela g1 = new GanadorQuiniela();
        g1.setId(1L);
        g1.setUsuario(user);
        g1.setMontoPremio(BigDecimal.valueOf(100));
        g1.setCierreQuiniela(cierre);
        g1.setJugada(jugada1);

        GanadorQuiniela g2 = new GanadorQuiniela();
        g2.setId(2L);
        g2.setUsuario(user);
        g2.setMontoPremio(BigDecimal.valueOf(200));
        g2.setCierreQuiniela(cierre);
        g2.setJugada(jugada2);

        when(ganadorRepo.findByUsuarioIdOrderByIdDesc(user.getId())).thenReturn(asList(g2, g1));

        List<PremioResponse> resultados = premioService.listarMisPremios(user);

        assertThat(resultados).hasSize(2);
        assertThat(resultados.get(0).getGanadorId()).isEqualTo(2L);
        assertThat(resultados.get(1).getGanadorId()).isEqualTo(1L);
    }
}


