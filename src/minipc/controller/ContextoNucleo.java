package minipc.controller;

import minipc.model.CPU;
import minipc.model.Instruccion;
import minipc.services.Despachador;
import minipc.services.Executor;
import minipc.services.GestorInterrupciones;

public final class ContextoNucleo {

    private final CPU cpu;

    private final Despachador despachador;

    private final GestorInterrupciones interrupciones;

    private final Executor executor;

    private Instruccion instruccionActual;

    private int ticksRestantes;

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

    public void setInstruccionActual(Instruccion instruccion) {
        instruccionActual = instruccion;
    }

    public int getTicksRestantes() {
        return ticksRestantes;
    }

    public void setTicksRestantes(int ticks) {
        ticksRestantes = ticks;
    }

    public void reiniciarInstruccion() {
        instruccionActual = null;
        ticksRestantes = 0;
    }

    public void reiniciar() {
        cpu.reiniciarCPU();
        cpu.reiniciarEstadisticas();
        interrupciones.reiniciar();
        reiniciarInstruccion();
    }
}