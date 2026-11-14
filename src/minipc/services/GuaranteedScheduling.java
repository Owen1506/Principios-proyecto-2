package minipc.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import minipc.model.AlgoritmoPlanificacion;
import minipc.model.BCP;
import minipc.model.EstadoProceso;

/* Reparte el servicio según la CPU recibida y la cuota justa. Lo crea Controlador al elegir la política. */
public class GuaranteedScheduling implements AlgoritmoPlanificacion {
    private static final double TOLERANCIA = 1e-12;
    private final GestorProcesos gestor;
    private final Map<Integer, Double> parteJusta = new HashMap<>();
    private final Map<Integer, Integer> servicio = new HashMap<>();
    private final Map<Integer, List<Integer>> participantes = new HashMap<>();
    private final Set<Integer> cpusOcupadas = new HashSet<>();

    public GuaranteedScheduling(GestorProcesos gestor) {
        if (gestor == null) throw new IllegalArgumentException("El gestor no puede ser null.");
        this.gestor = gestor;
    }

    /* Anota quién puede competir en cada CPU durante el siguiente tick. Lo llama Controlador antes de planificar. */
    @Override
    public void iniciarTick() {
        participantes.clear();
        cpusOcupadas.clear();
        for (BCP proceso : gestor.getListaTrabajos()) {
            EstadoProceso estado = proceso.getEstado();
            if (proceso.getCpuAsignada() > 0
                    && (estado == EstadoProceso.PREPARADO || estado == EstadoProceso.EJECUCION)) {
                participantes.computeIfAbsent(proceso.getCpuAsignada(), id -> new ArrayList<>())
                        .add(proceso.getPid());
            }
        }
    }

    /* Reparte la cuota justa solo cuando la CPU consumió el tick. Lo llama Controlador al cerrar el tick. */
    @Override
    public void finalizarTick() {
        for (int cpuId : cpusOcupadas) {
            List<Integer> grupo = participantes.get(cpuId);
            if (grupo == null || grupo.isEmpty()) continue;
            double cuota = 1.0 / grupo.size();
            for (int pid : grupo) parteJusta.merge(pid, cuota, Double::sum);
        }
        participantes.clear();
        cpusOcupadas.clear();
    }

    /* Devuelve el servicio contabilizado por el planificador. Lo usa Controlador.getServicioGarantizado. */
    public int getServicio(int pid) { return servicio.getOrDefault(pid, 0); }

    /* Devuelve la cuota justa acumulada mientras el proceso pudo competir. La usa Controlador.getParteJustaGarantizada. */
    public double getParteJusta(int pid) { return parteJusta.getOrDefault(pid, 0.0); }

    /* Devuelve el servicio dividido por la cuota; sin cuota comienza en cero. La usa Controlador.getProporcionGarantizada. */
    public double getProporcion(int pid) {
        double justa = getParteJusta(pid);
        return justa <= TOLERANCIA ? 0.0 : getServicio(pid) / justa;
    }

    /* Elige el preparado más atrasado y conserva el orden de cola en empates. Lo llama Controlador al planificar. */
    @Override
    public BCP seleccionarSiguiente(int cpuId) {
        BCP elegido = null;
        for (BCP p : gestor.getColaPreparados()) {
            if (p.getCpuAsignada() != cpuId || p.getEstado() != EstadoProceso.PREPARADO) continue;
            if (elegido == null || getProporcion(p.getPid())
                    < getProporcion(elegido.getPid()) - TOLERANCIA) elegido = p;
        }
        return elegido;
    }

    /* Interrumpe solo si otro preparado tiene una proporción menor. Lo llama Controlador antes de ejecutar. */
    @Override
    public boolean debeExpropiar(int cpuId, BCP actual) {
        if (actual == null) return false;
        BCP siguiente = seleccionarSiguiente(cpuId);
        return siguiente != null && getProporcion(siguiente.getPid())
                < getProporcion(actual.getPid()) - TOLERANCIA;
    }

    /* Registra el servicio real y marca la CPU que consumió el tick. Lo llama Controlador al ejecutar. */
    @Override
    public void alConsumirTick(int cpuId, BCP proceso) {
        servicio.merge(proceso.getPid(), 1, Integer::sum);
        cpusOcupadas.add(cpuId);
    }
}
