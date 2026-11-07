package minipc.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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

    private final int maxProcesos;
    private int maxProcesosEnRam = 5;

    private int siguientePid;

    private List<BCP> listaTrabajos;
    private Queue<BCP> colaPreparados;
    private List<BCP> bloqueados;

    private BCP procesoActual;

    private Memoria memoria;

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

        this.procesoActual = null;
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
     * @param prioridad prioridad asignada al proceso
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
        procesoActual = null;
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
    public void iniciarEjecucion(BCP bcp) {

        validarProceso(bcp);

        if (procesoActual != null && procesoActual != bcp) {
            throw new IllegalStateException("Ya existe un proceso utilizando la CPU.");
        }

        if (bcp.getEstado() != EstadoProceso.PREPARADO) {
            throw new IllegalStateException("Solo un proceso preparado puede pasar a ejecución.");
        }

        colaPreparados.remove(bcp);
        bcp.setEstado(EstadoProceso.EJECUCION);

        if (bcp.getHoraInicio() == null) {
            bcp.setHoraInicio(LocalDateTime.now());
        }

        procesoActual = bcp;
    }

    /**
     * Bloquea el proceso que actualmente utiliza la CPU.
     *
     * Se utilizará, por ejemplo, cuando el proceso quede esperando
     * una operación de entrada.
     */
    public void bloquearProcesoActual() {

        if (procesoActual == null) {
            throw new IllegalStateException("No existe un proceso en ejecución.");
        }

        procesoActual.setEstado(EstadoProceso.BLOQUEADO);

        if (!bloqueados.contains(procesoActual)) {
            bloqueados.add(procesoActual);
        }

        procesoActual = null;
    }

    /**
     * Desbloquea un proceso cuando termina el evento que esperaba.
     *
     * Si está en memoria principal:
     * BLOQUEADO -> PREPARADO.
     *
     * Si continúa en memoria virtual:
     * BLOQUEADO_SUSPENDIDO -> PREPARADO_SUSPENDIDO.
     *
     * @param bcp proceso que terminó de esperar el evento
     */
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

    /**
     * Finaliza el proceso que actualmente utiliza la CPU.
     *
     * Se registra la hora final y se libera la memoria utilizada
     * por el programa y su BCP.
     */
    public void finalizarProcesoActual() {

        if (procesoActual == null) {
            throw new IllegalStateException("No existe un proceso en ejecución.");
        }

        BCP bcp = procesoActual;

        bcp.setEstado(EstadoProceso.FINALIZADO);
        bcp.setHoraFinal(LocalDateTime.now());
        bcp.getArchivosAbiertos().clear();

        colaPreparados.remove(bcp);
        bloqueados.remove(bcp);

        memoria.liberarPrograma(bcp.getInicioPrograma(), bcp.getTamañoPrograma());

        int direccionBCP = bcp.getDireccionBCP();

        if (direccionBCP != -1) {
            memoria.liberarBCP(direccionBCP);
            bcp.setDireccionBCP(-1);
        }

        bcp.setDireccionSiguienteBCP(-1);

        procesoActual = null;

        actualizarEnlacesBCP();
    }

    /**
     * Incrementa un segundo simulado de CPU al proceso actual.
     *
     * Este método se utilizará por cada clic/tick en el que el
     * proceso esté utilizando la CPU.
     */
    public void incrementarTiempoCPU() {

        if (procesoActual != null) {
            procesoActual.incrementarTiempoCPU();
        }
    }

    /**
     * Busca un proceso mediante su PID.
     *
     * @param pid identificador del proceso
     * @return BCP correspondiente o null si no existe
     */
    public BCP buscarProceso(int pid) {

        for (BCP bcp : listaTrabajos) {

            if (bcp.getPid() == pid) {
                return bcp;
            }
        }

        return null;
    }

    /**
     * Cuenta los procesos que todavía no han finalizado.
     */
    private int contarProcesosActivos() {

        int cantidad = 0;

        for (BCP bcp : listaTrabajos) {

            if (bcp.getEstado() != EstadoProceso.FINALIZADO) {
                cantidad++;
            }
        }

        return cantidad;
    }

    /**
     * Actualiza las direcciones que enlazan un BCP con el siguiente.
     *
     * Solo se toman en cuenta BCP que actualmente se encuentren
     * almacenados en memoria principal.
     */
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

    /**
     * Verifica que el proceso recibido exista dentro del gestor.
     */
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
        return procesoActual;
    }

    public int getCantidadProcesosActivos() {
        return contarProcesosActivos();
    }


/**
 * Comprueba qué procesos alcanzaron su tiempo de llegada.
 *
 * Los procesos en RAM pasan de NUEVO a PREPARADO.
 * Los procesos en memoria virtual pasan de NUEVO
 * a PREPARADO_SUSPENDIDO.
 */
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
                    EstadoProceso.PREPARADO_SUSPENDIDO
            );
        }
    }
}

}
