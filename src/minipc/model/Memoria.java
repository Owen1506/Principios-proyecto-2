package minipc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import minipc.services.memoria.AsignacionMemoria;
import minipc.services.memoria.EstrategiaAsignacionMemoria;
import minipc.services.memoria.Hueco;
import minipc.services.memoria.dinamica.AsignacionDeRegistros;
import minipc.services.memoria.fija.GestorParticionesFijas;
import minipc.services.memoria.fija.Particion;
import minipc.services.memoria.paginacion.GestorPaginacion;
import minipc.services.memoria.paginacion.GestorPaginasVirtuales;
import minipc.services.memoria.paginacion.Marco;
import minipc.services.memoria.paginacion.TablaPaginas;

/**
 * Simula la memoria principal de la Mini PC.
 *
 * La memoria se divide en dos zonas:
 *
 * - Área del Sistema Operativo: almacena los BCP de los procesos.
 * - Área de usuario: almacena las instrucciones de los programas.
 *
 * Cada atributo del BCP ocupa una posición lógica dentro del área del S.O.
 * y cada instrucción ocupa una posición lógica dentro del área de usuario.
 */
public class Memoria {

    private final int size;
    private final int inicioUsuario;

    private EstrategiaAsignacionMemoria estrategia;
    private TipoEstrategiaMemoria tipoEstrategia;
    private ConfiguracionMemoria configuracionMemoria;
    private final Almacenamiento almacenamiento;
    private final Map<Integer, AsignacionMemoria> asignaciones = new HashMap<>();
    private List<BCP> areaSistema;
    private List<Instruccion> areaUsuario;

    /**
     * Crea una nueva memoria con un tamaño determinado y define
     * a partir de qué posición inicia el espacio de usuario.
     *
     * @param size          tamaño total de la memoria
     * @param inicioUsuario primera posición disponible para programas de usuario
     */
    public Memoria(int size, int inicioUsuario) {
        this(size, inicioUsuario, null,
                new ConfiguracionDinamica(TipoEstrategiaMemoria.FIRST_FIT));
    }

    /*
     * Crea la memoria con la estrategia pedida por la configuración.
     * Se conserva para el código que todavía trabaja
     * directamente con TipoEstrategiaMemoria.
     */
    public Memoria(int size, int inicioUsuario, TipoEstrategiaMemoria tipo) {
        this(size, inicioUsuario, null, new ConfiguracionDinamica(tipo));
    }

    /*
     * Crea la memoria con el método y los parámetros pedidos por la
     * configuración. La llama Controlador cuando construye la sesión.
     */
    public Memoria(int size, int inicioUsuario, ConfiguracionMemoria configuracion) {
        this(size, inicioUsuario, null, configuracion);
    }

    /*
     * Crea la memoria con acceso al almacenamiento, necesario para paginación
     * virtual. La llama Controlador cuando construye la sesión.
     */
    public Memoria(int size, int inicioUsuario, Almacenamiento almacenamiento) {
        this(size, inicioUsuario, almacenamiento,
                new ConfiguracionDinamica(TipoEstrategiaMemoria.FIRST_FIT));
    }

    public Memoria(int size, int inicioUsuario, Almacenamiento almacenamiento,
            ConfiguracionMemoria configuracion) {
        this(size, inicioUsuario, null, almacenamiento, configuracion);
    }

    /*
     * Conserva la inyección directa de una estrategia para personalizaciones o
     * extensiones. La llama cualquier código que ya construya su propia
     * estrategia.
     */
    public Memoria(int size, int inicioUsuario, EstrategiaAsignacionMemoria estrategia) {
        this(size, inicioUsuario, estrategia, null, null);
    }

    private Memoria(int size, int inicioUsuario,
            EstrategiaAsignacionMemoria estrategia, Almacenamiento almacenamiento,
            ConfiguracionMemoria configuracion) {

        if (size < 128) {
            throw new IllegalArgumentException("El tamaño mínimo de memoria es 128.");
        }

        if (inicioUsuario <= 0 || inicioUsuario >= size) {
            throw new IllegalArgumentException("El inicio del espacio de usuario no es válido.");
        }

        if (estrategia == null) {
            estrategia = crearEstrategia(configuracion, inicioUsuario, size, almacenamiento);
        }

        this.estrategia = estrategia;
        this.almacenamiento = almacenamiento;
        this.configuracionMemoria = configuracion;
        this.tipoEstrategia = configuracion == null
                ? null
                : configuracion.estrategia();
        this.size = size;
        this.inicioUsuario = inicioUsuario;
        reiniciar();
    }

