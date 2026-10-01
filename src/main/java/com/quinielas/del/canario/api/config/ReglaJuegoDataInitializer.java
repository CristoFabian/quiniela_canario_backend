package com.quinielas.del.canario.api.config;

import com.quinielas.del.canario.api.entity.CategoriaRegla;
import com.quinielas.del.canario.api.entity.ReglaJuego;
import com.quinielas.del.canario.api.repository.ReglaJuegoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra las reglas del juego (contenido inicial editable por el administrador)
 * al arrancar la aplicación, solo si aún no existen.
 * A partir de ahí, todo el mantenimiento (crear/editar/eliminar/reordenar) se
 * hace vía {@code /api/reglas/admin/**} sin necesidad de tocar código.
 */
@Component
@Order(2)
public class ReglaJuegoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReglaJuegoDataInitializer.class);

    private final ReglaJuegoRepository reglaRepo;

    public ReglaJuegoDataInitializer(ReglaJuegoRepository reglaRepo) {
        this.reglaRepo = reglaRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (reglaRepo.count() > 0) {
            log.info("Reglas del juego ya inicializadas, se omite la carga.");
            return;
        }

        log.info("Inicializando reglas del juego...");

        // ── GENERAL ────────────────────────────────────────────────────
        reglaRepo.save(new ReglaJuego(
                "Estructura de una quiniela",
                "Cada quiniela agrupa un conjunto fijo de 8 partidos. Pasa por los estados " +
                "CREADA -> ABIERTA -> EN JUEGO -> FINALIZADA. Mientras está ABIERTA los jugadores " +
                "pueden registrar jugadas y pagar su participación; al pasar a EN JUEGO ya no se " +
                "aceptan jugadas nuevas.",
                CategoriaRegla.GENERAL, 1, true));

        reglaRepo.save(new ReglaJuego(
                "Cómo participar",
                "Un jugador registra una jugada pronosticando los 8 partidos de la quiniela. " +
                "La jugada solo es válida una vez que su pago es aprobado por la administración " +
                "(pasa de CREADA -> PENDIENTE -> ACTIVA). Solo las jugadas ACTIVAS participan en la evaluación " +
                "y en la determinación de ganadores.",
                CategoriaRegla.GENERAL, 2, true));

        // ── PUNTUACION ─────────────────────────────────────────────────
        reglaRepo.save(new ReglaJuego(
                "Tipos de pronóstico y puntos por partido",
                "Por cada partido se debe elegir un solo pronóstico los cuales están " +
                "clasificados de la siguiente manera: \n" +
                "Resultado final (3 pts).\n" +
                "Total de goles (2 pts).\n" +
                "Ambos marcan / BTTS (2 pts).\n" +
                "Tiros de esquina (3 pts).",
                CategoriaRegla.PUNTUACION, 1, true));

        reglaRepo.save(new ReglaJuego(
                "Cuando y cómo se otorgan los puntos",
                "Los puntos se calculan automáticamente en cuanto un partido se marca como " +
                "terminado (FINALIZADO, SUSPENDIDO o POSPUESTO), no hasta el cierre de toda la " +
                "quiniela. Los partidos SUSPENDIDOS o POSPUESTOS no otorgan puntos a nadie (0 " +
                "puntos automáticos para todos los pronósticos de ese partido).",
                CategoriaRegla.PUNTUACION, 2, true));

        // ── DESEMPATE ──────────────────────────────────────────────────
        reglaRepo.save(new ReglaJuego(
                "Regla de oro: mismo puntaje siempre",
                "Todos los ganadores de una quiniela tienen siempre el mismo puntaje (el máximo " +
                "alcanzado). El desempate nunca elige a alguien con menos puntos; solo decide, " +
                "entre los empatados en el máximo, cuales se declaran ganadores oficiales.",
                CategoriaRegla.DESEMPATE, 1, true));

        reglaRepo.save(new ReglaJuego(
                "Cuando se activa el desempate",
                "Si 5 usuarios distintos o menos comparten el puntaje máximo, no hay desempate: " +
                "todos son declarados ganadores directamente. Si son más de 5, se activa la " +
                "cadena de desempate para intentar reducir el grupo (el 5 es solo un disparador, " +
                "no un tope máximo de ganadores).",
                CategoriaRegla.DESEMPATE, 2, true));

        reglaRepo.save(new ReglaJuego(
                "Cadena de desempate",
                "1) Pronósticos más difíciles: gana quien acertó más pronósticos de los tipos " +
                "con mayor puntaje descritos en la sección Puntuación - Tipos de pronostico y " +
                "puntos por partido.\n" +
                "2) Mayor número de aciertos totales, de cualquier tipo. \n" +
                "3) Ultimo partido acertado (por fecha).\n" +
                " 4) Empate definitivo: si el empate persiste, todos los finalistas son " +
                "declarados ganadores por igual.",
                CategoriaRegla.DESEMPATE, 3, true));

        // ── PREMIOS ────────────────────────────────────────────────────
        reglaRepo.save(new ReglaJuego(
                "Reparto del premio",
                "El premio a repartir es la bolsa acumulada, formada por la suma de todos los " +
                "pagos aprobados de los participantes de esa quiniela menos el 10% de comisión " +
                "que se le otorga a la administración por la organización de la misma. Si hay un " +
                "solo ganador, se lleva la bolsa completa. Si hay más de un ganador, la bolsa se " +
                "reparte entre todos los ganadores declarados según la determine el sistema de " +
                "Desempate descrito anteriormente.",
                CategoriaRegla.PREMIOS, 1, true));

        // ── PAGOS ──────────────────────────────────────────────────────
        reglaRepo.save(new ReglaJuego(
                "Confirmación de pago",
                "Toda jugada debe tener un pago reportado y aprobado por la administración " +
                "antes de participar. Si el pago es rechazado, la jugada vuelve a estado CREADA " +
                "para reintentar él envió del comprobante de pago mientras la quiniela siga " +
                "abierta. Una vez que tu comprobante de pago sea aprobado tus jugadas quedaran " +
                "en estado a ACTIVA y estarás participando en la quiniela.",
                CategoriaRegla.PAGOS, 1, true));

        reglaRepo.save(new ReglaJuego(
                "Saldo a favor por pagos verificados fuera de tiempo",
                "Si subes tu comprobante y el primer partido de la quiniela ya inició antes de " +
                "que la administración lo valide, tu jugada ya no puede activarse, pero el monto " +
                "pagado no se pierde: se acredita automáticamente como saldo a favor en tu " +
                "perfil.\n" +
                "Este saldo puede usarse para pagar cualquier jugada futura, en cualquier " +
                "quiniela, siempre que cubra el 100% del monto a pagar de tus jugadas seleccionadas. " +
                "Al pagar con saldo a favor, el pago queda " +
                "aprobado de inmediato y la jugada se activa sin necesidad de subir un nuevo " +
                "comprobante, ya que ese dinero ya fue verificado previamente.\n" +
                "El saldo no caduca: permanece disponible en tu perfil hasta que decidas usarlo.",
                CategoriaRegla.PAGOS, 2, true));

        log.info("Reglas del juego inicializadas.");
    }
}

