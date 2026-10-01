# Reglas del Juego — Quinielas del Canario

Este documento describe, en lenguaje claro, las reglas oficiales del juego a partir
de la lógica implementada en el backend (`EvaluacionService`, `CierreQuinielaService`,
`TipoPronostico`, `Quiniela`, `Jugada`, `Partido`, `Pago`).

---

## 1. Estructura de una Quiniela

- Cada quiniela agrupa un conjunto fijo de **8 partidos** (`Quiniela.MAX_PARTIDOS = 8`).
- Estados de la quiniela: `CREADA → ABIERTA → EN_JUEGO → FINALIZADA`.
  - **CREADA**: recién armada por el administrador, aún no visible a jugadores.
  - **ABIERTA**: los jugadores pueden registrar jugadas y pagar su participación.
  - **EN_JUEGO**: los partidos ya comenzaron; no se aceptan más jugadas nuevas.
  - **FINALIZADA**: todos los partidos terminaron y ya se ejecutó el cierre oficial.
- `bolsaAcumulada`: suma de todos los pagos **APROBADOS** vinculados a jugadas de esa
  quiniela. Es el pozo/premio que se reparte al final.

## 2. Cómo participar (Jugada)

- Un jugador registra una **jugada**, pronosticando **los 8 partidos** de la quiniela.
- Antes de que la jugada sea válida, debe pagar el costo de entrada. El pago pasa por:
  `PENDIENTE → APROBADO` (jugada pasa a `ACTIVA`) o `RECHAZADO` (jugada vuelve a `CREADA`).
- Solo las jugadas en estado **ACTIVA** participan en la evaluación y en la determinación
  de ganadores. Estados posibles de una jugada:
  `CREADA → PENDIENTE_VALIDACION → ACTIVA → FINALIZADA` (o `RECHAZADA` / `EXPIRADA` si el
  pago no se completa a tiempo).

## 3. Tipos de pronóstico y puntos por partido

Por cada uno de los 8 partidos, el jugador debe pronosticar **4 aspectos** distintos.
Cada aspecto tiene un puntaje fijo si se acierta:

| Tipo de pronóstico | Código | Puntos por acierto | Qué se pronostica |
|---|---|---|---|
| **Resultado final** | `RESULTADO` | **3 pts** | Victoria local / Empate / Victoria visitante |
| **Total de goles** | `GOLES` | **2 pts** | Rango de goles totales del partido (0-1, 2-3, 4-5, 6+) |
| **Ambos marcan (BTTS)** | `BTTS` | **2 pts** | Si ambos equipos anotan al menos un gol (Sí/No) |
| **Tiros de esquina** | `CORNERS` | **3 pts** | Rango de córners totales del partido (0-5, 6-9, 10+) |

**Máximo posible por partido:** 10 puntos (3+2+2+3).
**Máximo posible por jugada (8 partidos):** 80 puntos.

> Los tipos con **mayor puntaje** (`RESULTADO` y `CORNERS`, 3 pts cada uno) se consideran
> los **"pronósticos más difíciles"** y son el primer criterio de desempate.

## 4. Cuándo y cómo se otorgan los puntos

- Los puntos se calculan **automáticamente cuando un partido se marca como terminado**
  (`FINALIZADO`, `SUSPENDIDO` o `POSPUESTO`), no hasta que cierre toda la quiniela.
- Solo se evalúan pronósticos de jugadas **ACTIVAS**.
- Reglas de acierto por tipo:
  - **RESULTADO**: acierta si el código elegido coincide con el resultado real
    (calculado a partir del marcador).
  - **GOLES**: acierta si la suma de goles (local + visitante) cae dentro del rango elegido.
  - **BTTS**: acierta si la opción elegida (Sí/No) coincide con si ambos equipos anotaron.
  - **CORNERS**: acierta si el total de córners cae dentro del rango elegido.
- **Partidos SUSPENDIDOS o POSPUESTOS**: no otorgan puntos a nadie (0 puntos automáticos
  para todos los pronósticos de ese partido). No perjudican ni benefician a nadie.
- Los puntos se van acumulando en `Jugada.puntosObtenidos` conforme cada partido termina.

## 5. Cómo se determina el ganador (Cierre de la Quiniela)

El cierre solo puede ejecutarse cuando:
1. La quiniela está en estado `EN_JUEGO`.
2. Todos los 8 partidos ya están en un estado terminal (`FINALIZADO`, `SUSPENDIDO` o `POSPUESTO`).
3. Existe al menos una jugada `ACTIVA`.
4. La quiniela no ha sido cerrada previamente (el cierre es único e irreversible).

### Regla de oro: **todos los ganadores tienen siempre el mismo puntaje**

1. Se calcula el **puntaje máximo** alcanzado entre todas las jugadas activas.
2. Se identifican las **candidatas**: todas las jugadas que alcanzaron exactamente ese
   puntaje máximo (pueden ser de 1 hasta muchos usuarios distintos).
3. Se cuentan los **usuarios distintos** entre esas candidatas:
   - **5 usuarios distintos o menos** → **no hay desempate**. Todas las candidatas son
     declaradas ganadoras directamente (criterio `MAYOR_PUNTAJE`).
   - **Más de 5 usuarios distintos** → se activa la **cadena de desempate** para reducir
     el grupo de ganadores, sin nunca bajar el puntaje exigido.

