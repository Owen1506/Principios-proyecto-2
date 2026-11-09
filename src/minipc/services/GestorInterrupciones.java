package minipc.services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import minipc.model.BCP;
import minipc.model.CPU;
import minipc.model.Instruccion;

/**
 * Administra las interrupciones generadas durante la ejecución
 * de los procesos de la Mini PC.
 *
 * Cada instancia pertenece a una CPU específica.
 *
 * Interrupciones soportadas:
 *
 * INT 09H -> entrada por teclado.
 * INT 10H -> salida en pantalla.
 * INT 20H -> finalización del proceso.
 * INT 21H -> manejo de archivos.
 *
 * Las interrupciones solo modifican el proceso asociado
 * a la CPU correspondiente.
 */
public class GestorInterrupciones {

    private CPU cpu;
    private GestorProcesos gestorProcesos;
    private Despachador despachador;
    private SistemaArchivos sistemaArchivos;

    private Queue<BCP> esperandoTeclado;

    private String ultimaSalida;

    private final List<String> salidas = new ArrayList<>();

    /**
     * Inicializa el gestor de interrupciones de una CPU.
     *
     * @param cpu             CPU asociada al gestor
     * @param gestorProcesos  administrador global de procesos
     * @param despachador     despachador de esta CPU
     * @param sistemaArchivos administrador de archivos
     */
    public GestorInterrupciones(
            CPU cpu,
            GestorProcesos gestorProcesos,
            Despachador despachador,
            SistemaArchivos sistemaArchivos) {

        if (cpu == null) {
            throw new IllegalArgumentException(
                    "La CPU no puede ser null.");
        }

        if (gestorProcesos == null) {
            throw new IllegalArgumentException(
                    "El gestor de procesos no puede ser null.");
        }

        if (despachador == null) {
            throw new IllegalArgumentException(
                    "El despachador no puede ser null.");
        }

        if (sistemaArchivos == null) {
            throw new IllegalArgumentException(
                    "El sistema de archivos no puede ser null.");
        }

        this.cpu = cpu;
        this.gestorProcesos = gestorProcesos;
        this.despachador = despachador;
        this.sistemaArchivos = sistemaArchivos;

        this.esperandoTeclado = new LinkedList<>();
        this.ultimaSalida = "";
    }

    /**
     * Atiende una instrucción de interrupción.
     *
     * @param instruccion instrucción INT
     * @return true si la interrupción modificó el flujo
     *         normal del PC
     */
    public boolean atender(Instruccion instruccion) {

        String servicio = instruccion.getServicio();

        switch (servicio) {

            case "09H":
                atenderINT09();
                return true;

            case "10H":
                atenderINT10();
                return false;

            case "20H":
                atenderINT20();
                return true;

            case "21H":
                atenderINT21();
                return false;

            default:
                throw new IllegalArgumentException(
                        "Interrupción no soportada: " + servicio);
        }
    }

    /**
     * INT 09H.
     *
     * Bloquea al proceso de esta CPU mientras espera
     * una entrada por teclado.
     *
     * El siguiente proceso preparado será despachado
     * por el controlador durante el próximo tick.
     */
    private void atenderINT09() {

        // Obtener exclusivamente el proceso de esta CPU.
        BCP proceso = gestorProcesos.getProcesoActual(
                cpu.getId());

        if (proceso == null) {
            throw new IllegalStateException(
                    "No existe un proceso en ejecución en CPU "
                            + cpu.getId());
        }

        /*
         * Avanzar el PC para evitar que el proceso vuelva
         * a ejecutar INT 09H cuando sea desbloqueado.
         */
        cpu.incrementarPC();

        /*
         * Conservar los registros y el PC del proceso
         * antes de retirarlo de la CPU.
         */
        despachador.guardarContextoActual();

        // Registrar el proceso que espera entrada.
        esperandoTeclado.add(proceso);

        // Bloquear únicamente el proceso de esta CPU.
        gestorProcesos.bloquearProcesoActual(
                cpu.getId());

        /*
         * No despachar aquí.
         *
         * El controlador administra los cambios de proceso
         * y ejecutará el siguiente despacho en otro tick.
         */
    }

