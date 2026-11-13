package minipc.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import minipc.config.Configuracion;
import minipc.model.BCP;
import minipc.model.EstadoProceso;
import minipc.model.Memoria;

/**
 * Administra los procesos existentes dentro de la Mini PC.
 *
 * Se encarga de crear procesos, asignar PID, administrar sus estados,
 * mantener la lista de trabajos, la cola de preparados y los procesos
 * bloqueados.
 *
 * También mantiene la referencia al proceso que se encuentra
 * actualmente utilizando la CPU.
 */
public class GestorProcesos {
    private AsignadorProcesosCPU asignadorProcesosCPU;
    private final int maxProcesos;
    private int maxProcesosEnRam = 25;

    private int siguientePid;

    private List<BCP> listaTrabajos;
    private Queue<BCP> colaPreparados;
    private List<BCP> bloqueados;

    private final Map<Integer, BCP> procesosEnEjecucion = new HashMap<>();

    private int instanteSimulado;
    private BCP procesoActual;
    private Memoria memoria;
    private boolean suspendido = false;

    /**
     * Crea el gestor de procesos.
     *
     * @param memoria memoria principal de la Mini PC
     */
    public GestorProcesos(Memoria memoria) {
        this(memoria, 5);
    }

    public GestorProcesos(Memoria memoria, int maxProcesos) {

        if (memoria == null) {
            throw new IllegalArgumentException("La memoria no puede ser null.");
        }

        this.memoria = memoria;
        if (maxProcesos <= 0) {
            throw new IllegalArgumentException("El máximo de procesos debe ser positivo.");
        }
        this.maxProcesos = maxProcesos;

        this.siguientePid = 1;

        this.listaTrabajos = new ArrayList<>();
        this.colaPreparados = new LinkedList<>();
        this.bloqueados = new ArrayList<>();

    }

    public void establecerAsignadorCPU(AsignadorProcesosCPU asignador) {

        if (asignador == null) {
            throw new IllegalArgumentException(
                    "El asignador no puede ser null.");
        }

        this.asignadorProcesosCPU = asignador;
    }

    public GestorProcesos(Memoria memoria, Configuracion configuracion) {
        this(memoria, configuracion.getMaxProcesos());
        this.maxProcesosEnRam = configuracion.getMaxProcesosEnRam();
    }

    public List<BCP> getProcesosEnRAM() {
        List<BCP> procesosEnRAM = new ArrayList<>();
        for (BCP proceso : listaTrabajos) {
            if (proceso.getEstado() != EstadoProceso.FINALIZADO
                    && proceso.getEstado() != EstadoProceso.PREPARADO_SUSPENDIDO
                    && proceso.getEstado() != EstadoProceso.BLOQUEADO_SUSPENDIDO
                    && proceso.getInicioPrograma() >= 0) {
                procesosEnRAM.add(proceso);
            }
        }
        return procesosEnRAM;
    }

    public boolean hayEspacioParaPrograma(int cantidad) {
        List<BCP> procesosEnRAM = getProcesosEnRAM();
        if (cantidad <= 0 || procesosEnRAM.size() >= maxProcesosEnRam) {
            return false;
        }
        return memoria.hayEspacioDisponible(cantidad);
    }

    /**
     * Crea un nuevo proceso y su respectivo BCP.
     *
     * El proceso inicialmente queda en estado NUEVO.
     *
     * @param inicioPrograma dirección inicial del programa en memoria
     * @param tamañoPrograma tamaño del programa
     * @param prioridad      prioridad asignada al proceso
     * @return BCP creado
     */
    public BCP crearProceso(int inicioPrograma, int tamañoPrograma, int prioridad) {

        if (inicioPrograma >= 0 && getProcesosEnRAM().size() >= maxProcesosEnRam) {
            throw new IllegalStateException("Se alcanzó el máximo de procesos en RAM.");
        }

        if (contarProcesosActivos() >= maxProcesos) {
            throw new IllegalStateException("Se alcanzó el máximo de " + maxProcesos + " procesos.");
        }

        BCP bcp = new BCP(siguientePid, inicioPrograma, tamañoPrograma);

        bcp.setPrioridad(prioridad);

        memoria.guardarBCP(bcp);
        siguientePid++;

        listaTrabajos.add(bcp);

        actualizarEnlacesBCP();

        return bcp;
    }

