package minipc.services;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import minipc.model.AlgoritmoPlanificacion;
import minipc.model.BCP;
import minipc.model.EstadoProceso;

/** Sortea boletos entre los procesos preparados de cada CPU. */
public class LotteryScheduling implements AlgoritmoPlanificacion {
    private final GestorProcesos gestor;
    private final int quantum;
    private final Map<Integer, Integer> ticksCPU = new HashMap<>();

    public LotteryScheduling(GestorProcesos gestor, int quantum) {
        if (gestor == null || quantum < 1) {
            throw new IllegalArgumentException("Lottery requiere gestor y quantum positivo.");
        }
        this.gestor = gestor;
        this.quantum = quantum;
    }

    private boolean participa(BCP proceso, int cpuId) {
        return proceso.getCpuAsignada() == cpuId
                && proceso.getEstado() == EstadoProceso.PREPARADO;
    }

    @Override
    public boolean hayProcesosPreparados(int cpuId) {
        // Consultar disponibilidad no consume números aleatorios.
        return gestor.getColaPreparados().stream().anyMatch(p -> participa(p, cpuId));
    }

    @Override
    public BCP seleccionarSiguiente(int cpuId) {
        // Cada valor es el límite superior inclusivo del rango del proceso.
        Map<BCP, Long> rangos = new LinkedHashMap<>();
        long total = 0;
        for (BCP proceso : gestor.getColaPreparados()) {
            if (!participa(proceso, cpuId)) continue;
            if (proceso.getPrioridad() < 1) {
                throw new IllegalStateException("El PID " + proceso.getPid()
                        + " debe tener al menos un ticket.");
            }
            total += proceso.getPrioridad();
            rangos.put(proceso, total);
        }
        if (rangos.isEmpty()) return null;
        long ganador = ThreadLocalRandom.current().nextLong(total) + 1;
        for (Map.Entry<BCP, Long> rango : rangos.entrySet()) {
            if (ganador <= rango.getValue()) return rango.getKey();
        }
        throw new IllegalStateException("No se encontró el boleto ganador.");
    }

    @Override
    public boolean debeExpropiar(int cpuId, BCP actual) {
        // El controlador devuelve al actual a PREPARADO antes del sorteo.
        return actual != null && ticksCPU.getOrDefault(cpuId, 0) >= quantum;
    }

    @Override
    public void alDespachar(int cpuId, BCP proceso) {
        ticksCPU.put(cpuId, 0);
    }

    @Override
    public void alConsumirTick(int cpuId, BCP proceso) {
        ticksCPU.merge(cpuId, 1, Integer::sum);
    }
}
