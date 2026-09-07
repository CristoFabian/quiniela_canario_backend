package com.quinielas.del.canario.api.config;

import com.quinielas.del.canario.api.entity.OpcionPronostico;
import com.quinielas.del.canario.api.entity.TipoPronostico;
import com.quinielas.del.canario.api.repository.TipoPronosticoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra los catálogos de TipoPronostico y OpcionPronostico
 * al arrancar la aplicación, solo si aún no existen.
 */
@Component
public class CatalogDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogDataInitializer.class);

    private final TipoPronosticoRepository tipoRepo;

    public CatalogDataInitializer(TipoPronosticoRepository tipoRepo) {
        this.tipoRepo = tipoRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (tipoRepo.count() > 0) {
            log.info("Catalogos de pronosticos ya inicializados, se omite la carga.");
            return;
        }

        log.info("Inicializando catalogos de tipos y opciones de pronostico...");

        // ── 1. RESULTADO (3 pts) ──────────────────────────────────────
        TipoPronostico resultado = new TipoPronostico(
                "RESULTADO",
                "Resultado final",
                "Pronostica el resultado final del juego.",
                3, true);
        resultado.getOpciones().add(new OpcionPronostico(resultado, "LOCAL_WIN",  "Victoria local",    null, null, true));
        resultado.getOpciones().add(new OpcionPronostico(resultado, "DRAW",       "Empate",            null, null, true));
        resultado.getOpciones().add(new OpcionPronostico(resultado, "AWAY_WIN",   "Victoria visitante", null, null, true));
        tipoRepo.save(resultado);

        // ── 2. GOLES (2 pts) ─────────────────────────────────────────
        TipoPronostico goles = new TipoPronostico(
                "GOLES",
                "Total de goles",
                "Pronostica el total de goles que habra en el juego.",
                2, true);
        goles.getOpciones().add(new OpcionPronostico(goles, "G_0_1", "0-1 goles", 0, 1,    true));
        goles.getOpciones().add(new OpcionPronostico(goles, "G_2_3", "2-3 goles", 2, 3,    true));
        goles.getOpciones().add(new OpcionPronostico(goles, "G_4_5", "4-5 goles", 4, 5,    true));
        goles.getOpciones().add(new OpcionPronostico(goles, "G_6_M", "6+ goles",  6, null, true));
        tipoRepo.save(goles);

        // ── 3. BTTS (2 pts) ──────────────────────────────────────────
        TipoPronostico btts = new TipoPronostico(
                "BTTS",
                "Ambos equipos marcan",
                "Pronostica si ambos equipos marcan o no.",
                2, true);
        btts.getOpciones().add(new OpcionPronostico(btts, "BTTS_SI", "Si, ambos marcan",    null, null, true));
        btts.getOpciones().add(new OpcionPronostico(btts, "BTTS_NO", "No, no ambos marcan", null, null, true));
        tipoRepo.save(btts);

        // ── 4. CORNERS (3 pts) ───────────────────────────────────────
        TipoPronostico corners = new TipoPronostico(
                "CORNERS",
                "Tiros de esquina",
                "Pronostica el total de tiros de esquina que habra en el juego.",
                3, true);
        corners.getOpciones().add(new OpcionPronostico(corners, "C_0_5",  "0-5 corners",  0,  5,    true));
        corners.getOpciones().add(new OpcionPronostico(corners, "C_6_9",  "6-9 corners",  6,  9,    true));
        corners.getOpciones().add(new OpcionPronostico(corners, "C_10_M", "10+ corners",  10, null, true));
        tipoRepo.save(corners);

        log.info("Catalogos inicializados: 4 tipos de pronostico y 12 opciones.");
    }
}