    public BCP crearProcesoSuspendido(int tamañoPrograma, int prioridad) {
        return crearProceso(-1, tamañoPrograma, prioridad);
    }

    /** Deshace una admisión que todavía no llegó a preparar el proceso. */
    public void descartarProcesoNuevo(BCP bcp) {
        validarProceso(bcp);
        if (bcp.getEstado() != EstadoProceso.NUEVO) {
            throw new IllegalStateException("Solo se puede descartar una admisión incompleta.");
        }
        if (bcp.getDireccionBCP() != -1) {
            memoria.liberarBCP(bcp.getDireccionBCP());
        }
        listaTrabajos.remove(bcp);
        colaPreparados.remove(bcp);
        bloqueados.remove(bcp);
        bcp.setDireccionSiguienteBCP(-1);
        if (bcp.getPid() == siguientePid - 1) {
            siguientePid--;
        }
        actualizarEnlacesBCP();
    }

    public void cerrarArchivoEnTodosLosProcesos(String nombre) {
        for (BCP bcp : listaTrabajos) {
            bcp.cerrarArchivo(nombre);
        }
    }

    public void iniciarEjecucion(BCP bcp) {
        iniciarEjecucion(1, bcp);
    }

    public void bloquearProcesoActual() {
        bloquearProcesoActual(1);
    }

    public void bloquearProcesoActual(int cpuId) {

        BCP procesoActual = procesosEnEjecucion.get(cpuId);

        if (procesoActual == null) {
            throw new IllegalStateException(
                    "No existe un proceso en ejecución.");
        }

        procesoActual.setEstado(EstadoProceso.BLOQUEADO);

        if (!bloqueados.contains(procesoActual)) {
            bloqueados.add(procesoActual);
        }

        procesosEnEjecucion.remove(cpuId);
    }

    public void reiniciar() {
        for (BCP bcp : listaTrabajos) {
            if (bcp.getDireccionBCP() != -1) {
                memoria.liberarBCP(bcp.getDireccionBCP());
            }
            bcp.setDireccionSiguienteBCP(-1);
            bcp.getArchivosAbiertos().clear();
        }
        listaTrabajos.clear();
        colaPreparados.clear();
        bloqueados.clear();
        procesosEnEjecucion.clear();
        instanteSimulado = 0;

        siguientePid = 1;
    }

    /**
     * Cambia un proceso NUEVO al estado PREPARADO y lo agrega
     * a la cola de preparados.
     *
     * Un proceso suspendido debe regresar primero desde memoria virtual.
     *
     * @param bcp proceso que se desea preparar
     */
    public void prepararProceso(BCP bcp) {

        validarProceso(bcp);

        if (bcp.getEstado() == EstadoProceso.FINALIZADO) {
            throw new IllegalStateException("No se puede preparar un proceso finalizado.");
        }

        if (bcp.getEstado() == EstadoProceso.PREPARADO_SUSPENDIDO
                || bcp.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO) {
            throw new IllegalStateException("El proceso debe regresar de memoria virtual antes de pasar a preparado.");
        }

        if (bcp.getEstado() == EstadoProceso.EJECUCION) {
            throw new IllegalStateException("El proceso ya se encuentra en ejecución.");
        }

        if (bcp.getEstado() == EstadoProceso.BLOQUEADO) {
            throw new IllegalStateException("Un proceso bloqueado debe ser desbloqueado antes de pasar a preparado.");
        }

        bcp.setEstado(EstadoProceso.PREPARADO);

        if (!colaPreparados.contains(bcp)) {
            colaPreparados.add(bcp);
        }
    }

