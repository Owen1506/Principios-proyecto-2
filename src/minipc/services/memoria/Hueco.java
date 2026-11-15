package minipc.services.memoria;

// Representa un hueco libre en el área de usuario, definido por su dirección inicial y su tamaño.
public record Hueco(int inicio, int tamaño) {

    public Hueco {
        if (inicio < 0) {
            throw new IllegalArgumentException("El inicio del hueco no puede ser negativo.");
        }
        if (tamaño <= 0) {
            throw new IllegalArgumentException("El tamaño del hueco debe ser positivo.");
        }
        if ((long) inicio + tamaño > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("El hueco excede el rango de direcciones.");
        }
    }

    /** Devuelve la primera dirección que queda fuera del hueco. */
    public int fin() {
        return inicio + tamaño;
    }

    /** Indica si el hueco puede contener una reserva de la cantidad indicada. */
    public boolean alcanzaPara(int cantidad) {
        return cantidad > 0 && tamaño >= cantidad;
    }
}
