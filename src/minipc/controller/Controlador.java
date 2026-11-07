package minipc.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import minipc.config.ConfigLoader;
import minipc.config.Configuracion;
import minipc.model.Almacenamiento;
import minipc.model.BCP;
import minipc.model.CPU;
import minipc.model.EstadoProceso;
import minipc.model.Instruccion;
import minipc.model.Memoria;
import minipc.parser.ASMParser;
import minipc.parser.ASMReader;
import minipc.parser.ASMValidator;
import minipc.services.Despachador;
import minipc.services.Executor;
import minipc.services.FCFS;
import minipc.services.GestorInterrupciones;
import minipc.services.GestorMemoriaVirtual;
import minipc.services.GestorProcesos;
import minipc.services.SistemaArchivos;

/**
 * Coordina los principales componentes de la Mini PC.
 *
 * Integra la admisión, planificación, memoria e interrupciones del backend.
 */
public class Controlador {

    private CPU cpu;
    private Memoria memoria;
    private Almacenamiento almacenamiento;
    private int tiempoSimulado = 0;
    private GestorProcesos gestorProcesos;
    private GestorMemoriaVirtual gestorMemoriaVirtual;
    private FCFS fcfs;
    private Despachador despachador;
    private SistemaArchivos sistemaArchivos;
    private GestorInterrupciones gestorInterrupciones;
    private Executor executor;

    // Control de la instrucción que está consumiendo ticks de CPU.
    private Instruccion instruccionActual;
    private int ticksRestantes;
    private Configuracion configuracion;
    private final List<String> erroresEjecucion = new ArrayList<>();

    public Controlador(int tamañoMemoria, int inicioUsuario, int tamañoAlmacenamiento, int tamañoIndice,
            int tamañoMemoriaVirtual) {

        this.cpu = new CPU();
        this.memoria = new Memoria(tamañoMemoria, inicioUsuario);
        this.almacenamiento = new Almacenamiento(tamañoAlmacenamiento, tamañoIndice, tamañoMemoriaVirtual);
        inicializarServicios(null);
    }

    public Controlador(Configuracion configuracion) {
        this(configuracion.getTamañoMemoria(), configuracion.getInicioUsuario(),
                configuracion.getTamañoAlmacenamiento(), configuracion.getTamañoIndice(),
                configuracion.getTamañoMemoriaVirtual());
        this.configuracion = configuracion;
        inicializarServicios(configuracion);
    }

    public Controlador(Path archivoConfiguracion) throws IOException {
        this(new ConfigLoader().cargar(archivoConfiguracion));
    }

    private void inicializarServicios(Configuracion configuracion) {
        this.gestorProcesos = configuracion == null ? new GestorProcesos(memoria)
                : new GestorProcesos(memoria, configuracion);
        this.gestorMemoriaVirtual = new GestorMemoriaVirtual(memoria, almacenamiento, gestorProcesos);
        String algoritmo = configuracion == null ? "FCFS" : configuracion.getAlgoritmo();
        this.fcfs = switch (algoritmo) {
            case "FCFS" -> new FCFS(gestorProcesos);
            default -> throw new IllegalArgumentException("Algoritmo no implementado: " + algoritmo);
        };
        this.despachador = new Despachador(cpu, gestorProcesos, fcfs);
        this.sistemaArchivos = new SistemaArchivos(almacenamiento, gestorProcesos);
        this.gestorInterrupciones = new GestorInterrupciones(cpu, gestorProcesos, despachador, sistemaArchivos);
        this.executor = new Executor(cpu, gestorProcesos, gestorInterrupciones);

        this.instruccionActual = null;
        this.ticksRestantes = 0;
    }

    /**
     * Carga un programa en memoria principal y crea su proceso.
     *
     * El programa se agrega sin eliminar los procesos que ya se
     * encuentran cargados.
     */
    public BCP cargarPrograma(List<Instruccion> programa) {
        return cargarPrograma(programa, 0);
    }

    /**
     * Carga un programa con una prioridad determinada.
     */
    public BCP cargarPrograma(List<Instruccion> programa, int prioridad) {
        int numero = 1;
        while (almacenamiento.existeArchivo("programa_" + numero + ".asm")) {
            numero++;
        }
        return cargarPrograma("programa_" + numero + ".asm", programa, prioridad);
    }

