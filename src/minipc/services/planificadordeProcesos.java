package minipc.services;

import minipc.model.BCP;

/** Selecciona el siguiente proceso preparado mediante el algoritmo configurado. */
public class planificadordeProcesos {

    private final String tipoAlgoritmo;
    private final GestorProcesos gestorProcesos;

    public planificadordeProcesos(String tipoAlgoritmo, GestorProcesos gestorProcesos) {
        if (gestorProcesos == null) {
            throw new IllegalArgumentException("El gestor de procesos no puede ser null.");
        }
        if (!"FCFS".equals(tipoAlgoritmo)) {
            throw new IllegalArgumentException("Algoritmo no implementado: " + tipoAlgoritmo);
        }
        this.tipoAlgoritmo = tipoAlgoritmo;
        this.gestorProcesos = gestorProcesos;
    }

    /** La seleccion no retira el proceso de la cola; lo hace GestorProcesos. */
    public BCP seleccionarSiguiente() {
        if (!hayProcesosPreparados()) {
            return null;
        }

        switch (tipoAlgoritmo) {
            case "FCFS":
                return seleccionarFCFS();
            default:
                throw new IllegalArgumentException("Algoritmo no implementado: " + tipoAlgoritmo);
        }
    }

    private BCP seleccionarFCFS() {
        return gestorProcesos.getColaPreparados().peek();
    }

    public boolean hayProcesosPreparados() {
        return !gestorProcesos.getColaPreparados().isEmpty();
    }
}