    /**
     * Marca un proceso como el proceso actualmente en ejecución.
     *
     *
     * @param bcp proceso que recibe la CPU
     */

    public void iniciarEjecucion(int cpuId, BCP bcp) {

        validarProceso(bcp);
        if (bcp.getCpuAsignada() != cpuId) {

            throw new IllegalStateException(
                    "El PID " + bcp.getPid()
                            + " pertenece a CPU "
                            + bcp.getCpuAsignada()
                            + ", no a CPU " + cpuId + ".");
        }

        if (procesosEnEjecucion.containsKey(cpuId)) {
            throw new IllegalStateException(
                    "La CPU " + cpuId + " ya tiene un proceso.");
        }

        if (bcp.getEstado() != EstadoProceso.PREPARADO) {
            throw new IllegalStateException(
                    "Solo un proceso preparado puede pasar a ejecución.");
        }

        colaPreparados.remove(bcp);

        bcp.setEstado(EstadoProceso.EJECUCION);

        if (bcp.getHoraInicio() == null) {
            bcp.setHoraInicio(LocalDateTime.now());
        }

        bcp.registrarInicioSimulado(instanteSimulado);

        procesosEnEjecucion.put(cpuId, bcp);
    }

    public void expropiarProcesoActual(int cpuId) {

        BCP proceso = procesosEnEjecucion.get(cpuId);

        if (proceso == null
                || proceso.getEstado() != EstadoProceso.EJECUCION) {

            throw new IllegalStateException(
                    "No existe un proceso en ejecución en CPU "
                            + cpuId + ".");
        }

        procesosEnEjecucion.remove(cpuId);

        proceso.setEstado(EstadoProceso.PREPARADO);

        if (!colaPreparados.contains(proceso)) {

            colaPreparados.add(proceso);
        }
    }

    // se encarga de desbloquear un proceso que se encuentra bloqueado y lo pasa a
    // preparado, si el proceso se encuentra bloqueado suspendido lo pasa a
    // preparado suspendido
    public void desbloquearProceso(BCP bcp) {

        validarProceso(bcp);

        if (bcp.getEstado() == EstadoProceso.BLOQUEADO) {

            bloqueados.remove(bcp);
            bcp.setEstado(EstadoProceso.PREPARADO);

            if (!colaPreparados.contains(bcp)) {
                colaPreparados.add(bcp);
            }

            return;
        }

        if (bcp.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO) {
            bcp.setEstado(EstadoProceso.PREPARADO_SUSPENDIDO);
            return;
        }

        throw new IllegalStateException("El proceso no se encuentra bloqueado.");
    }

    // finaliza el proceso que actualmente se encuentra en ejecución, liberando la
    // memoria del programa y el BCP de memoria, además de cerrar los archivos
    // abiertos por el proceso
    public void finalizarProcesoActual() {
        finalizarProcesoActual(1);
    }

    public void finalizarProcesoActual(int cpuId) {

        BCP procesoActual = procesosEnEjecucion.get(cpuId);

        if (procesoActual == null) {
            throw new IllegalStateException(
                    "No existe un proceso en ejecución.");
        }

        BCP bcp = procesoActual;

        bcp.setEstado(EstadoProceso.FINALIZADO);

        bcp.setHoraFinal(LocalDateTime.now());

        bcp.registrarFinSimulado(instanteSimulado + 1);

        bcp.getArchivosAbiertos().clear();

        colaPreparados.remove(bcp);
        bloqueados.remove(bcp);

        memoria.liberarPrograma(
                bcp.getInicioPrograma(),
                bcp.getTamañoPrograma());

        int direccionBCP = bcp.getDireccionBCP();

        if (direccionBCP != -1) {

            memoria.liberarBCP(direccionBCP);

            bcp.setDireccionBCP(-1);
        }

        bcp.setDireccionSiguienteBCP(-1);

        procesosEnEjecucion.remove(cpuId);
        if (asignadorProcesosCPU != null) {

            asignadorProcesosCPU.liberarCupo(bcp);
        }
        actualizarEnlacesBCP();
    }

