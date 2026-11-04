package minipc.services;

import minipc.model.BCP;
import minipc.model.CPU;

/**
 * Se encarga de asignar la CPU a los procesos seleccionados
 * por el planificador.
 *
 * El despachador restaura el contexto del proceso que va a ejecutarse
 * y permite guardar el contexto del proceso que actualmente utiliza
 * la CPU.
 */
public class Despachador {

    private CPU cpu;
    private GestorProcesos gestorProcesos;
    private planificadordeProcesos planificador;

    /**
     * Crea un nuevo despachador.
     *
     * @param cpu CPU de la Mini PC
     * @param gestorProcesos gestor encargado de administrar los procesos
     * @param planificador selector del siguiente proceso preparado
     */
    public Despachador(CPU cpu, GestorProcesos gestorProcesos, planificadordeProcesos planificador) {

        if (cpu == null) {
            throw new IllegalArgumentException("La CPU no puede ser null.");
        }

        if (gestorProcesos == null) {
            throw new IllegalArgumentException("El gestor de procesos no puede ser null.");
        }

        if (planificador == null) {
            throw new IllegalArgumentException("El planificador no puede ser null.");
        }

        this.cpu = cpu;
        this.gestorProcesos = gestorProcesos;
        this.planificador = planificador;
    }

    /**
     * Guarda el contexto del proceso que actualmente utiliza la CPU.
     *
     * Este método debe ejecutarse antes de retirar un proceso de la CPU,
     * por ejemplo cuando queda bloqueado.
     */
    public void guardarContextoActual() {

        BCP actual = gestorProcesos.getProcesoActual();

        if (actual != null) {
            actual.guardarContexto(cpu);
        }
    }

    /**
     * Selecciona mediante el planificador el siguiente proceso preparado
     * y le asigna la CPU.
     *
     * @return proceso que recibió la CPU o null si no existen
     *         procesos preparados
     */
    public BCP despacharSiguiente() {

        if (gestorProcesos.getProcesoActual() != null) {
            throw new IllegalStateException("La CPU ya está siendo utilizada por un proceso.");
        }

        BCP siguiente = planificador.seleccionarSiguiente();

        if (siguiente == null) {
            cpu.reiniciarCPU();
            return null;
        }

        siguiente.restaurarContexto(cpu);
        gestorProcesos.iniciarEjecucion(siguiente);

        return siguiente;
    }
}
