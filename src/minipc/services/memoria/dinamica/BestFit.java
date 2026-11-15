package minipc.services.memoria.dinamica;

import minipc.services.memoria.Hueco;
import minipc.services.memoria.seleccion.SeleccionBloques;

/// Estrategia Best Fit: reserva el hueco libre más pequeño que alcance.
public final class BestFit extends AsignacionDeRegistros {

    public BestFit(int inicioUsuario, int tamañoMemoria) {
        super(inicioUsuario, tamañoMemoria);
    }

    @Override
    protected Hueco seleccionarHueco(int cantidad) {
        return SeleccionBloques.mejor(huecos(), cantidad, Hueco::tamaño);
    }
}
