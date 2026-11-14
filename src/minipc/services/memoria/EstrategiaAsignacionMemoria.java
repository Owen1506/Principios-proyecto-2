package minipc.services.memoria;

/** Administra reservas en el área de usuario; no almacena instrucciones ni BCP. */
public interface EstrategiaAsignacionMemoria {
    /** Reserva posiciones o lanza una excepción sin modificar reservas anteriores. */
    AsignacionMemoria asignar(int cantidad);
    void liberar(AsignacionMemoria asignacion);
    boolean hayEspacioDisponible(int cantidad);
    int getPosicionesLibres();
    void reiniciar();
}