    // Incrementa el contador de tiempo de CPU del proceso que actualmente se
    // encuentra en ejecución.
    public void incrementarTiempoCPU() {
        incrementarTiempoCPU(1);
    }

    public void incrementarTiempoCPU(int cpuId) {

        BCP procesoActual = procesosEnEjecucion.get(cpuId);

        if (procesoActual != null) {
            procesoActual.incrementarTiempoCPU();
        }
    }

    // busca un proceso por su PID dentro de la lista de trabajos del gestor.
    // Devuelve el BCP correspondiente si se encuentra, o null si no existe.
    public BCP buscarProceso(int pid) {

        for (BCP bcp : listaTrabajos) {

            if (bcp.getPid() == pid) {
                return bcp;
            }
        }

        return null;
    }

    private int contarProcesosActivos() {

        int cantidad = 0;

        for (BCP bcp : listaTrabajos) {

            if (bcp.getEstado() != EstadoProceso.FINALIZADO) {
                cantidad++;
            }
        }

        return cantidad;
    }

    // Actualiza los enlaces de los BCP en memoria para reflejar la lista de
    // trabajos actual. Esto asegura que cada BCP apunte al siguiente BCP en la
    // lista, o -1 si es el último.
    private void actualizarEnlacesBCP() {

        BCP anterior = null;

        for (BCP bcp : listaTrabajos) {

            if (bcp.getDireccionBCP() == -1) {
                continue;
            }

            if (anterior != null) {
                anterior.setDireccionSiguienteBCP(bcp.getDireccionBCP());
            }

            bcp.setDireccionSiguienteBCP(-1);
            anterior = bcp;
        }
    }

    // Valida que el proceso no sea null y que pertenezca a la lista de trabajos del
    // gestor.
    private void validarProceso(BCP bcp) {

        if (bcp == null) {
            throw new IllegalArgumentException("El proceso no puede ser null.");
        }

        if (!listaTrabajos.contains(bcp)) {
            throw new IllegalArgumentException("El proceso no pertenece al gestor.");
        }
    }

    public List<BCP> getListaTrabajos() {
        return listaTrabajos;
    }

    public Queue<BCP> getColaPreparados() {
        return colaPreparados;
    }

    public List<BCP> getBloqueados() {
        return bloqueados;
    }

    public BCP getProcesoActual() {
        return getProcesoActual(1);
    }

    public BCP getProcesoActual(int cpuId) {
        return procesosEnEjecucion.get(cpuId);
    }

    public int getCantidadProcesosActivos() {
        return contarProcesosActivos();
    }

    public boolean getSuspendido() {
        return suspendido;
    }

    public void setSuspendido() {
        if (!suspendido) {
            suspendido = true;
        } else {
            suspendido = true;
        }

    }

    public void establecerInstanteSimulado(int instanteSimulado) {

        if (instanteSimulado < 0) {
            throw new IllegalArgumentException(
                    "El tiempo simulado no puede ser negativo.");
        }

        this.instanteSimulado = instanteSimulado;
    }

    /** Segundo simulado actual, usado por los planificadores que dependen del tiempo. */
    public int getInstanteSimulado() {

        return instanteSimulado;
    }

    public void prepararProcesosLlegados(int tiempoActual) {

        for (BCP proceso : listaTrabajos) {

            // Solo revisar procesos que todavía están en NUEVO.
            if (proceso.getEstado() != EstadoProceso.NUEVO) {
                continue;
            }

            // Todavía no ha llegado.
            if (proceso.getTiempoLlegada() > tiempoActual) {
                continue;
            }

            // El programa ya está cargado en RAM.
            if (proceso.getInicioPrograma() >= 0) {

                prepararProceso(proceso);

            } else {

                // El programa está esperando espacio en RAM.
                proceso.setEstado(
                        EstadoProceso.PREPARADO_SUSPENDIDO);
            }
        }
    }

}
