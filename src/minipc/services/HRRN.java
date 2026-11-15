package minipc.services;

import minipc.model.AlgoritmoPlanificacion;
import minipc.model.BCP;
import minipc.model.EstadoProceso;

public class HRRN implements AlgoritmoPlanificacion {

    private final GestorProcesos gestorProcesos;

    public HRRN(GestorProcesos gestorProcesos) {

        if (gestorProcesos == null) {
            throw new IllegalArgumentException(
                    "El gestor de procesos no puede ser null.");
        }

        this.gestorProcesos = gestorProcesos;
    }

    @Override
    public BCP seleccionarSiguiente(int cpuId) {

        BCP elegido = null;
        double mejorRazon = Double.NEGATIVE_INFINITY;

        for (BCP candidato : gestorProcesos.getColaPreparados()) {

            if (candidato.getCpuAsignada() != cpuId
                    || candidato.getEstado() != EstadoProceso.PREPARADO) {

                continue;
            }

            double razon = calcularRazonRespuesta(candidato);

            if (elegido == null
                    || razon > mejorRazon
                    || (razon == mejorRazon
                            && esMejorDesempate(candidato, elegido))) {

                elegido = candidato;
                mejorRazon = razon;
            }
        }

        return elegido;
    }

    /**
     * Calcula R = (w + s) / s.
     *
     * w = instante simulado - tiempo de llegada - tiempo de CPU ya consumido.
     * Se descuenta el servicio ya recibido para no contarlo dos veces cuando
     * el proceso estuvo bloqueado y volvió a la cola de preparados.
     *
     * s = ráfaga total prevista del proceso.
     */
    private double calcularRazonRespuesta(BCP proceso) {

        int servicio = Math.max(1, proceso.getRafaga());

        int espera = gestorProcesos.getInstanteSimulado()
                - proceso.getTiempoLlegada()
                - proceso.getTiempoCPU();

        if (espera < 0) {
            espera = 0;
        }

        return (espera + (double) servicio) / servicio;
    }

    /**
     * Desempate determinista: menor tiempo de llegada, luego menor ráfaga
     * y finalmente menor PID.
     */
    private boolean esMejorDesempate(BCP candidato, BCP actual) {

        if (candidato.getTiempoLlegada() != actual.getTiempoLlegada()) {

            return candidato.getTiempoLlegada()
                    < actual.getTiempoLlegada();
        }

        if (candidato.getRafaga() != actual.getRafaga()) {

            return candidato.getRafaga()
                    < actual.getRafaga();
        }

        return candidato.getPid() < actual.getPid();
    }
}