    /**
     * Construye la estrategia que corresponde al método configurado.
     *
     */
    private static EstrategiaAsignacionMemoria crearEstrategia(
            ConfiguracionMemoria configuracion, int inicioUsuario, int size,
            Almacenamiento almacenamiento) {

        if (configuracion == null) {
            throw new IllegalArgumentException(
                    "Debe indicar una configuración de memoria.");
        }

        return switch (configuracion.metodo()) {
            case DINAMICAS -> {
                TipoEstrategiaMemoria tipo = configuracion.estrategia();
                if (tipo == null) {
                    throw new IllegalArgumentException(
                            "Las particiones dinámicas requieren una estrategia.");
                }
                yield tipo.crear(inicioUsuario, size);
            }
            case FIJAS_IGUALES -> {
                int tamaño = ((ConfiguracionFijaIgual) configuracion).tamañoParticion();
                int cantidad = (size - inicioUsuario) / tamaño;
                if (cantidad == 0) {
                    throw new IllegalArgumentException("No cabe ninguna partición en el área de usuario.");
                }
                yield new GestorParticionesFijas(inicioUsuario, size,
                        Collections.nCopies(cantidad, tamaño), false);
            }
            case FIJAS_DIFERENTES -> {
                ConfiguracionFijaDiferente fija = (ConfiguracionFijaDiferente) configuracion;
                yield new GestorParticionesFijas(inicioUsuario, size, fija.tamaños(), true);
            }
            case PAGINACION -> {
                ConfiguracionPaginacion paginacion = (ConfiguracionPaginacion) configuracion;
                if (paginacion.virtual()) {
                    if (almacenamiento == null) {
                        throw new IllegalArgumentException("La paginación virtual requiere almacenamiento.");
                    }
                    yield new GestorPaginasVirtuales(almacenamiento, inicioUsuario, size, paginacion.tamañoPagina());
                }
                yield new GestorPaginacion(inicioUsuario, size, paginacion.tamañoPagina());
            }
        };
    }

    public boolean hayEspacioDisponible(int cantidad) {
        return estrategia.hayEspacioDisponible(cantidad);
    }

    /**
     * Carga un programa dentro del espacio de usuario.
     *
     * @param programa instrucciones que se desean cargar
     * @return dirección inicial del programa
     */
    public int cargarPrograma(List<Instruccion> programa) {

        if (programa == null || programa.isEmpty()) {
            throw new IllegalArgumentException("El programa está vacío.");
        }

        if (programa.contains(null)) {
            throw new IllegalArgumentException("El programa contiene instrucciones null.");
        }

        AsignacionMemoria asignacion = estrategia.asignar(programa.size());
        if (estrategia instanceof GestorPaginasVirtuales virtual) {
            try {
                virtual.registrarRespaldo(asignacion, programa);
            } catch (RuntimeException error) {
                estrategia.liberar(asignacion);
                throw error;
            }
            int inicio = asignacion.obtenerDireccionFisica(0);
            asignaciones.put(inicio, asignacion);
            return inicio;
        }
        // Validar toda la reserva antes de escribir instrucciones.
        try {
            if (asignacion.getCantidad() != programa.size()) {
                throw new IllegalStateException("La reserva no coincide con el programa.");
            }
            for (int direccion : asignacion.getPosiciones()) {
                validarDireccionUsuario(direccion);
                if (leer(direccion) != null)
                    throw new IllegalStateException("La reserva se superpone con otro programa.");
            }
        } catch (RuntimeException error) {
            estrategia.liberar(asignacion);
            throw error;
        }
        for (int i = 0; i < programa.size(); i++) {
            areaUsuario.set(asignacion.obtenerDireccionFisica(i) - inicioUsuario, programa.get(i));
        }
        int inicio = asignacion.obtenerDireccionFisica(0);
        asignaciones.put(inicio, asignacion);
        return inicio;
    }

