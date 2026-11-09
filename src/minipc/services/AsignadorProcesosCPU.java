
package minipc.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import minipc.model.BCP;
import minipc.model.EstadoProceso;

public class AsignadorProcesosCPU {

    private static final int MAX_CPU = 5;

    private static final int MAX_PROCESOS_POR_CPU = 5;

    private final int cantidadCPU;

    private final Map<Integer, List<BCP>> procesosPorCPU;

    public AsignadorProcesosCPU(int cantidadCPU) {

        if (cantidadCPU < 1 || cantidadCPU > MAX_CPU) {

            throw new IllegalArgumentException(
                    "La cantidad de CPU debe estar entre 1 y 5.");
        }

        this.cantidadCPU = cantidadCPU;

        this.procesosPorCPU = new LinkedHashMap<>();

        for (int cpuId = 1; cpuId <= cantidadCPU; cpuId++) {

            procesosPorCPU.put(
                    cpuId,
                    new ArrayList<>());
        }
    }

    public int asignarProceso(BCP proceso) {

        if (proceso == null) {

            throw new IllegalArgumentException(
                    "El proceso no puede ser null.");
        }

        /*
         * Si el proceso ya tiene una asignación
         * registrada en este asignador, se conserva.
         */
        if (proceso.getCpuAsignada() != -1) {

            int cpuActual = proceso.getCpuAsignada();

            List<BCP> asignados = procesosPorCPU.get(cpuActual);

            if (asignados != null
                    && asignados.contains(proceso)) {

                return cpuActual;
            }

            throw new IllegalStateException(
                    "El proceso tiene una asignación "
                            + "que no pertenece a este asignador.");
        }

        int cpuSeleccionada = buscarCPUDisponible();

        if (cpuSeleccionada == -1) {

            cpuSeleccionada = buscarCPUMenorCarga();
        }

        if (cpuSeleccionada == -1) {

            throw new IllegalStateException(
                    "Todas las CPU alcanzaron el máximo "
                            + "de procesos asignados.");
        }

        proceso.setCpuAsignada(cpuSeleccionada);

        procesosPorCPU.get(cpuSeleccionada)
                .add(proceso);

        return cpuSeleccionada;
    }

    private int buscarCPUDisponible() {

        for (int cpuId = 1; cpuId <= cantidadCPU; cpuId++) {

            if (procesosPorCPU.get(cpuId).isEmpty()) {

                return cpuId;
            }
        }

        return -1;
    }

    private int buscarCPUMenorCarga() {

        int cpuSeleccionada = -1;

        long menorTiempo = Long.MAX_VALUE;

        int menorCantidad = Integer.MAX_VALUE;

        for (int cpuId = 1; cpuId <= cantidadCPU; cpuId++) {

            List<BCP> procesos = procesosPorCPU.get(cpuId);

            /*
             * No considerar CPU que ya alcanzaron
             * el máximo permitido.
             */
            if (procesos.size() >= MAX_PROCESOS_POR_CPU) {

                continue;
            }

            /*
             * Calcular el tiempo pendiente acumulado.
             */
            long tiempoActual = calcularCargaRestante(cpuId);

            int cantidadActual = procesos.size();

            /*
             * Elegir menor carga.
             *
             * Si existe empate en tiempo,
             * elegir menor cantidad de procesos.
             */
            if (tiempoActual < menorTiempo
                    || (tiempoActual == menorTiempo
                            && cantidadActual < menorCantidad)) {

                menorTiempo = tiempoActual;

                menorCantidad = cantidadActual;

                cpuSeleccionada = cpuId;
            }
        }

        return cpuSeleccionada;
    }

    public long calcularCargaRestante(int cpuId) {

        validarCPU(cpuId);

        long tiempoTotal = 0;

        for (BCP proceso : procesosPorCPU.get(cpuId)) {

            if (proceso.getEstado() == EstadoProceso.FINALIZADO) {

                continue;
            }

            tiempoTotal += Math.max(
                    0,
                    proceso.getTiempoRestante());
        }

        return tiempoTotal;
    }

    public void distribuirProcesos(List<BCP> procesos) {

        if (procesos == null) {

            throw new IllegalArgumentException(
                    "La lista de procesos no puede ser null.");
        }

        for (BCP proceso : procesos) {

            if (proceso.getEstado() != EstadoProceso.FINALIZADO) {

                asignarProceso(proceso);
            }
        }
    }

    public List<BCP> getProcesosAsignados(int cpuId) {

        validarCPU(cpuId);

        return Collections.unmodifiableList(
                new ArrayList<>(
                        procesosPorCPU.get(cpuId)));
    }

    public List<BCP> getProcesosPreparados(int cpuId) {

        validarCPU(cpuId);

        List<BCP> preparados = new ArrayList<>();

        for (BCP proceso : procesosPorCPU.get(cpuId)) {

            if (proceso.getEstado() == EstadoProceso.PREPARADO) {

                preparados.add(proceso);
            }
        }

        return preparados;
    }

    public int getCantidadProcesos(int cpuId) {

        validarCPU(cpuId);

        return procesosPorCPU.get(cpuId).size();
    }

    public void liberarCupo(BCP proceso) {

        if (proceso == null) {

            throw new IllegalArgumentException(
                    "El proceso no puede ser null.");
        }

        if (proceso.getEstado() != EstadoProceso.FINALIZADO) {

            throw new IllegalStateException(
                    "Solo se puede liberar el cupo "
                            + "de un proceso finalizado.");
        }

        int cpuId = proceso.getCpuAsignada();

        validarCPU(cpuId);

        procesosPorCPU.get(cpuId).remove(proceso);
    }

    public int getCantidadCPU() {

        return cantidadCPU;
    }

    private void validarCPU(int cpuId) {

        if (!procesosPorCPU.containsKey(cpuId)) {

            throw new IllegalArgumentException(
                    "CPU inexistente: " + cpuId);
        }
    }
}
