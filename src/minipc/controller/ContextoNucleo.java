package minipc.controller;

import java.util.HashMap;
import java.util.Map;
import minipc.model.CPU;
import minipc.model.Instruccion;
import minipc.services.Despachador;
import minipc.services.Executor;
import minipc.services.GestorInterrupciones;

// se encarga de crear y mantener el estado de los componentes de un núcleo
public final class ContextoNucleo {

    private final CPU cpu;

    private final Despachador despachador;

    private final GestorInterrupciones interrupciones;

    private final Executor executor;

    private Instruccion instruccionActual;

    private int ticksRestantes;

    private final Map<Integer, InstruccionPausada> instruccionesPausadas = new HashMap<>();

    private static final class InstruccionPausada {

        private final Instruccion instruccion;

        private final int ticksPendientes;

        private InstruccionPausada(
                Instruccion instruccion,
                int ticksPendientes) {

            this.instruccion = instruccion;

            this.ticksPendientes = ticksPendientes;
        }
    }

    public ContextoNucleo(
            CPU cpu,
            Despachador despachador,
            GestorInterrupciones interrupciones,
            Executor executor) {

        this.cpu = cpu;
        this.despachador = despachador;
        this.interrupciones = interrupciones;
        this.executor = executor;
    }

    public CPU getCpu() {
        return cpu;
    }

    public Despachador getDespachador() {
        return despachador;
    }

    public GestorInterrupciones getInterrupciones() {
        return interrupciones;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Instruccion getInstruccionActual() {
        return instruccionActual;
    }

    public void setInstruccionActual(
            Instruccion instruccion) {

        instruccionActual = instruccion;
    }

    public int getTicksRestantes() {
        return ticksRestantes;
    }

    public void setTicksRestantes(int ticks) {
        ticksRestantes = ticks;
    }

    /**
     * Limpia la instrucción activa.
     */
    public void reiniciarInstruccion() {

        instruccionActual = null;

        ticksRestantes = 0;
    }

    // guarda la intruccion interrumpida de un proceso que fue expropiado
    public void guardarInstruccionInterrumpida(int pid) {

        if (instruccionActual != null
                && ticksRestantes > 0) {

            instruccionesPausadas.put(
                    pid,
                    new InstruccionPausada(
                            instruccionActual,
                            ticksRestantes));
        }

        reiniciarInstruccion();
    }

    // restaura la instruccion interrumpida de un proceso que fue expropiado
    public void restaurarInstruccionInterrumpida(int pid) {

        InstruccionPausada pausada = instruccionesPausadas.remove(pid);

        if (pausada == null) {

            reiniciarInstruccion();

        } else {

            instruccionActual = pausada.instruccion;

            ticksRestantes = pausada.ticksPendientes;
        }
    }

    // descarta la instruccion interrumpida de un proceso que fue expropiado
    public void descartarInstruccionInterrumpida(int pid) {

        instruccionesPausadas.remove(pid);
    }

    public void reiniciar() {

        cpu.reiniciarCPU();

        cpu.reiniciarEstadisticas();

        interrupciones.reiniciar();

        reiniciarInstruccion();

        instruccionesPausadas.clear();
    }
}