    /** Importa, valida y guarda el archivo real antes de admitir su proceso. */
    public BCP cargarPrograma(Path archivo) throws IOException {
        List<String> lineas = new ASMReader().leerArchivo(archivo);
        List<String> errores = new ASMValidator().validarPrograma(lineas);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(archivo.getFileName() + ":\n" + String.join("\n", errores));
        }
        return cargarPrograma(archivo.getFileName().toString(), new ASMParser().parsear(lineas), 0);
    }

    public BCP cargarPrograma(String nombre, List<Instruccion> programa) {
        return cargarPrograma(nombre, programa, 0);
    }

    /** Una admisión fallida deshace también la nueva copia en disco. */
    public BCP cargarPrograma(String nombre, List<Instruccion> programa, int prioridad) {
        almacenamiento.guardarPrograma(nombre, programa);
        try {
            return cargarProgramaDesdeDisco(nombre, prioridad);
        } catch (RuntimeException e) {
            almacenamiento.eliminarArchivo(nombre);
            throw e;
        }
    }

    public BCP cargarProgramaDesdeDisco(String nombre) {
        return cargarProgramaDesdeDisco(nombre, 0);
    }

    /** Permite volver a ejecutar un programa sin duplicar su copia en disco. */
    public BCP cargarProgramaDesdeDisco(String nombre, int prioridad) {
        return admitirPrograma(almacenamiento.leerPrograma(nombre), prioridad);
    }

    private BCP admitirPrograma(List<Instruccion> programa, int prioridad) {

        if (programa == null || programa.isEmpty()) {
            throw new IllegalArgumentException("No se puede cargar un programa vacío.");
        }
        if (programa.size() > memoria.getSize() - memoria.getInicioUsuario()) {
            throw new IllegalArgumentException("El programa supera toda el área de usuario de RAM.");
        }

        if (!gestorProcesos.hayEspacioParaPrograma(programa.size())) {
            if (!almacenamiento.hayEspacioVirtualDisponible(programa.size())) {
                throw new IllegalStateException("No existe espacio suficiente ni en RAM ni en memoria virtual.");
            }

            BCP bcp = gestorProcesos.crearProcesoSuspendido(programa.size(), prioridad);
            try {
                gestorMemoriaVirtual.guardarProcesoNuevo(bcp, programa);
            } catch (RuntimeException e) {
                gestorProcesos.descartarProcesoNuevo(bcp);
                throw e;
            }
            return bcp;
        }

        int inicioPrograma = -1;
        BCP bcp = null;
        try {
            inicioPrograma = memoria.cargarPrograma(programa);
            bcp = gestorProcesos.crearProceso(inicioPrograma, programa.size(), prioridad);

            return bcp;

        } catch (RuntimeException e) {
            if (inicioPrograma != -1) {
                memoria.liberarPrograma(inicioPrograma, programa.size());
            }
            if (bcp != null && bcp.getEstado() == EstadoProceso.NUEVO) {
                gestorProcesos.descartarProcesoNuevo(bcp);
            }
            throw e;
        }
    }

    /**
     * Ejecuta un segundo/tick simulado.
     *
     * Cada llamada representa un clic del botón Siguiente
     * o un segundo de ejecución automática.
     */
    public void ejecutarSiguiente() {

        if (simulacionFinalizada()) {
            return;
        }

        // Verificar qué procesos alcanzaron su tiempo de llegada.
        gestorProcesos.prepararProcesosLlegados(tiempoSimulado);

        // Ejecutar un paso de CPU.
        ejecutarPasoCPU();

        // Avanzar el reloj global, incluso si la CPU estuvo ociosa.
        tiempoSimulado++;
    }

    /**
     * Ejecuta un paso del procesador utilizando FCFS.
     *
     * Conserva la lógica de ejecución de instrucciones,
     * interrupciones, cambios de contexto y finalización.
     */
    private void ejecutarPasoCPU() {

        /*
         * Si la CPU está libre, se intenta despachar un proceso.
         */
        if (gestorProcesos.getProcesoActual() == null) {

            intentarReactivarSuspendidos();

            if (!fcfs.hayProcesosPreparados()) {
                return;
            }

            despachador.despacharSiguiente();
            reiniciarControlInstruccion();
        }

        BCP proceso = gestorProcesos.getProcesoActual();

        if (proceso == null) {
            return;
        }

        /*
         * Si el PC ya superó el programa, el proceso termina.
         */
        if (cpu.getPC() > proceso.getFinPrograma()) {

            despachador.guardarContextoActual();
            gestorProcesos.finalizarProcesoActual();

            reiniciarControlInstruccion();
            intentarReactivarSuspendidos();
            despachador.despacharSiguiente();

            return;
        }

        if (cpu.getPC() < proceso.getInicioPrograma()) {

            finalizarPorError(
                    proceso,
                    new IllegalStateException(
                            "El PC se encuentra fuera del rango del proceso."));

            return;
        }

        /*
         * FETCH.
         *
         * Se obtiene una nueva instrucción solamente cuando
         * no existe otra consumiendo ticks de CPU.
         */
        if (instruccionActual == null) {

            instruccionActual = memoria.leer(cpu.getPC());

            if (instruccionActual == null) {

                finalizarPorError(
                        proceso,
                        new IllegalStateException(
                                "No existe una instrucción en la dirección "
                                        + cpu.getPC()));

                return;
            }

            cpu.setIR(instruccionActual.getTextoOriginal());

            ticksRestantes = instruccionActual.getPeso();
        }

        /*
         * Consumir un segundo de CPU.
         */
        if (instruccionActual.getPeso() > 0) {

            gestorProcesos.incrementarTiempoCPU();

            ticksRestantes--;

            /*
             * La instrucción todavía necesita más tiempo.
             */
            if (ticksRestantes > 0) {

                proceso.guardarContexto(cpu);

                return;
            }
        }

        /*
         * Ejecutar el efecto de la instrucción una vez
         * consumidos los ticks correspondientes.
         */
        boolean pcModificado;

        try {

            pcModificado = executor.ejecutar(instruccionActual);

        } catch (IllegalArgumentException
                | IllegalStateException
                | ArithmeticException error) {

            /*
             * Evitar finalizar un proceso distinto si una
             * interrupción produjo un cambio de contexto.
             */
            if (gestorProcesos.getProcesoActual() != proceso) {
                throw error;
            }

            finalizarPorError(proceso, error);

            return;
        }

        reiniciarControlInstruccion();

        /*
         * Una interrupción puede bloquear o finalizar el
         * proceso actual y asignar la CPU a otro proceso.
         */
        if (gestorProcesos.getProcesoActual() != proceso) {

            intentarReactivarSuspendidos();

            if (gestorProcesos.getProcesoActual() == null
                    && fcfs.hayProcesosPreparados()) {

                despachador.despacharSiguiente();
            }

            return;
        }

        /*
         * Los saltos JMP, JE y JNE pueden modificar
         * directamente el Program Counter.
         */
        if (!pcModificado) {
            cpu.incrementarPC();
        }

        proceso.guardarContexto(cpu);

        /*
         * Si el programa llegó a su última instrucción
         * sin utilizar INT 20H, también debe finalizar.
         */
        if (cpu.getPC() > proceso.getFinPrograma()) {

            gestorProcesos.finalizarProcesoActual();

            intentarReactivarSuspendidos();

            despachador.despacharSiguiente();
        }
    }

    /*
     * Si todos los procesos tienen una llegada posterior a cero, todavía no habrá
     * ninguno en READY.
     */
    public boolean puedeAvanzar() {

        if (gestorProcesos.getProcesoActual() != null) {
            return true;
        }

        if (!gestorProcesos.getColaPreparados().isEmpty()) {
            return true;
        }

        for (BCP proceso : gestorProcesos.getListaTrabajos()) {

            // Procesos que todavía deben llegar.
            if (proceso.getEstado() == EstadoProceso.NUEVO) {
                return true;
            }

            // Procesos suspendidos que podrían reactivarse.
            if (proceso.getEstado() == EstadoProceso.PREPARADO_SUSPENDIDO
                    && gestorMemoriaVirtual.estaEnMemoriaVirtual(proceso)
                    && gestorProcesos.hayEspacioParaPrograma(
                            proceso.getTamañoPrograma())) {

                return true;
            }
        }

        return false;
    }

    private void finalizarPorError(BCP proceso, RuntimeException error) {
        despachador.guardarContextoActual();
        proceso.setMotivoError(error.getMessage());
        erroresEjecucion.add("ERROR DE EJECUCIÓN | PID " + proceso.getPid()
                + " | PC=" + proceso.getPC() + " | IR=" + proceso.getIR()
                + " | " + error.getMessage());
        gestorProcesos.finalizarProcesoActual();
        reiniciarControlInstruccion();
        intentarReactivarSuspendidos();
        despachador.despacharSiguiente();
    }

    public List<String> getErroresEjecucion() {
        return new ArrayList<>(erroresEjecucion);
    }

    /**
     * Ejecuta automáticamente mientras existan procesos capaces
     * de utilizar la CPU.
     *
     * Si todos los procesos quedan bloqueados esperando entrada,
     * el método se detiene para permitir interacción con el usuario.
     */
    public void ejecutarTodo() {

        while (!simulacionFinalizada() && puedeAvanzar()) {
            ejecutarSiguiente();
        }
    }

    /**
     * Entrega una entrada de teclado al proceso que está esperando
     * una INT 09H.
     */
    public BCP recibirEntradaTeclado(String entrada) {
        BCP proceso = gestorInterrupciones.recibirEntradaTeclado(entrada);
        intentarReactivarSuspendidos();
        if (gestorProcesos.getProcesoActual() == null && fcfs.hayProcesosPreparados()) {
            despachador.despacharSiguiente();
        }
        return proceso;
    }

    private void intentarReactivarSuspendidos() {
        boolean procesoReactivado;
        do {
            procesoReactivado = false;
            for (BCP bcp : gestorProcesos.getListaTrabajos()) {
                if (bcp.getEstado() == EstadoProceso.PREPARADO_SUSPENDIDO
                        && gestorMemoriaVirtual.estaEnMemoriaVirtual(bcp)
                        && gestorProcesos.hayEspacioParaPrograma(bcp.getTamañoPrograma())) {
                    gestorMemoriaVirtual.reactivarProceso(bcp);
                    procesoReactivado = true;
                    break;
                }
            }
        } while (procesoReactivado);
    }

    /**
     * Limpia el control de ticks correspondiente a la instrucción anterior.
     */
    private void reiniciarControlInstruccion() {
        instruccionActual = null;
        ticksRestantes = 0;
    }

    /**
     * Indica si ya no existen procesos activos.
     */
    public boolean simulacionFinalizada() {
        return gestorProcesos.getCantidadProcesosActivos() == 0;
    }

    /**
     * La suspensión de bloqueados es explícita; no existe una política de desalojo
     * automático.
     */
    public void suspenderProceso(int pid) {
        BCP proceso = gestorProcesos.buscarProceso(pid);
        gestorMemoriaVirtual.suspenderProceso(proceso);
    }

    public void reiniciar() {
        reiniciar(configuracion == null || configuracion.isConservarArchivosAlReiniciar());
    }

    /** Reinicia la simulación y permite escoger si conservar los archivos. */
    public void reiniciar(boolean conservarArchivos) {
        tiempoSimulado = 0;
        erroresEjecucion.clear();
        gestorInterrupciones.reiniciar();
        gestorMemoriaVirtual.reiniciar();
        gestorProcesos.reiniciar();
        memoria.reiniciar();
        almacenamiento.reiniciar(!conservarArchivos);
        cpu.reiniciarCPU();
        reiniciarControlInstruccion();
    }

    public void configurarLlegada(int pid, int llegada) {

        if (llegada < 0) {
            throw new IllegalArgumentException(
                    "El tiempo de llegada no puede ser negativo.");
        }

        BCP proceso = gestorProcesos.buscarProceso(pid);

        if (proceso == null) {
            throw new IllegalArgumentException(
                    "No existe el proceso " + pid);
        }

        proceso.setTiempoLlegada(llegada);
    }

    public CPU getCPU() {
        return cpu;
    }

    public Memoria getMemoria() {
        return memoria;
    }

    public Almacenamiento getAlmacenamiento() {
        return almacenamiento;
    }

    public GestorProcesos getGestorProcesos() {
        return gestorProcesos;
    }

    public GestorMemoriaVirtual getGestorMemoriaVirtual() {
        return gestorMemoriaVirtual;
    }

    public GestorInterrupciones getGestorInterrupciones() {
        return gestorInterrupciones;
    }

    public BCP getProcesoActual() {
        return gestorProcesos.getProcesoActual();
    }

    public String getUltimaSalida() {
        return gestorInterrupciones.getUltimaSalida();
    }

    public int getSizeMemoria() {
        return memoria.getSize();
    }

    public int getInicioUsuario() {
        return memoria.getInicioUsuario();
    }

    public int getFinSO() {
        return memoria.getFinSO();
    }

    public int getTiempoSimulado() {
        return tiempoSimulado;
    }
}
