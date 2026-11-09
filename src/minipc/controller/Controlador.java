package minipc.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import minipc.config.ConfigLoader;
import minipc.config.Configuracion;
import minipc.model.Almacenamiento;
import minipc.model.BCP;
import minipc.model.CPU;
import minipc.model.EstadoProceso;
import minipc.model.Instruccion;
import minipc.model.Memoria;
import minipc.model.TipoAlgoritmo;
import minipc.parser.ASMParser;
import minipc.parser.ASMReader;
import minipc.parser.ASMValidator;
import minipc.services.AsignadorProcesosCPU;
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
    private final List<ContextoNucleo> nucleos = new ArrayList<>();
    private Memoria memoria;
    private Almacenamiento almacenamiento;
    private int tiempoSimulado = 0;
    private AsignadorProcesosCPU asignadorProcesosCPU;
    private GestorProcesos gestorProcesos;
    private GestorMemoriaVirtual gestorMemoriaVirtual;
    private FCFS fcfs;
    private Despachador despachador;
    private SistemaArchivos sistemaArchivos;
    private GestorInterrupciones gestorInterrupciones;
    private Executor executor;
    private TipoAlgoritmo algoritmoSeleccionado;

    // Control de la instrucción que está consumiendo ticks de CPU.

    private Configuracion configuracion;
    private final List<String> erroresEjecucion = new ArrayList<>();

    public Controlador(int tamañoMemoria, int inicioUsuario, int tamañoAlmacenamiento, int tamañoIndice,
            int tamañoMemoriaVirtual) {

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

        this.gestorProcesos = configuracion == null
                ? new GestorProcesos(memoria)
                : new GestorProcesos(memoria, configuracion);

        this.gestorMemoriaVirtual = new GestorMemoriaVirtual(
                memoria,
                almacenamiento,
                gestorProcesos);

        String algoritmo = configuracion == null
                ? "FCFS"
                : configuracion.getAlgoritmo();

        this.fcfs = switch (algoritmo) {

            case "FCFS" -> new FCFS(gestorProcesos);

            default -> throw new IllegalArgumentException(
                    "Algoritmo no implementado: " + algoritmo);
        };

        // Crear un único sistema de archivos compartido.
        this.sistemaArchivos = new SistemaArchivos(
                almacenamiento,
                gestorProcesos);

        /*
         * Crear los núcleos.
         *
         * Este método también establece las referencias
         * principales cpu, despachador, gestorInterrupciones
         * y executor a partir del primer núcleo.
         */
        inicializarNucleos(1);
    }

    private void inicializarNucleos(int cantidad) {
        asignadorProcesosCPU = new AsignadorProcesosCPU(cantidad);

        gestorProcesos.establecerAsignadorCPU(
                asignadorProcesosCPU);

        nucleos.clear();

        for (int numero = 1; numero <= cantidad; numero++) {

            CPU procesador = new CPU(numero);

            Despachador nuevoDespachador = new Despachador(
                    procesador,
                    gestorProcesos,
                    fcfs);

            GestorInterrupciones nuevasInterrupciones = new GestorInterrupciones(
                    procesador,
                    gestorProcesos,
                    nuevoDespachador,
                    sistemaArchivos);

            Executor nuevoExecutor = new Executor(
                    procesador,
                    gestorProcesos,
                    nuevasInterrupciones);

            nucleos.add(
                    new ContextoNucleo(
                            procesador,
                            nuevoDespachador,
                            nuevasInterrupciones,
                            nuevoExecutor));
        }

        ContextoNucleo primero = nucleos.get(0);

        cpu = primero.getCpu();
        despachador = primero.getDespachador();
        gestorInterrupciones = primero.getInterrupciones();
        executor = primero.getExecutor();
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
        if (tiempoSimulado > 0) {

            int procesosAsignados = 0;

            for (int cpuId = 1; cpuId <= nucleos.size(); cpuId++) {

                procesosAsignados += asignadorProcesosCPU
                        .getCantidadProcesos(cpuId);
            }

            if (procesosAsignados >= nucleos.size() * 5) {

                throw new IllegalStateException(
                        "No hay cupos disponibles "
                                + "en las CPU configuradas.");
            }
        }
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
                asignarSiHayCupo(bcp);
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
            asignarSiHayCupo(bcp);

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
     *
     *
     * Ejecuta un
     * segundo de simulación.**
     * Durante cada tick:*1.
     * Se verifican
     * los procesos
     * que llegaron.*2.
     * Se revisan
     * los procesos suspendidos.*3.
     * Todas las
     * CPU ejecutan
     * un paso.*4.
     * Cada CPU
     * registra su
     * tiempo ocupado
     * u ocioso.*5.
     * El reloj
     * global avanza
     * un segundo.
     */

    public void ejecutarSiguiente() {

        // No continuar si terminaron todos los procesos.
        if (simulacionFinalizada()) {
            return;
        }
        verificarAsignacionesCompletas();

        // Comprobar que el usuario seleccionó un algoritmo.
        validarAlgoritmoSeleccionado();

        // Establecer el segundo actual de la simulación.
        gestorProcesos.establecerInstanteSimulado(
                tiempoSimulado);

        // Admitir procesos cuyo tiempo de llegada se cumplió.
        gestorProcesos.prepararProcesosLlegados(
                tiempoSimulado);

        // Intentar recuperar procesos desde memoria virtual.
        intentarReactivarSuspendidos();

        // Todas las CPU ejecutan dentro del mismo segundo simulado.
        for (ContextoNucleo nucleo : nucleos) {

            // Ejecutar un paso de esta CPU.
            boolean ocupada = ejecutarPasoCPU(nucleo);

            // Registrar si estuvo ocupada u ociosa.
            nucleo.getCpu().registrarTick(ocupada);
        }

        // Verificar que ningún PID esté asignado a dos CPU.
        verificarProcesosEnCPU();

        // Avanzar una única vez el reloj global.
        tiempoSimulado++;
    }

    private void validarAlgoritmoSeleccionado() {

        if (algoritmoSeleccionado == null) {

            throw new IllegalStateException(
                    "Debe seleccionar un algoritmo "
                            + "antes de iniciar la ejecución.");
        }

        // Validación temporal mientras se implementan
        // los demás algoritmos.
        if (algoritmoSeleccionado != TipoAlgoritmo.FCFS) {

            throw new UnsupportedOperationException(
                    "El algoritmo " + algoritmoSeleccionado
                            + " todavía no está implementado.");
        }
    }

    /**
     * Comprueba que un proceso no esté ejecutándose
     * simultáneamente en más de una CPU.
     */
    private void verificarProcesosEnCPU() {

        Set<Integer> procesosEjecutando = new HashSet<>();

        for (ContextoNucleo nucleo : nucleos) {

            int cpuId = nucleo.getCpu().getId();

            BCP proceso = gestorProcesos.getProcesoActual(cpuId);

            if (proceso == null) {
                continue;
            }

            int pid = proceso.getPid();

            if (!procesosEjecutando.add(pid)) {

                throw new IllegalStateException(
                        "Error de planificación: el proceso PID "
                                + pid
                                + " está asignado simultáneamente "
                                + "a más de una CPU.");
            }
        }
    }

    /**
     * Ejecuta un paso del procesador utilizando FCFS.
     *
     * Conserva la lógica de ejecución de instrucciones,
     * interrupciones, cambios de contexto y finalización.
     */
    /**
     * Ejecuta un tick de una CPU específica.
     *
     * @param nucleo CPU que ejecutará el tick.
     * @return true si consumió un segundo de CPU,
     *         false si permaneció ociosa.
     */
    private boolean ejecutarPasoCPU(ContextoNucleo nucleo) {

        // Obtener los componentes de esta CPU.
        CPU procesador = nucleo.getCpu();

        int cpuId = procesador.getId();

        Despachador plan = nucleo.getDespachador();

        /*
         * 1. DESPACHO
         *
         * Si la CPU está libre, buscar un proceso preparado.
         */
        if (gestorProcesos.getProcesoActual(cpuId) == null) {

            if (!fcfs.hayProcesosPreparados(cpuId)) {

                return false;
            }

            plan.despacharSiguiente();

            nucleo.reiniciarInstruccion();
        }

        /*
         * 2. OBTENER PROCESO ACTUAL
         */
        BCP proceso = gestorProcesos.getProcesoActual(cpuId);

        if (proceso == null) {
            return false;
        }

        /*
         * 3. VERIFICAR EL PROGRAM COUNTER
         */
        if (procesador.getPC() > proceso.getFinPrograma()) {

            plan.guardarContextoActual();

            gestorProcesos.finalizarProcesoActual(cpuId);

            nucleo.reiniciarInstruccion();

            intentarReactivarSuspendidos();

            return false;
        }

        if (procesador.getPC() < proceso.getInicioPrograma()) {

            finalizarPorError(
                    nucleo,
                    proceso,
                    new IllegalStateException(
                            "El PC está fuera del rango del proceso."));

            return false;
        }

        /*
         * 4. FETCH
         *
         * Obtener la siguiente instrucción del programa.
         */
        if (nucleo.getInstruccionActual() == null) {

            Instruccion instruccion = memoria.leer(procesador.getPC());

            if (instruccion == null) {

                finalizarPorError(
                        nucleo,
                        proceso,
                        new IllegalStateException(
                                "No existe una instrucción en la dirección "
                                        + procesador.getPC()));

                return false;
            }

            // Guardar la instrucción en el núcleo correspondiente.
            nucleo.setInstruccionActual(instruccion);

            // Cada instrucción consume al menos un tick.
            nucleo.setTicksRestantes(
                    Math.max(1, instruccion.getPeso()));

            // Actualizar registro IR de esta CPU.
            procesador.setIR(instruccion.getTextoOriginal());
        }

        /*
         * 5. CONSUMIR UN SEGUNDO DE CPU
         */
        gestorProcesos.incrementarTiempoCPU(cpuId);

        nucleo.setTicksRestantes(
                nucleo.getTicksRestantes() - 1);

        /*
         * Si todavía quedan ticks, guardar el contexto.
         */
        if (nucleo.getTicksRestantes() > 0) {

            proceso.guardarContexto(procesador);

            return true;
        }

        /*
         * 6. EXECUTE
         *
         * Ejecutar el efecto de la instrucción.
         */
        boolean pcModificado;

        try {

            pcModificado = nucleo.getExecutor()
                    .ejecutar(nucleo.getInstruccionActual());

        } catch (IllegalArgumentException
                | IllegalStateException
                | ArithmeticException error) {

            /*
             * No finalizar accidentalmente otro proceso
             * si ocurrió un cambio de contexto.
             */
            if (gestorProcesos.getProcesoActual(cpuId) == proceso) {

                finalizarPorError(nucleo, proceso, error);

            } else {

                erroresEjecucion.add(
                        "Error posterior a interrupción en CPU "
                                + cpuId
                                + ": "
                                + error.getMessage());
            }

            return true;
        }

        /*
         * 7. REINICIAR CONTROL DE INSTRUCCIÓN
         */
        nucleo.reiniciarInstruccion();

        /*
         * 8. COMPROBAR CAMBIO DE PROCESO
         *
         * Una interrupción pudo finalizar o bloquear
         * el proceso que estaba ejecutándose.
         */
        if (gestorProcesos.getProcesoActual(cpuId) != proceso) {

            intentarReactivarSuspendidos();

            return true;
        }

        /*
         * 9. ACTUALIZAR PROGRAM COUNTER
         *
         * Los saltos pueden modificar directamente el PC.
         */
        if (!pcModificado) {

            procesador.incrementarPC();
        }

        /*
         * 10. GUARDAR CONTEXTO
         */
        proceso.guardarContexto(procesador);

        /*
         * 11. VERIFICAR FINALIZACIÓN
         */
        if (procesador.getPC() > proceso.getFinPrograma()) {

            gestorProcesos.finalizarProcesoActual(cpuId);

            intentarReactivarSuspendidos();
        }

        // Esta CPU consumió un segundo simulado.
        return true;
    }

    /*
     * Ejecutar el efecto de la instrucción una vez
     * consumidos los ticks correspondientes.
     */
    /*
     * Si todos los procesos tienen una llegada posterior a cero, todavía no habrá
     * ninguno en READY.
     */
    public boolean puedeAvanzar() {

        for (ContextoNucleo nucleo : nucleos) {

            int cpuId = nucleo.getCpu().getId();

            if (gestorProcesos.getProcesoActual(cpuId) != null) {
                return true;
            }
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

    public List<String> getErroresEjecucion() {
        return new ArrayList<>(erroresEjecucion);
    }

    private void finalizarPorError(
            ContextoNucleo nucleo,
            BCP proceso,
            RuntimeException error) {

        Despachador plan = nucleo.getDespachador();

        int cpuId = nucleo.getCpu().getId();

        // Guardar contexto de la CPU correspondiente.
        plan.guardarContextoActual();

        // Registrar motivo.
        proceso.setMotivoError(error.getMessage());

        erroresEjecucion.add(
                "ERROR DE EJECUCIÓN"
                        + " | CPU " + cpuId
                        + " | PID " + proceso.getPid()
                        + " | PC=" + proceso.getPC()
                        + " | IR=" + proceso.getIR()
                        + " | " + error.getMessage());

        // Finalizar solamente el proceso de esta CPU.
        gestorProcesos.finalizarProcesoActual(cpuId);

        // Limpiar la instrucción de esta CPU.
        nucleo.reiniciarInstruccion();

        intentarReactivarSuspendidos();
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

    public void reiniciar(boolean conservarArchivos) {

        tiempoSimulado = 0;

        erroresEjecucion.clear();

        gestorMemoriaVirtual.reiniciar();

        gestorProcesos.reiniciar();

        memoria.reiniciar();

        almacenamiento.reiniciar(!conservarArchivos);

        // Reiniciar cada CPU independientemente.
        for (ContextoNucleo nucleo : nucleos) {

            nucleo.reiniciar();
        }
    }

    public TipoAlgoritmo getAlgoritmoSeleccionado() {
        return algoritmoSeleccionado;
    }

    public void configurarAlgoritmo(TipoAlgoritmo algoritmo) {

        if (algoritmo == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un algoritmo de planificación.");
        }

        if (tiempoSimulado > 0) {
            throw new IllegalStateException(
                    "No se puede cambiar el algoritmo "
                            + "después de iniciar la simulación.");
        }

        for (BCP proceso : gestorProcesos.getListaTrabajos()) {

            if (proceso.getHoraInicio() != null) {
                throw new IllegalStateException(
                        "No se puede cambiar el algoritmo "
                                + "porque ya inició un proceso.");
            }
        }

        this.algoritmoSeleccionado = algoritmo;
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

    public void configurarCantidadCPU(int cantidad) {

        if (cantidad < 1 || cantidad > 5) {

            throw new IllegalArgumentException(
                    "Solo se permiten entre 1 y 5 CPU.");
        }

        /*
         * Impedir modificaciones después
         * de iniciar la simulación.
         */
        if (tiempoSimulado != 0
                || gestorProcesos.getListaTrabajos().stream()
                        .anyMatch(p -> p.getHoraInicio() != null)) {

            throw new IllegalStateException(
                    "Configure las CPU antes de iniciar "
                            + "la simulación.");
        }

        /*
         * Eliminar las asignaciones anteriores.
         *
         * Esto solamente se permite porque todavía
         * no ha comenzado la ejecución.
         */
        for (BCP proceso : gestorProcesos.getListaTrabajos()) {

            if (proceso.getEstado() != EstadoProceso.FINALIZADO) {

                proceso.setCpuAsignada(-1);
            }
        }

        /*
         * Recrear los núcleos y el asignador
         * con la nueva cantidad de CPU.
         */
        inicializarNucleos(cantidad);

        /*
         * Distribuir nuevamente los procesos
         * utilizando las reglas del asignador.
         */
        for (BCP proceso : gestorProcesos.getListaTrabajos()) {

            asignarSiHayCupo(proceso);
        }
    }

    /**
     * Asigna un proceso a una CPU siempre que
     * exista capacidad disponible.
     *
     * Si todas las CPU alcanzaron el máximo de
     * cinco procesos, el proceso permanecerá
     * temporalmente sin asignación.
     *
     * El usuario deberá configurar suficientes
     * CPU antes de iniciar la ejecución.
     *
     * @param proceso proceso que debe ser asignado
     */
    private void asignarSiHayCupo(BCP proceso) {

        /*
         * No volver a asignar procesos que
         * ya tienen CPU o finalizaron.
         */
        if (proceso.getEstado() == EstadoProceso.FINALIZADO
                || proceso.getCpuAsignada() != -1) {

            return;
        }

        /*
         * Comprobar si existe al menos
         * una CPU con espacio disponible.
         */
        for (int cpuId = 1; cpuId <= nucleos.size(); cpuId++) {

            if (asignadorProcesosCPU
                    .getCantidadProcesos(cpuId) < 5) {

                /*
                 * El asignador decide cuál CPU
                 * recibirá el proceso.
                 */
                asignadorProcesosCPU
                        .asignarProceso(proceso);

                return;
            }
        }

        /*
         * No existe espacio disponible.
         *
         * El proceso permanece sin CPU hasta que
         * el usuario configure más procesadores.
         */
    }

    private void verificarAsignacionesCompletas() {

        for (BCP proceso : gestorProcesos.getListaTrabajos()) {

            if (proceso.getEstado() != EstadoProceso.FINALIZADO
                    && proceso.getCpuAsignada() == -1) {

                throw new IllegalStateException(
                        "El proceso PID "
                                + proceso.getPid()
                                + " no tiene CPU asignada. "
                                + "Configure más CPU antes de ejecutar.");
            }
        }
    }

    private int obtenerTiempoCpuConsumido() {

        int total = 0;

        for (BCP proceso : gestorProcesos.getListaTrabajos()) {
            total += proceso.getTiempoCPU();
        }

        return total;
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

    public int getCantidadCPU() {
        return nucleos.size();
    }

    public List<ContextoNucleo> getNucleos() {
        return Collections.unmodifiableList(nucleos);
    }

    public CPU getCPU(int numero) {
        return obtenerNucleo(numero).getCpu();
    }

    public BCP getProcesoActual(int numero) {
        return gestorProcesos.getProcesoActual(
                obtenerNucleo(numero).getCpu().getId());
    }

    private ContextoNucleo obtenerNucleo(int numero) {

        if (numero < 1 || numero > nucleos.size()) {
            throw new IllegalArgumentException(
                    "CPU inexistente: " + numero);
        }

        return nucleos.get(numero - 1);
    }
}
