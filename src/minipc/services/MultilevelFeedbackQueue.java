package minipc.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import minipc.model.AlgoritmoPlanificacion;
import minipc.model.BCP;
import minipc.model.EstadoProceso;
import minipc.model.TipoAlgoritmo;

/* Organiza los procesos en tres niveles con quantum y política para Q2. Lo crea Controlador. */
public class MultilevelFeedbackQueue implements AlgoritmoPlanificacion {
    private final GestorProcesos gestor;
    private final TipoAlgoritmo algoritmoQ2;
    private final Map<Integer, Integer> niveles = new HashMap<>();
    private final Map<Integer, Integer> consumidos = new HashMap<>();
    private final Set<Integer> turnoAgotado = new HashSet<>();

    public MultilevelFeedbackQueue(GestorProcesos gestor, TipoAlgoritmo algoritmoQ2) {
        if (gestor == null || !esAlgoritmoValido(algoritmoQ2)) {
            throw new IllegalArgumentException("Q2 admite FCFS, SPN, SRT, HRRN o RR.");
        }
        this.gestor = gestor;
        this.algoritmoQ2 = algoritmoQ2;
    }

    /* Comprueba que Q2 admita FCFS, SPN, SRT, HRRN o RR. Lo llama el constructor y Controlador. */
    public static boolean esAlgoritmoValido(TipoAlgoritmo tipo) {
        return tipo == TipoAlgoritmo.FCFS || tipo == TipoAlgoritmo.SPN
                || tipo == TipoAlgoritmo.SRT || tipo == TipoAlgoritmo.HRRN
                || tipo == TipoAlgoritmo.RR;
    }

    /* Devuelve el nivel actual sin modificar el BCP. Lo usa Controlador y la interfaz. */
    public int getNivel(int pid) {
        return niveles.getOrDefault(pid, 0);
    }

    /* Baja de nivel a quien agotó su turno antes de planificar. Lo llama Controlador. */
    @Override
    public void iniciarTick() {
        turnoAgotado.clear();
        for (BCP p : gestor.getListaTrabajos()) {
            int pid = p.getPid();
            if (p.getEstado() == EstadoProceso.FINALIZADO) {
                niveles.remove(pid);
                consumidos.remove(pid);
                continue;
            }
            if (p.getEstado() != EstadoProceso.PREPARADO
                    && p.getEstado() != EstadoProceso.EJECUCION) continue;
            int nivel = getNivel(pid);
            if (nivel < 2 && consumidos.getOrDefault(pid, 0) >= (1 << nivel)) {
                niveles.put(pid, nivel + 1);
                consumidos.put(pid, 0);
                if (p.getEstado() == EstadoProceso.EJECUCION) turnoAgotado.add(pid);
            }
        }
    }

    /* Escoge la cola más alta y aplica la política de Q2. Lo llama Controlador al planificar. */
    @Override
    public BCP seleccionarSiguiente(int cpuId) {
        BCP elegido = null;
        for (BCP p : gestor.getColaPreparados()) {
            if (p.getCpuAsignada() != cpuId || p.getEstado() != EstadoProceso.PREPARADO) continue;
            if (elegido == null || getNivel(p.getPid()) < getNivel(elegido.getPid())
                    || (getNivel(p.getPid()) == 2 && getNivel(elegido.getPid()) == 2
                            && mejorEnQ2(p, elegido))) {
                elegido = p;
            }
        }
        return elegido;
    }

    /* Compara candidatos de Q2; FCFS y RR conservan el orden. Lo llama seleccionarSiguiente. */
    private boolean mejorEnQ2(BCP candidato, BCP actual) {
        if (algoritmoQ2 == TipoAlgoritmo.SPN) {
            return candidato.getRafaga() < actual.getRafaga();
        }
        if (algoritmoQ2 == TipoAlgoritmo.SRT) {
            return candidato.getTiempoRestante() < actual.getTiempoRestante();
        }
        if (algoritmoQ2 == TipoAlgoritmo.HRRN) {
            return razonRespuesta(candidato) > razonRespuesta(actual);
        }
        return false;
    }

    /* Calcula la razón de respuesta de HRRN. Lo llama mejorEnQ2. */
    private double razonRespuesta(BCP p) {
        int servicio = Math.max(1, p.getRafaga());
        int espera = Math.max(0, gestor.getInstanteSimulado() - p.getTiempoLlegada() - p.getTiempoCPU());
        return (espera + (double) servicio) / servicio;
    }

    /* Interrumpe por nivel superior o por la política del nivel. Lo llama Controlador. */
    @Override
    public boolean debeExpropiar(int cpuId, BCP actual) {
        if (actual == null) return false;
        int nivel = getNivel(actual.getPid());
        BCP siguiente = seleccionarSiguiente(cpuId);
        boolean agotoSuperior = turnoAgotado.remove(actual.getPid());
        if (siguiente != null && getNivel(siguiente.getPid()) < nivel) return true;
        if (agotoSuperior && siguiente != null && getNivel(siguiente.getPid()) <= nivel) return true;
        if (nivel != 2) return false;
        if (algoritmoQ2 == TipoAlgoritmo.RR && consumidos.getOrDefault(actual.getPid(), 0) >= 4) {
            consumidos.put(actual.getPid(), 0);
            return siguiente != null && getNivel(siguiente.getPid()) == 2;
        }
        return algoritmoQ2 == TipoAlgoritmo.SRT && siguiente != null
                && getNivel(siguiente.getPid()) == 2
                && siguiente.getTiempoRestante() < actual.getTiempoRestante();
    }

    /* Acumula el servicio recibido aunque el proceso se bloquee. Lo llama Controlador. */
    @Override
    public void alConsumirTick(int cpuId, BCP proceso) {
        consumidos.merge(proceso.getPid(), 1, Integer::sum);
    }
}
