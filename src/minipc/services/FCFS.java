package minipc.services;

import minipc.model.BCP;
import minipc.model.EstadoProceso;

/**
 * Implementa el algoritmo de planificación FCFS
 * (First Come, First Served).
 *
 * Los procesos son seleccionados en el mismo orden
 * en que ingresaron a la cola de preparados.
 */
public class FCFS {

    private GestorProcesos gestorProcesos;

    /**
     * Crea el planificador FCFS.
     *
     * @param gestorProcesos gestor que contiene la cola de preparados
     */
    public FCFS(GestorProcesos gestorProcesos) {

        if (gestorProcesos == null) {
            throw new IllegalArgumentException("El gestor de procesos no puede ser null.");
        }

        this.gestorProcesos = gestorProcesos;
    }

    /**
     * Selecciona el primer proceso de la cola de preparados.
     *
     * El proceso no se elimina todavía de la cola. Será
     * GestorProcesos quien lo retire cuando realmente pase
     * al estado EJECUCION.
     *
     * @return siguiente proceso que debe utilizar la CPU,
     *         o null si no existen procesos preparados
     */
    /**
     * Selecciona el siguiente proceso preparado
     * correspondiente a la CPU 1.
     *
     * Se conserva por compatibilidad con las
     * llamadas anteriores del simulador.
     *
     * @return proceso seleccionado o null
     */
    public BCP seleccionarSiguiente() {

        return seleccionarSiguiente(1);
    }

    /**
     * Selecciona el primer proceso preparado que
     * pertenece a la CPU indicada.
     *
     * Se respeta el orden de la cola de preparados,
     * pero solamente se consideran procesos cuya
     * afinidad corresponde a esta CPU.
     *
     * El proceso no se elimina todavía de la cola.
     *
     * @param cpuId identificador de la CPU
     * @return proceso seleccionado o null
     */
    public BCP seleccionarSiguiente(int cpuId) {

        for (BCP proceso : gestorProcesos.getColaPreparados()) {

            if (proceso.getEstado() == EstadoProceso.PREPARADO
                    && proceso.getCpuAsignada() == cpuId) {

                return proceso;
            }
        }

        return null;
    }


    /**
     * Comprueba si la CPU 1 tiene procesos
     * preparados para ejecutar.
     *
     * @return true si existen procesos preparados
     */
    public boolean hayProcesosPreparados() {

        return hayProcesosPreparados(1);
    }

    /**
     * Comprueba si existen procesos preparados
     * asignados a la CPU indicada.
     *
     * @param cpuId identificador de CPU
     * @return true si existen procesos preparados
     */
    public boolean hayProcesosPreparados(int cpuId) {

        return seleccionarSiguiente(cpuId) != null;
    }
}
