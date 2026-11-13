package minipc.services;

import minipc.model.AlgoritmoPlanificacion;
import minipc.model.BCP;
import minipc.model.EstadoProceso;

/**
 * Planificador HRRN (Highest Response Ratio Next).
 *
 * Fundamentación:
 *
 * - Stallings, "Operating Systems: Internals and Design Principles",
 *   9.ª edición, §9.2, p. 443: define la razón de respuesta como
 *
 *       R = (w + s) / s
 *
 *   donde w es el tiempo que el proceso ha pasado esperando al procesador
 *   y s es el tiempo de servicio previsto (la ráfaga total). El valor mínimo
 *   de R es 1.0 cuando el proceso acaba de entrar al sistema. Cuando el
 *   proceso actual termina o se bloquea, se elige el proceso listo con el
 *   mayor valor de R.
 *
 * - Silberschatz, "Operating System Concepts", 10.ª edición, cap. 5:
 *   los algoritmos no expropiativos (FCFS, SJF no preventivo) conservan la
 *   CPU hasta que el proceso termina o se bloquea, y el tiempo de servicio
 *   debe estimarse a partir de la ráfaga de CPU. HRRN es no expropiativo.
 *
 * El envejecimiento implícito en w evita la inanición: aunque se favorecen
 * las ráfagas cortas (denominador menor), un proceso que espera demasiado
 * termina superando a los trabajos cortos que compiten por la CPU.
 */
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