    public AsignacionMemoria getAsignacion(int inicio) {
        AsignacionMemoria asignacion = asignaciones.get(inicio);
        if (asignacion == null)
            throw new IllegalArgumentException("No existe una reserva en " + inicio);
        return asignacion;
    }

    /** Traduce el PC lógico utilizando la reserva vigente del proceso. */
    public int obtenerDireccionFisica(BCP proceso, int posicionLogica) {
        AsignacionMemoria asignacion = getAsignacion(proceso.getInicioPrograma());
        if (asignacion != proceso.getAsignacionMemoria()) {
            throw new IllegalStateException("El proceso no tiene una asignación vigente en RAM.");
        }
        if (estrategia instanceof GestorPaginacion paginacion) {
            return paginacion.getTabla(asignacion).obtenerDireccionFisica(posicionLogica);
        }
        if (estrategia instanceof GestorPaginasVirtuales virtual) {
            return virtual.direccionFisica(asignacion, posicionLogica);
        }
        return asignacion.obtenerDireccionFisica(posicionLogica);
    }

    public Instruccion leer(BCP proceso, int posicionLogica) {
        if (estrategia instanceof GestorPaginasVirtuales virtual) {
            AsignacionMemoria asignacion = getAsignacion(proceso.getInicioPrograma());
            if (asignacion != proceso.getAsignacionMemoria()) {
                throw new IllegalStateException("El proceso no tiene una asignación vigente en RAM.");
            }
            GestorPaginasVirtuales.ResultadoAcceso acceso = virtual.acceder(asignacion, posicionLogica);
            int direccion = acceso.direccionFisica();
            areaUsuario.set(direccion - inicioUsuario, acceso.instruccion());
            return acceso.instruccion();
        }
        return leer(obtenerDireccionFisica(proceso, posicionLogica));
    }

    public int getPosicionesLibres() {
        return estrategia.getPosicionesLibres();
    }

    public List<GestorPaginasVirtuales.EstadoPagina> getPaginasVirtuales(BCP proceso) {
        if (!(estrategia instanceof GestorPaginasVirtuales virtual) || proceso == null
                || proceso.getAsignacionMemoria() == null
                || asignaciones.get(proceso.getInicioPrograma()) != proceso.getAsignacionMemoria()) {
            return List.of();
        }
        return virtual.paginas(proceso.getAsignacionMemoria());
    }

    public int getFallosPaginacionVirtual() {
        return estrategia instanceof GestorPaginasVirtuales virtual ? virtual.getFallos() : 0;
    }

    public int getReemplazosPaginacionVirtual() {
        return estrategia instanceof GestorPaginasVirtuales virtual ? virtual.getReemplazos() : 0;
    }

    public TipoEstrategiaMemoria getTipoEstrategia() {
        return tipoEstrategia;
    }

    public MetodoAdministracionMemoria getMetodoMemoria() {
        return configuracionMemoria == null
                ? null
                : configuracionMemoria.metodo();
    }

    public ConfiguracionMemoria getConfiguracionMemoria() {
        return configuracionMemoria;
    }

    public EstrategiaAsignacionMemoria getEstrategia() {
        return estrategia;
    }

    public List<Hueco> getHuecos() {
        return estrategia instanceof AsignacionDeRegistros base
                ? base.getHuecos()
                : List.of();
    }

    // Permite distinguir una falta temporal de una solicitud imposible.
    public boolean puedeAlojarAlgunaVez(int cantidad) {
        if (cantidad <= 0) return false;
        if (estrategia instanceof GestorPaginacion paginacion) {
            return cantidad <= paginacion.getCapacidad();
        }
        if (estrategia instanceof GestorPaginasVirtuales virtual) {
            return virtual.hayEspacioDisponible(cantidad);
        }
        if (estrategia instanceof GestorParticionesFijas fija) {
            return cantidad <= fija.tamañoMaximo();
        }
        return cantidad <= size - inicioUsuario;
    }

