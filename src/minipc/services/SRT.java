
package minipc.services;

import minipc.model.AlgoritmoPlanificacion;
import minipc.model.BCP;
import minipc.model.EstadoProceso;

public class SRT implements AlgoritmoPlanificacion {

    private final GestorProcesos gestorProcesos;

    public SRT(GestorProcesos gestorProcesos) {
        if (gestorProcesos == null) {
            throw new IllegalArgumentException("El gestor de procesos no puede ser null.");
        }
        this.gestorProcesos = gestorProcesos;
    }

    public BCP seleccionarSiguiente(int cpuId) {
        BCP elegido = null;
        for (BCP candidato : gestorProcesos.getColaPreparados()) {

            if (candidato.getCpuAsignada() != cpuId
                    || candidato.getEstado() != EstadoProceso.PREPARADO) {

                continue;
            }
            if (elegido == null || candidato.getTiempoRestante() < elegido.getTiempoRestante()
                    || candidato.getTiempoRestante() == elegido.getTiempoRestante()
                            && candidato.getTiempoLlegada() < elegido.getTiempoLlegada()) {
                elegido = candidato;
            }
        }
        return elegido;
    }


    public boolean debeExpropiar(int cpuId, BCP actual) {
        BCP siguiente = seleccionarSiguiente(cpuId);
        return siguiente != null && siguiente.getTiempoRestante() < actual.getTiempoRestante();
    }

}
