package minipc.services.memoria.dinamica;

import minipc.services.memoria.Hueco;
import minipc.services.memoria.seleccion.SeleccionBloques;

// Estrategia First Fit: reserva el primer hueco libre que alcance.
public final class FirstFit extends AsignacionDeRegistros {

    public FirstFit(int inicioUsuario, int tamañoMemoria) {
        super(inicioUsuario, tamañoMemoria);
    }

    @Override
    protected Hueco seleccionarHueco(int cantidad) {
        return SeleccionBloques.primero(huecos(), cantidad, Hueco::tamaño);
    }
}