    public List<Particion> getParticionesFijas() {
        if (estrategia instanceof GestorParticionesFijas fija) {
            return fija.getParticiones();
        }
        return List.of();
    }

    public List<Marco> getMarcos() {
        if (estrategia instanceof GestorPaginacion paginacion) {
            return paginacion.getMarcos();
        }
        if (estrategia instanceof GestorPaginasVirtuales virtual) {
            return virtual.marcos().stream()
                    .map(m -> new Marco(m.numero(), m.inicio(), m.tamaño(),
                            m.posicionesUtilizadas()))
                    .toList();
        }
        return List.of();
    }

    /** Solo devuelve tablas vigentes; un proceso suspendido no conserva marcos de RAM. */
    public TablaPaginas getTablaPaginas(BCP proceso) {
        if (!(estrategia instanceof GestorPaginacion paginacion) || proceso == null
                || proceso.getAsignacionMemoria() == null
                || asignaciones.get(proceso.getInicioPrograma()) != proceso.getAsignacionMemoria()) {
            return null;
        }
        return paginacion.getTabla(proceso.getAsignacionMemoria());
    }

    public int getSobrantePaginacion() {
        return estrategia instanceof GestorPaginacion paginacion ? paginacion.getSobrante() : 0;
    }

    public boolean puedeCambiarConfiguracion() {
        return asignaciones.isEmpty();
    }

    public boolean puedeCambiarEstrategia() {
        return puedeCambiarConfiguracion();
    }

    public void cambiarConfiguracionMemoria(ConfiguracionMemoria configuracion) {

        if (configuracion == null) {
            throw new IllegalArgumentException(
                    "Debe indicar una configuración de memoria.");
        }

        if (configuracion.equals(configuracionMemoria)) {
            return;
        }

        if (!puedeCambiarConfiguracion()) {
            throw new IllegalStateException(
                    "No se puede cambiar la configuración de memoria "
                            + "mientras haya programas cargados.");
        }

        EstrategiaAsignacionMemoria nueva = crearEstrategia(
                configuracion, inicioUsuario, size, almacenamiento);

        this.estrategia = nueva;
        this.configuracionMemoria = configuracion;
        this.tipoEstrategia = configuracion.estrategia();
        this.asignaciones.clear();

        for (int i = 0; i < areaUsuario.size(); i++) {
            areaUsuario.set(i, null);
        }
    }

