package minipc.services;

import minipc.model.AlgoritmoPlanificacion;
import minipc.model.BCP;
import minipc.model.EstadoProceso;

public class FCFS implements AlgoritmoPlanificacion {

    private GestorProcesos gestorProcesos;

    /**
     * Crea el planificador FCFS.
     *
     * gestorProcesos gestor que contiene la cola de preparados
     */
    public FCFS(GestorProcesos gestorProcesos) {

        if (gestorProcesos == null) {
            throw new IllegalArgumentException("El gestor de procesos no puede ser null.");
        }

        this.gestorProcesos = gestorProcesos;
    }

    // selecciona el primer proceso preparado que pertenece a la CPU 1
    public BCP seleccionarSiguiente() {

        return seleccionarSiguiente(1);
    }

    // selecciona el primer proceso preparado que pertenece a la CPU indicada
    @Override
    public BCP seleccionarSiguiente(int cpuId) {

        for (BCP proceso : gestorProcesos.getColaPreparados()) {

            if (proceso.getEstado() == EstadoProceso.PREPARADO
                    && proceso.getCpuAsignada() == cpuId) {

                return proceso;
            }
        }

        return null;
    }

    // Comprueba si existen procesos preparados asignados a la CPU 1
    public boolean hayProcesosPreparados() {

        return hayProcesosPreparados(1);
    }

    // Comprueba si existen procesos preparados asignados a la CPU indicada
    public boolean hayProcesosPreparados(int cpuId) {

        return seleccionarSiguiente(cpuId) != null;
    }
}
