package minipc.controller;

import minipc.model.Almacenamiento;
import minipc.model.BCP;
import minipc.model.CPU;
import minipc.model.EstadoProceso;
import minipc.model.Instruccion;
import minipc.model.Memoria;
import minipc.config.Configuracion;
import minipc.config.ConfigLoader;
import java.io.IOException;
import java.nio.file.Path;

import minipc.services.Despachador;
import minipc.services.Executor;
import minipc.services.planificadordeProcesos;
import minipc.services.GestorInterrupciones;
import minipc.services.GestorProcesos;
import minipc.services.GestorMemoriaVirtual;
import minipc.services.SistemaArchivos;

import java.util.List;
import java.util.ArrayList;
import minipc.parser.ASMReader;
import minipc.parser.ASMValidator;
import minipc.parser.ASMParser;

/**
 * Coordina los principales componentes de la Mini PC.
 *
 * Integra la admisión, planificación, memoria e interrupciones del backend.
 */
public class Controlador {

    private CPU cpu;
    private Memoria memoria;
    private Almacenamiento almacenamiento;

    private GestorProcesos gestorProcesos;
    private GestorMemoriaVirtual gestorMemoriaVirtual;
    private planificadordeProcesos planificador;
    private Despachador despachador;
    private SistemaArchivos sistemaArchivos;
    private GestorInterrupciones gestorInterrupciones;
    private Executor executor;

    // Instrucción correspondiente al segundo simulado actual.
    private Instruccion instruccionActual;
    private Configuracion configuracion;
    private final List<String> erroresEjecucion = new ArrayList<>();

    public Controlador(int tamañoMemoria, int inicioUsuario, int tamañoAlmacenamiento, int tamañoIndice, int tamañoMemoriaVirtual) {

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
        this.planificador = new planificadordeProcesos(algoritmo, gestorProcesos);
        this.despachador = new Despachador(cpu, gestorProcesos, planificador);
        this.sistemaArchivos = new SistemaArchivos(almacenamiento, gestorProcesos);
        this.gestorInterrupciones = new GestorInterrupciones(cpu, gestorProcesos, despachador, sistemaArchivos);
        this.executor = new Executor(cpu, gestorProcesos, gestorInterrupciones);

        this.instruccionActual = null;
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
            gestorProcesos.prepararProceso(bcp);
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
     * Cada llamada representa un clic del botón Siguiente.
     */
    public void ejecutarSiguiente() {

        /*
         * Si la CPU está libre se intenta despachar un proceso.
         */
        if (gestorProcesos.getProcesoActual() == null) {

            intentarReactivarSuspendidos();

            if (!planificador.hayProcesosPreparados()) {
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
            finalizarPorError(proceso, new IllegalStateException("El PC se encuentra fuera del rango del proceso."));
            return;
        }

        /*
         * FETCH.
         *
         * Se obtiene la instrucción que se ejecutará en este segundo.
         */
        if (instruccionActual == null) {

            instruccionActual = memoria.leer(cpu.getPC());

            if (instruccionActual == null) {
                finalizarPorError(proceso, new IllegalStateException("No existe una instrucción en la dirección " + cpu.getPC()));
                return;
            }

            cpu.setIR(instruccionActual.getTextoOriginal());
        }

        // Cada instrucción consume exactamente un segundo de CPU.
        gestorProcesos.incrementarTiempoCPU();

        boolean pcModificado;
        try {
            pcModificado = executor.ejecutar(instruccionActual);
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException error) {
            // Una falla posterior al cambio de proceso no debe finalizar al siguiente PID.
            if (gestorProcesos.getProcesoActual() != proceso) throw error;
            finalizarPorError(proceso, error);
            return;
        }

        reiniciarControlInstruccion();

        /*
         * Una interrupción puede haber bloqueado o finalizado el proceso
         * y el Despachador puede haber entregado la CPU a otro proceso.
         *
         * En ese caso no debemos modificar el PC del nuevo proceso.
         */
        if (gestorProcesos.getProcesoActual() != proceso) {
            intentarReactivarSuspendidos();
            if (gestorProcesos.getProcesoActual() == null && planificador.hayProcesosPreparados()) {
                despachador.despacharSiguiente();
            }
            return;
        }

        /*
         * JMP, JE o JNE pueden modificar directamente el PC.
         */
        if (!pcModificado) {
            cpu.incrementarPC();
        }

        proceso.guardarContexto(cpu);

        /*
         * Si se alcanzó el final sin utilizar INT 20H,
         * también se finaliza el proceso.
         */
        if (cpu.getPC() > proceso.getFinPrograma()) {
            gestorProcesos.finalizarProcesoActual();
            intentarReactivarSuspendidos();
            despachador.despacharSiguiente();
        }
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
     * Si no existen procesos ejecutables, el método se detiene.
     */
    public void ejecutarTodo() {

        while (!simulacionFinalizada()) {

            if (gestorProcesos.getProcesoActual() == null) {
                intentarReactivarSuspendidos();
                if (!planificador.hayProcesosPreparados()) {
                    break;
                }
            }

            ejecutarSiguiente();
        }
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
     * Limpia la referencia a la instrucción anterior.
     */
    private void reiniciarControlInstruccion() {
        instruccionActual = null;
    }

    /**
     * Indica si ya no existen procesos activos.
     */
    public boolean simulacionFinalizada() {
        return gestorProcesos.getCantidadProcesosActivos() == 0;
    }

    /** La suspensión de bloqueados es explícita; no existe una política de desalojo automático. */
    public void suspenderProceso(int pid) {
        BCP proceso = gestorProcesos.buscarProceso(pid);
        gestorMemoriaVirtual.suspenderProceso(proceso);
    }

    public void reiniciar() {
        reiniciar(configuracion == null || configuracion.isConservarArchivosAlReiniciar());
    }

    /** Reinicia la simulación y permite escoger si conservar los archivos. */
    public void reiniciar(boolean conservarArchivos) {
        erroresEjecucion.clear();
        gestorInterrupciones.reiniciar();
        gestorMemoriaVirtual.reiniciar();
        gestorProcesos.reiniciar();
        memoria.reiniciar();
        almacenamiento.reiniciar(!conservarArchivos);
        cpu.reiniciarCPU();
        reiniciarControlInstruccion();
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
}