    public void cambiarEstrategia(TipoEstrategiaMemoria tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException(
                    "Debe indicar una estrategia de memoria.");
        }
        if (tipo != tipoEstrategia && !puedeCambiarConfiguracion()) {
            throw new IllegalStateException(
                    "No se puede cambiar la estrategia mientras haya programas cargados.");
        }
        cambiarConfiguracionMemoria(new ConfiguracionDinamica(tipo));
    }

    /**
     * Guarda un BCP dentro del espacio reservado para el S.O.
     *
     * Reserva un bloque consecutivo con una posición por atributo del BCP.
     *
     * @param bcp BCP que se desea almacenar
     * @return dirección donde fue almacenado
     */
    public int guardarBCP(BCP bcp) {

        if (bcp == null) {
            throw new IllegalArgumentException("El BCP no puede ser null.");
        }

        int cantidad = BCP.getTamañoKernel();
        int consecutivos = 0;
        for (int i = 0; i < inicioUsuario; i++) {
            consecutivos = areaSistema.get(i) == null ? consecutivos + 1 : 0;
            if (consecutivos == cantidad) {
                int inicio = i - cantidad + 1;
                for (int j = 0; j < cantidad; j++)
                    areaSistema.set(inicio + j, bcp);
                bcp.setDireccionBCP(inicio);
                return inicio;
            }
        }

        throw new IllegalStateException("No hay espacio disponible para almacenar otro BCP.");
    }

    /**
     * Obtiene un BCP almacenado en el área del S.O.
     *
     * @param direccion dirección del BCP
     * @return BCP almacenado o null si la posición está libre
     */
    public BCP leerBCP(int direccion) {

        validarDireccionSO(direccion);

        return areaSistema.get(direccion);
    }

    /**
     * Libera todo el bloque ocupado por un BCP.
     *
     * @param direccion dirección del BCP
     */
    public void liberarBCP(int direccion) {

        validarDireccionSO(direccion);

        BCP bcp = areaSistema.get(direccion);
        if (bcp != null) {
            int inicio = bcp.getDireccionBCP();
            for (int i = 0; i < BCP.getTamañoKernel(); i++) {
                areaSistema.set(inicio + i, null);
            }
            bcp.setDireccionBCP(-1);
        }
    }

    /**
     * Obtiene una instrucción almacenada en el área de usuario.
     *
     *
     * @param direccion dirección de memoria
     * @return instrucción almacenada o null si la posición está libre
     */
    public Instruccion leer(int direccion) {

        validarDireccionUsuario(direccion);

        return areaUsuario.get(direccion - inicioUsuario);
    }

    /**
     * Escribe una instrucción dentro del área de usuario.
     *
     * @param direccion   dirección donde se almacenará
     * @param instruccion instrucción que será almacenada
     */
    public void escribir(int direccion, Instruccion instruccion) {

        validarDireccionUsuario(direccion);
        if (instruccion == null || areaUsuario.get(direccion - inicioUsuario) == null) {
            throw new IllegalArgumentException("Solo se pueden reemplazar instrucciones en posiciones asignadas.");
        }
        areaUsuario.set(direccion - inicioUsuario, instruccion);
    }

    /**
     * Libera las posiciones utilizadas por un programa.
     *
     * @param inicio         dirección inicial del programa
     * @param tamañoPrograma cantidad de posiciones ocupadas
     */
    public void liberarPrograma(int inicio, int tamañoPrograma) {

        AsignacionMemoria asignacion = getAsignacion(inicio);
        if (asignacion.getCantidad() != tamañoPrograma) {
            throw new IllegalArgumentException("La cantidad no coincide con la reserva.");
        }
        estrategia.liberar(asignacion);
        for (int direccion : asignacion.getPosiciones()) {
            if (direccion >= inicioUsuario && direccion < size) {
                areaUsuario.set(direccion - inicioUsuario, null);
            }
        }
        asignaciones.remove(inicio);
    }

    /**
     * Obtiene una representación simple del contenido de una posición.
     *
     */
    public String obtenerContenido(int direccion) {

        validarDireccion(direccion);

        if (direccion < inicioUsuario) {
            BCP bcp = areaSistema.get(direccion);
            return bcp == null ? "" : bcp.obtenerAtributoKernel(direccion - bcp.getDireccionBCP());
        }
        Instruccion instruccion = areaUsuario.get(direccion - inicioUsuario);
        return instruccion == null ? "" : instruccion.getOperacion();
    }

    /**
     * Reinicia completamente la memoria principal.
     */
    public void reiniciar() {
        estrategia.reiniciar();
        asignaciones.clear();
        areaSistema = new ArrayList<>(Collections.<BCP>nCopies(inicioUsuario, null));
        areaUsuario = new ArrayList<>(Collections.<Instruccion>nCopies(size - inicioUsuario, null));
    }

    /**
     * Valida una dirección general de memoria.
     */
    private void validarDireccion(int direccion) {

        if (direccion < 0 || direccion >= size) {
            throw new IllegalArgumentException("Dirección de memoria inválida: " + direccion);
        }
    }

    /**
     * Verifica que una dirección pertenezca al área del S.O.
     */
    private void validarDireccionSO(int direccion) {

        validarDireccion(direccion);

        if (direccion >= inicioUsuario) {
            throw new IllegalArgumentException("La dirección no pertenece al espacio del S.O.");
        }
    }

    /**
     * Verifica que una dirección pertenezca al área de usuario.
     */
    private void validarDireccionUsuario(int direccion) {

        validarDireccion(direccion);

        if (direccion < inicioUsuario) {
            throw new IllegalArgumentException("La dirección pertenece al espacio reservado para el S.O.");
        }
    }

    public int getSize() {
        return size;
    }

    public int getInicioUsuario() {
        return inicioUsuario;
    }

    public int getFinSO() {
        return inicioUsuario - 1;
    }
}
