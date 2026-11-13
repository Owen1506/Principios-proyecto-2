package minipc.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import minipc.model.AlgoritmoPlanificacion;
import minipc.model.BCP;
import minipc.model.EstadoProceso;

/*
 * ¿Qué hace? Reparte cada CPU por turnos entre procesos admitidos y aumenta
 * las prioridades de los que esperan más rápido que las de los servidos.
 * ¿Quién lo llama? El controlador y los despachadores de la Mini PC.
 * Basado en Stallings, 9.ª edición, problema 9.12 y figura 9.19, p. 458.
 */
public class SelfishRoundRobin implements AlgoritmoPlanificacion {
    private final GestorProcesos gestor;
    private final int quantum;
    private final int tasaEspera;
    private final int tasaServicio;
    private final Set<Integer> servidos = new HashSet<>();
    private final Map<Integer, Integer> ticksCPU = new HashMap<>();
    private final Map<BCP, Integer> incrementos = new HashMap<>();

    public SelfishRoundRobin(GestorProcesos gestor, int quantum, int tasaEspera, int tasaServicio) {
        if (gestor == null || quantum < 1 || tasaServicio < 1 || tasaEspera <= tasaServicio) {
            throw new IllegalArgumentException("SRR requiere gestor, quantum positivo y 0 < b < a.");
        }
        this.gestor = gestor;
        this.quantum = quantum;
        this.tasaEspera = tasaEspera;
        this.tasaServicio = tasaServicio;
    }

    /* ¿Qué hace? Comprueba si puede competir por CPU. ¿Quién lo llama? La admisión y el cierre del tick. */
    private boolean disponible(BCP proceso) {
        return proceso.getEstado() == EstadoProceso.PREPARADO
                || proceso.getEstado() == EstadoProceso.EJECUCION;
    }

    /* ¿Qué hace? Incorpora a quienes alcanzaron al grupo de su CPU. ¿Quién lo llama? iniciarTick(). */
    private void admitir(int cpuId) {
        Integer umbral = null;
        BCP mejor = null;
        for (BCP p : gestor.getListaTrabajos()) {
            if (p.getCpuAsignada() != cpuId || !disponible(p)) continue;
            if (servidos.contains(p.getPid())) {
                umbral = umbral == null ? p.getPrioridad() : Math.min(umbral, p.getPrioridad());
            } else if (mejor == null || p.getPrioridad() > mejor.getPrioridad()
                    || (p.getPrioridad() == mejor.getPrioridad()
                    && (p.getTiempoLlegada() < mejor.getTiempoLlegada()
                    || (p.getTiempoLlegada() == mejor.getTiempoLlegada() && p.getPid() < mejor.getPid())))) {
                mejor = p;
            }
        }
        if (umbral == null && mejor != null) {
            servidos.add(mejor.getPid());
            umbral = mejor.getPrioridad();
        }
        if (umbral != null) {
            for (BCP p : gestor.getListaTrabajos()) {
                if (p.getCpuAsignada() == cpuId && disponible(p) && p.getPrioridad() >= umbral) {
                    servidos.add(p.getPid());
                }
            }
        }
    }

    /* ¿Qué hace? Prepara admisiones e incrementos una sola vez por segundo. ¿Quién lo llama? El controlador antes de las CPU. */
    @Override
    public void iniciarTick() {
        incrementos.clear();
        Set<Integer> cpus = new HashSet<>();
        for (BCP p : gestor.getListaTrabajos()) {
            if (disponible(p) && p.getCpuAsignada() > 0) cpus.add(p.getCpuAsignada());
        }
        for (int cpu : cpus) admitir(cpu);
        for (BCP p : gestor.getListaTrabajos()) {
            if (disponible(p)) incrementos.put(p, servidos.contains(p.getPid()) ? tasaServicio : tasaEspera);
        }
    }

    /* ¿Qué hace? Sube las prioridades registradas sin desbordar enteros. ¿Quién lo llama? El controlador al finalizar el segundo. */
    @Override
    public void finalizarTick() {
        incrementos.forEach((p, incremento) -> p.setPrioridad((int) Math.min(
                Integer.MAX_VALUE, (long) p.getPrioridad() + incremento)));
        incrementos.clear();
    }

    /* ¿Qué hace? Busca el siguiente servido de la cola. ¿Quién lo llama? El despachador y debeExpropiar(). */
    @Override
    public BCP seleccionarSiguiente(int cpuId) {
        for (BCP p : gestor.getColaPreparados()) {
            if (p.getCpuAsignada() == cpuId && p.getEstado() == EstadoProceso.PREPARADO
                    && servidos.contains(p.getPid())) return p;
        }
        return null;
    }

    /* ¿Qué hace? Cambia de turno al agotar el quantum si hay otro servido. ¿Quién lo llama? El controlador antes de ejecutar. */
    @Override
    public boolean debeExpropiar(int cpuId, BCP actual) {
        return actual != null && ticksCPU.getOrDefault(cpuId, 0) >= quantum
                && seleccionarSiguiente(cpuId) != null;
    }

    /* ¿Qué hace? Reinicia el turno. ¿Quién lo llama? El despachador. */
    @Override
    public void alDespachar(int cpuId, BCP proceso) { ticksCPU.put(cpuId, 0); }

    /* ¿Qué hace? Cuenta el tiempo del turno. ¿Quién lo llama? El controlador. */
    @Override
    public void alConsumirTick(int cpuId, BCP proceso) { ticksCPU.merge(cpuId, 1, Integer::sum); }
}
