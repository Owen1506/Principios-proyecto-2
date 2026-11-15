package minipc.services.memoria.dinamica;

import minipc.services.memoria.Hueco;

// Estrategia First Fit: reserva el primer hueco libre que alcance.
public final class FirstFit extends AsignacionDeRegistros {

    public FirstFit(int inicioUsuario, int tamañoMemoria) {
        super(inicioUsuario, tamañoMemoria);
    }

    @Override
    protected Hueco seleccionarHueco(int cantidad) {
        for (Hueco hueco : huecos()) {
            if (hueco.alcanzaPara(cantidad)) {
                return hueco;
            }
        }
        return null;
    }
}
