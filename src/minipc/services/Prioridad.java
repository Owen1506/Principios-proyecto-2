package minipc.services;

import minipc.model.AlgoritmoPlanificacion;
import minipc.model.BCP;
import minipc.model.EstadoProceso;

/**
 * Planificador por prioridad estática.
 *
 * Fundamentación:
 *
 * - Silberschatz, "Operating System Concepts", 10.ª edición, cap. 5,
 *   §5.3.4: la CPU se asigna al proceso de mayor prioridad. Las prioridades
 *   iguales se atienden por FCFS.
 *
 * - Stallings, "Operating Systems: Internals and Design Principles",
 *   9.ª edición, cap. 9: la planificación por prioridad puede ser expulsiva
 *   o no expulsiva. En este simulador se usa un número mayor como mayor
 *   prioridad y la expulsión se controla con la variable {@code expropia}.
 *
 * No se aplica envejecimiento (aging): la prioridad de cada proceso se
 * mantiene fija durante toda la simulación.
 */
public class Prioridad implements AlgoritmoPlanificacion {

    private final GestorProcesos gestorProcesos;

    /*
     * ¿Qué hace? Guarda si este planificador puede quitar la CPU a un proceso
     * cuando aparece otro con más prioridad.
     * ¿Quién lo llama? debeExpropiar() para decidir si interrumpe el ciclo
     * actual; setExpropia() permite cambiarlo sin recompilar.
     */
    private boolean expropia = false;

    public Prioridad(GestorProcesos gestorProcesos) {
        this(gestorProcesos, true);
    }

    public Prioridad(GestorProcesos gestorProcesos, boolean expropia) {

        if (gestorProcesos == null) {
            throw new IllegalArgumentException(
                    "El gestor de procesos no puede ser null.");
        }

        this.gestorProcesos = gestorProcesos;
        this.expropia = expropia;
    }

    /*
     * ¿Qué hace? Activa o desactiva la expropiación. Si queda en false, el
     * proceso actual termina su ciclo sin ser interrumpido.
     * ¿Quién lo llama? Las pruebas o quien quiera comparar las dos variantes.
     */
    public void setExpropia(boolean expropia) {
        this.expropia = expropia;
    }

    /*
     * ¿Qué hace? Informa si la expropiación está activa.
     * ¿Quién lo llama? Las pruebas y la interfaz si necesita mostrar el modo.
     */
    public boolean isExpropia() {
        return expropia;
    }

    /*
     * ¿Qué hace? Busca, entre los procesos preparados de esta CPU, el que
     * tenga la prioridad más alta. Si empatan, gana el que llegó primero y
     * luego el PID menor.
     * ¿Quién lo llama? El Despachador cuando la CPU queda libre.
     */
    @Override
    public BCP seleccionarSiguiente(int cpuId) {

        BCP elegido = null;

        for (BCP candidato : gestorProcesos.getColaPreparados()) {

            if (candidato.getCpuAsignada() != cpuId
                    || candidato.getEstado() != EstadoProceso.PREPARADO) {

                continue;
            }

            if (elegido == null
                    || candidato.getPrioridad() > elegido.getPrioridad()
                    || (candidato.getPrioridad() == elegido.getPrioridad()
                            && esMejorDesempate(candidato, elegido))) {

                elegido = candidato;
            }
        }

        return elegido;
    }

    /*
     * ¿Qué hace? Decide si el proceso que está usando la CPU debe devolverla
     * porque hay otro preparado con más prioridad. Si expropia es false,
     * nunca interrumpe y deja que el proceso actual siga su ciclo.
     * ¿Quién lo llama? Controlador.ejecutarPasoCPU() antes de cada tick.
     */
    @Override
    public boolean debeExpropiar(int cpuId, BCP actual) {

        if (!expropia || actual == null) {
            return false;
        }

        BCP siguiente = seleccionarSiguiente(cpuId);

        return siguiente != null
                && siguiente.getPrioridad() > actual.getPrioridad();
    }

    /*
     * ¿Qué hace? Rompe el empate entre dos procesos con la misma prioridad:
     * primero el que llegó antes y, si también empatan, el PID menor.
     * ¿Quién lo llama? seleccionarSiguiente().
     */
    private boolean esMejorDesempate(BCP candidato, BCP actual) {

        if (candidato.getTiempoLlegada() != actual.getTiempoLlegada()) {

            return candidato.getTiempoLlegada()
                    < actual.getTiempoLlegada();
        }

        return candidato.getPid() < actual.getPid();
    }
}
