package minipc.services.memoria.dinamica;

import minipc.services.memoria.Hueco;

/// Estrategia Best Fit: reserva el hueco libre más pequeño que alcance.
public final class BestFit extends AsignacionDeRegistros {

    public BestFit(int inicioUsuario, int tamañoMemoria) {
        super(inicioUsuario, tamañoMemoria);
    }

    @Override
    protected Hueco seleccionarHueco(int cantidad) {
        Hueco mejor = null;

        for (Hueco hueco : huecos()) {
            if (hueco.alcanzaPara(cantidad)
                    && (mejor == null || hueco.tamaño() < mejor.tamaño())) {
                mejor = hueco;
            }
        }

        // Si hay empate, se conserva el primero por orden de dirección.
        return mejor;
    }
}
