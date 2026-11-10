package minipc.services;

import java.util.HashMap;
import java.util.Map;
import minipc.model.AlgoritmoPlanificacion;
import minipc.model.BCP;
import minipc.model.EstadoProceso;

public class RoundRobin implements AlgoritmoPlanificacion {

    private final GestorProcesos gestorProcesos;
    private final int quantum;

    // Ticks consumidos durante el turno actual de cada CPU.
    private final Map<Integer, Integer> ticksPorCPU = new HashMap<>();

    public RoundRobin(GestorProcesos gestorProcesos, int quantum) {

        if (gestorProcesos == null) {
            throw new IllegalArgumentException(
                    "El gestor de procesos no puede ser null.");
        }

        if (quantum <= 0) {
            throw new IllegalArgumentException(
                    "El quantum debe ser mayor que cero.");
        }

        this.gestorProcesos = gestorProcesos;
        this.quantum = quantum;
    }

    @Override
    public BCP seleccionarSiguiente(int cpuId) {

        // Seleccionar el primer proceso preparado
        // que pertenezca a esta CPU.
        for (BCP candidato : gestorProcesos.getColaPreparados()) {

            if (candidato.getCpuAsignada() == cpuId
                    && candidato.getEstado() == EstadoProceso.PREPARADO) {

                return candidato;
            }
        }

        return null;
    }

    @Override
    public boolean debeExpropiar(int cpuId, BCP actual) {

        if (actual == null) {
            return false;
        }

        int ticksConsumidos = ticksPorCPU.getOrDefault(cpuId, 0);

        // Todavía no agotó el quantum.
        if (ticksConsumidos < quantum) {
            return false;
        }

        // Solo cambiar de proceso si hay otro preparado.
        return seleccionarSiguiente(cpuId) != null;
    }

    @Override
    public void alDespachar(int cpuId, BCP proceso) {

        // Comienza un nuevo turno.
        ticksPorCPU.put(cpuId, 0);
    }

    @Override
    public void alConsumirTick(int cpuId, BCP proceso) {

        // Registrar un segundo consumido.
        ticksPorCPU.merge(cpuId, 1, Integer::sum);
    }
}