> ⚠️ El "5" es solo un **disparador** que decide si hace falta desempatar o no.
> **No es un límite máximo garantizado de ganadores.** Si el empate persiste incluso
> después de aplicar los 3 criterios de desempate, **todos** los que sigan empatados
> son declarados ganadores (podrían ser más de 5).

### Cadena de desempate (en orden, solo si aplica)

Se aplica en cascada. En cuanto un criterio reduce el grupo a 5 usuarios distintos o
menos, el proceso se detiene ahí y esos son los ganadores oficiales.

1. **Pronósticos más difíciles**: gana quien acertó más pronósticos de los tipos con
   mayor puntaje del catálogo (`RESULTADO` y `CORNERS`, ambos con 3 pts). Si varios tipos
   comparten el puntaje máximo, se cuentan los aciertos combinados de todos ellos juntos.
2. **Mayor número de aciertos totales**: gana quien acertó más pronósticos en total,
   sin importar el tipo.
3. **Último partido acertado**: gana quien acertó al menos un pronóstico del partido
   más reciente (por fecha) de la quiniela. Si nadie del grupo lo acertó, este criterio
   se ignora y se pasa al siguiente sin reducir el grupo.
4. **Empate definitivo**: si tras aplicar los 3 criterios anteriores el empate persiste,
   **todos** los finalistas son declarados ganadores por igual (no hay más desempate).

### Qué pasa al cerrar

- Se genera un registro de auditoría (`CierreQuiniela`) con el puntaje máximo, el total
  de jugadas evaluadas, el total de ganadores, si hubo empate y qué criterio se aplicó.
- Se genera un registro (`GanadorQuiniela`) por cada ganador, con una "foto" inmutable
  de sus puntos, nombre completo y el criterio que lo hizo ganador.
- Todas las jugadas activas reciben una **posición final** (ranking denso: jugadas con
  el mismo puntaje comparten posición) y se marcan como `FINALIZADA`.
- La quiniela pasa a estado `FINALIZADA` y ya no admite cambios.

## 6. Reparto del premio

- La bolsa acumulada (`bolsaAcumulada`) es la suma de todos los pagos aprobados de los
  participantes de esa quiniela.
- Antes de repartir, se retiene una **comisión de la casa del 10%** sobre la bolsa
  acumulada (`montoComision = bolsaAcumulada * 0.10`, redondeado a 2 decimales).
- El **premio neto a repartir** es `premioTotalRepartido = bolsaAcumulada - montoComision`.
- Si hay **un solo ganador**, se lleva el premio neto completo.
- Si hay **más de un ganador** (empate resuelto o empate definitivo), el premio neto se
  reparte **en partes iguales** entre todos los ganadores declarados. El cierre calcula
  `montoPremio` para cada `GanadorQuiniela` (`premioTotalRepartido / totalGanadores`, con
  2 decimales); si la división deja centavos sobrantes por redondeo, se asignan uno a
  uno a los primeros ganadores para que la suma exacta de los premios sea igual al
  premio neto repartido.
- El registro `CierreQuiniela` guarda como auditoría inmutable: `bolsaAcumuladaSnapshot`,
  `porcentajeComision`, `montoComision` y `premioTotalRepartido`.

## 7. Entrega del premio monetario

Una vez calculado `montoPremio`, cada registro `GanadorQuiniela` sigue su propio flujo de
entrega, independiente del cierre de la quiniela, mediante el estado `EstadoPremio`:

1. **PENDIENTE** (automático al cerrar): el premio ya está calculado pero aún no se ha
   pagado.
2. **PAGADO**: el administrador realiza la transferencia por fuera del sistema y sube el
   comprobante (imagen o PDF) mediante
   `PUT /api/admin/premios/{ganadorId}/comprobante`. Se registra quién lo pagó
   (`pagadoPor`) y cuándo (`fechaPagoPremio`).
3. **CONFIRMADO**: el jugador revisa el comprobante (`GET /api/jugador/premios/{ganadorId}/comprobante`)
   y confirma que recibió el dinero mediante
   `PATCH /api/jugador/premios/{ganadorId}/confirmar`. Se registra `fechaConfirmacionJugador`.
   Este es un estado final: el comprobante ya no puede reemplazarse.

El jugador puede consultar todos sus premios (de cualquier quiniela) en
`GET /api/jugador/premios`, y el administrador puede ver los de una quiniela específica en
`GET /api/admin/quinielas/{id}/premios`.

---

### Resumen rápido para el jugador

1. Pronostica los 4 aspectos (resultado, goles, ambos marcan, córners) de los 8 partidos.
2. Paga tu entrada y espera la aprobación.
3. Cada acierto suma puntos: 3 (resultado), 2 (goles), 2 (ambos marcan), 3 (córners).
4. Al terminar todos los partidos, gana(n) quien(es) tenga(n) más puntos.
5. Si hay empate entre pocos jugadores (5 o menos), todos ganan por igual.
6. Si empatan más de 5 jugadores, se desempata por: aciertos en los pronósticos más
   difíciles → total de aciertos → acierto del último partido → empate definitivo
   (todos ganan) si nada de eso resuelve el empate.