    /**
     * Recibe una entrada y desbloquea al primer proceso
     * que estaba esperando teclado en esta CPU.
     *
     * @param entrada valor introducido por el usuario
     * @return BCP del proceso desbloqueado
     */
    public BCP recibirEntradaTeclado(String entrada) {

        if (esperandoTeclado.isEmpty()) {
            throw new IllegalStateException(
                    "No existen procesos esperando entrada de teclado.");
        }

        int valor;

        try {
            valor = Integer.parseInt(entrada.trim());

        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "La entrada debe ser un número entero.");
        }

        if (valor < 0 || valor > 255) {
            throw new IllegalArgumentException(
                    "La entrada debe estar entre 0 y 255.");
        }

        // Recuperar el proceso que esperaba entrada.
        BCP proceso = esperandoTeclado.poll();

        // Guardar el dato introducido.
        proceso.setDX(String.valueOf(valor));

        // Devolver el proceso a preparados.
        gestorProcesos.desbloquearProceso(proceso);

        /*
         * No ejecutar un despacho inmediato.
         *
         * El proceso mantiene su CPU asignada y será
         * seleccionado por su planificador correspondiente.
         */
        return proceso;
    }

    /**
     * INT 10H.
     *
     * Registra el contenido del registro DX como salida
     * producida por esta CPU.
     */
    private void atenderINT10() {

        ultimaSalida = cpu.getRegistros().getDX();

        salidas.add(ultimaSalida);
    }

    /**
     * INT 20H.
     *
     * Finaliza únicamente el proceso que está utilizando
     * esta CPU.
     *
     * La CPU queda disponible para que el controlador
     * asigne otro proceso durante un tick posterior.
     */
    private void atenderINT20() {

        BCP proceso = gestorProcesos.getProcesoActual(
                cpu.getId());

        if (proceso == null) {
            throw new IllegalStateException(
                    "No existe un proceso en ejecución en CPU "
                            + cpu.getId());
        }

        // Conservar el contexto final.
        despachador.guardarContextoActual();

        // Finalizar el proceso asociado a esta CPU.
        gestorProcesos.finalizarProcesoActual(
                cpu.getId());

        /*
         * No llamar a despacharSiguiente().
         *
         * El controlador hará el cambio de proceso
         * cuando corresponda.
         */
    }

    /**
     * INT 21H.
     *
     * Ejecuta el servicio de archivos utilizando los
     * registros y el BCP de la CPU correspondiente.
     */
    private void atenderINT21() {

        // Obtener el proceso de esta CPU.
        BCP proceso = gestorProcesos.getProcesoActual(
                cpu.getId());

        if (proceso == null) {
            throw new IllegalStateException(
                    "No existe un proceso en ejecución en CPU "
                            + cpu.getId());
        }

        // Ejecutar el servicio sobre el proceso correcto.
        sistemaArchivos.ejecutarServicio(cpu, proceso);
    }

    /**
     * Retorna la última salida generada por esta CPU.
     *
     * @return último mensaje
     */
    public String getUltimaSalida() {
        return ultimaSalida;
    }

    /**
     * Reinicia los registros de interrupciones.
     */
    public void reiniciar() {

        salidas.clear();

        esperandoTeclado.clear();

        ultimaSalida = "";
    }

    /**
     * Comprueba si existen procesos esperando teclado
     * en esta CPU.
     *
     * @return true si existe al menos uno
     */
    public boolean hayProcesoEsperandoTeclado() {
        return !esperandoTeclado.isEmpty();
    }

    /**
     * Obtiene las salidas producidas por esta CPU.
     *
     * @return copia de los mensajes registrados
     */
    public List<String> getSalidas() {
        return new ArrayList<>(salidas);
    }
}
