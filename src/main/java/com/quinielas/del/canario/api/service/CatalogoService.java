package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.OpcionPronosticoRequest;
import com.quinielas.del.canario.api.dto.OpcionPronosticoResponse;
import com.quinielas.del.canario.api.dto.TipoPronosticoRequest;
import com.quinielas.del.canario.api.dto.TipoPronosticoResponse;
import com.quinielas.del.canario.api.entity.OpcionPronostico;
import com.quinielas.del.canario.api.entity.TipoPronostico;
import com.quinielas.del.canario.api.repository.OpcionPronosticoRepository;
import com.quinielas.del.canario.api.repository.TipoPronosticoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogoService {

    private final TipoPronosticoRepository   tipoRepo;
    private final OpcionPronosticoRepository opcionRepo;

    public CatalogoService(TipoPronosticoRepository tipoRepo,
                           OpcionPronosticoRepository opcionRepo) {
        this.tipoRepo   = tipoRepo;
        this.opcionRepo = opcionRepo;
    }

    // ═════════════════════════════════════════════════════════════════
    //  TIPO PRONOSTICO
    // ═════════════════════════════════════════════════════════════════

    /** Listar todos los tipos (activos e inactivos). */
    @Transactional(readOnly = true)
    public List<TipoPronosticoResponse> listarTipos() {
        return tipoRepo.findAll()
                .stream()
                .map(TipoPronosticoResponse::from)
                .collect(Collectors.toList());
    }

    /** Obtener un tipo por id. */
    @Transactional(readOnly = true)
    public TipoPronosticoResponse obtenerTipo(Long id) {
        return TipoPronosticoResponse.from(buscarTipoOException(id));
    }

    /** Crear un nuevo tipo de pronóstico. */
    @Transactional
    public TipoPronosticoResponse crearTipo(TipoPronosticoRequest request) {
        if (tipoRepo.existsByCodigo(request.getCodigo())) {
            throw new IllegalArgumentException(
                    "Ya existe un tipo de pronostico con el codigo: " + request.getCodigo());
        }
        if (request.getPuntos() <= 0) {
            throw new IllegalArgumentException("Los puntos deben ser mayor a 0.");
        }

        TipoPronostico tipo = new TipoPronostico(
                request.getCodigo(),
                request.getNombre(),
                request.getDescripcion(),
                request.getPuntos(),
                request.isActivo());
        return TipoPronosticoResponse.from(tipoRepo.save(tipo));
    }

    /** Actualizar un tipo existente.
     *  - Si el tipo pasa a inactivo  (true → false): todas sus opciones se desactivan en cascada.
     *  - Si el tipo se reactiva      (false → true):  todas sus opciones se reactivan en cascada.
     */
    @Transactional
    public TipoPronosticoResponse actualizarTipo(Long id, TipoPronosticoRequest request) {
        TipoPronostico tipo = buscarTipoOException(id);

        // Si cambia el código verificar que no exista otro con ese código
        if (!tipo.getCodigo().equals(request.getCodigo()) &&
                tipoRepo.existsByCodigo(request.getCodigo())) {
            throw new IllegalArgumentException(
                    "Ya existe otro tipo de pronostico con el codigo: " + request.getCodigo());
        }
        if (request.getPuntos() <= 0) {
            throw new IllegalArgumentException("Los puntos deben ser mayor a 0.");
        }

        tipo.setCodigo(request.getCodigo());
        tipo.setNombre(request.getNombre());
        tipo.setDescripcion(request.getDescripcion());
        tipo.setPuntos(request.getPuntos());

        // Cascada: si se desactiva el tipo → desactivar todas sus opciones
        //          si se reactiva el tipo  → reactivar  todas sus opciones
        boolean seDesactiva = tipo.isActivo() && !request.isActivo();
        boolean seReactiva  = !tipo.isActivo() && request.isActivo();
        tipo.setActivo(request.isActivo());
        if (seDesactiva) {
            tipo.getOpciones().forEach(o -> o.setActivo(false));
        } else if (seReactiva) {
            tipo.getOpciones().forEach(o -> o.setActivo(true));
        }

        return TipoPronosticoResponse.from(tipoRepo.save(tipo));
    }

    /**
     * Eliminación lógica: activo = false.
     * También desactiva en cascada todas sus opciones.
     */
    @Transactional
    public TipoPronosticoResponse eliminarTipo(Long id) {
        TipoPronostico tipo = buscarTipoOException(id);
        tipo.setActivo(false);
        tipo.getOpciones().forEach(o -> o.setActivo(false));
        return TipoPronosticoResponse.from(tipoRepo.save(tipo));
    }

    // ═════════════════════════════════════════════════════════════════
    //  OPCION PRONOSTICO
    // ═════════════════════════════════════════════════════════════════

    /** Listar todas las opciones de un tipo (activas e inactivas). */
    @Transactional(readOnly = true)
    public List<OpcionPronosticoResponse> listarOpciones(Long tipoId) {
        buscarTipoOException(tipoId); // valida que el tipo exista
        return opcionRepo.findByTipoPronosticoId(tipoId)
                .stream()
                .map(OpcionPronosticoResponse::from)
                .collect(Collectors.toList());
    }

    /** Obtener una opción por id. */
    @Transactional(readOnly = true)
    public OpcionPronosticoResponse obtenerOpcion(Long id) {
        return OpcionPronosticoResponse.from(buscarOpcionOException(id));
    }

    /**
     * Crear una nueva opción para un tipo. Valida:
     *  1. El tipo debe estar activo.
     *  2. Código único dentro del tipo.
     *  3. valorMin <= valorMax cuando valorMax != null.
     *  4. Sin solapamiento de rangos con opciones existentes del mismo tipo.
     */
    @Transactional
    public OpcionPronosticoResponse crearOpcion(Long tipoId, OpcionPronosticoRequest request) {
        TipoPronostico tipo = buscarTipoOException(tipoId);

        validarTipoActivo(tipo);
        validarCodigoUnicoOpcion(tipoId, request.getCodigo(), null);
        validarRango(request.getValorMin(), request.getValorMax());
        validarSinSolapamiento(tipoId, request.getValorMin(), request.getValorMax(), null);

        OpcionPronostico opcion = new OpcionPronostico(
                tipo,
                request.getCodigo(),
                request.getDescripcion(),
                request.getValorMin(),
                request.getValorMax(),
                request.isActivo());

        return OpcionPronosticoResponse.from(opcionRepo.save(opcion));
    }

    /**
     * Actualizar una opción existente. Valida:
     *  1. El tipo al que pertenece debe estar activo.
     *  2. Código único dentro del tipo (excluyendo la propia opción).
     *  3. valorMin <= valorMax cuando valorMax != null.
     *  4. Sin solapamiento con otras opciones del mismo tipo.
     */
    @Transactional
    public OpcionPronosticoResponse actualizarOpcion(Long id, OpcionPronosticoRequest request) {
        OpcionPronostico opcion = buscarOpcionOException(id);

        validarTipoActivo(opcion.getTipoPronostico());
        validarCodigoUnicoOpcion(opcion.getTipoPronostico().getId(), request.getCodigo(), id);
        validarRango(request.getValorMin(), request.getValorMax());
        validarSinSolapamiento(opcion.getTipoPronostico().getId(),
                request.getValorMin(), request.getValorMax(), id);

        opcion.setCodigo(request.getCodigo());
        opcion.setDescripcion(request.getDescripcion());
        opcion.setValorMin(request.getValorMin());
        opcion.setValorMax(request.getValorMax());
        opcion.setActivo(request.isActivo());

        return OpcionPronosticoResponse.from(opcionRepo.save(opcion));
    }

    /** Eliminación lógica de opción: activo = false. */
    @Transactional
    public OpcionPronosticoResponse eliminarOpcion(Long id) {
        OpcionPronostico opcion = buscarOpcionOException(id);
        opcion.setActivo(false);
        return OpcionPronosticoResponse.from(opcionRepo.save(opcion));
    }

    // ═════════════════════════════════════════════════════════════════
    //  Validaciones privadas
    // ═════════════════════════════════════════════════════════════════

    /** El tipo de pronóstico debe estar activo para poder gestionar sus opciones. */
    private void validarTipoActivo(TipoPronostico tipo) {
        if (!tipo.isActivo()) {
            throw new IllegalArgumentException(
                    "No se pueden gestionar opciones de un tipo de pronostico inactivo: "
                    + tipo.getCodigo());
        }
    }

    /**
     * El código debe ser único dentro del tipo.
     * @param excludeId null en crear; id de la opción actual en actualizar.
     */
    private void validarCodigoUnicoOpcion(Long tipoId, String codigo, Long excludeId) {
        boolean existe = (excludeId == null)
                ? opcionRepo.existsByTipoPronosticoIdAndCodigo(tipoId, codigo)
                : opcionRepo.existsByTipoPronosticoIdAndCodigoAndIdNot(tipoId, codigo, excludeId);
        if (existe) {
            throw new IllegalArgumentException(
                    "Ya existe una opcion con el codigo '" + codigo +
                    "' en este tipo de pronostico.");
        }
    }

    /** valorMin debe ser <= valorMax cuando ambos están presentes. */
    private void validarRango(Integer valorMin, Integer valorMax) {
        if (valorMin != null && valorMax != null && valorMin > valorMax) {
            throw new IllegalArgumentException(
                    "El valor minimo (" + valorMin +
                    ") no puede ser mayor que el valor maximo (" + valorMax + ").");
        }
    }

    /**
     * No se permiten rangos solapados con otras opciones del mismo tipo.
     * Solo se evalúan opciones que tengan valorMin definido (opciones de rango).
     * Se trata valorMax = null como infinito (Integer.MAX_VALUE).
     *
     * @param excludeId null en crear; id de la opción actual en actualizar.
     */
    private void validarSinSolapamiento(Long tipoId, Integer nuevoMin, Integer nuevoMax,
                                        Long excludeId) {
        // Si la nueva opción no tiene rango, no hay solapamiento posible
        if (nuevoMin == null) return;

        int nuevoMaxEfectivo = (nuevoMax != null) ? nuevoMax : Integer.MAX_VALUE;

        List<OpcionPronostico> existentes = opcionRepo.findByTipoPronosticoId(tipoId)
                .stream()
                // Excluir la propia opción en caso de actualización
                .filter(o -> excludeId == null || !o.getId().equals(excludeId))
                // Solo comparar contra opciones que también tienen rango
                .filter(o -> o.getValorMin() != null)
                .collect(Collectors.toList());

        for (OpcionPronostico existente : existentes) {
            int exMin = existente.getValorMin();
            int exMax = (existente.getValorMax() != null) ? existente.getValorMax() : Integer.MAX_VALUE;

            // Dos rangos [a,b] y [c,d] se solapan si a <= d AND c <= b
            if (nuevoMin <= exMax && exMin <= nuevoMaxEfectivo) {
                String rangoExistente = existente.getValorMax() != null
                        ? exMin + "-" + existente.getValorMax()
                        : exMin + "+";
                String rangoNuevo = nuevoMax != null
                        ? nuevoMin + "-" + nuevoMax
                        : nuevoMin + "+";
                throw new IllegalArgumentException(
                        "El rango [" + rangoNuevo + "] se solapa con la opcion existente '" +
                        existente.getCodigo() + "' [" + rangoExistente + "].");
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Helpers de búsqueda
    // ═════════════════════════════════════════════════════════════════

    private TipoPronostico buscarTipoOException(Long id) {
        return tipoRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Tipo de pronostico no encontrado con id: " + id));
    }

    private OpcionPronostico buscarOpcionOException(Long id) {
        return opcionRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Opcion de pronostico no encontrada con id: " + id));
    }
}

