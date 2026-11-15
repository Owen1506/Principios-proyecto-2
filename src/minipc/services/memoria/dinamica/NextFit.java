package minipc.services.memoria.dinamica;

import minipc.services.memoria.Hueco;

// Estrategia Next Fit: reserva el primer hueco libre que alcance después del último reservado.
public final class NextFit extends AsignacionDeRegistros {

    private int siguienteDireccion;

    public NextFit(int inicioUsuario, int tamañoMemoria) {
        super(inicioUsuario, tamañoMemoria);
        siguienteDireccion = inicioUsuario;
    }

    @Override
    // Selecciona el primer hueco libre que alcance la cantidad solicitada,
    // empezando desde la dirección siguiente a la última reserva. Si no hay
    // ninguno, vuelve a empezar desde el inicio del área de usuario.
    protected Hueco seleccionarHueco(int cantidad) {
        for (Hueco hueco : huecos()) {
            if (hueco.fin() > siguienteDireccion && hueco.alcanzaPara(cantidad)) {
                return hueco;
            }
        }

        for (Hueco hueco : huecos()) {
            if (hueco.fin() <= siguienteDireccion && hueco.alcanzaPara(cantidad)) {
                return hueco;
            }
        }

        return null;
    }

    @Override
    // Actualiza la dirección siguiente a la última reserva, para que la próxima
    // búsqueda empiece desde allí.
    protected void alAsignar(Hueco reservado) {
        siguienteDireccion = reservado.fin();
        if (siguienteDireccion == getInicioUsuario() + getTamañoUsuario()) {
            siguienteDireccion = getInicioUsuario();
        }
    }

    @Override
    // Reinicia la dirección siguiente a la primera dirección del área de usuario,
    protected void alReiniciar() {
        siguienteDireccion = getInicioUsuario();
    }
}
