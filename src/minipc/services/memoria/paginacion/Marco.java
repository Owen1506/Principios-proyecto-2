package minipc.services.memoria.paginacion;

/** Fotografía de un marco; las posiciones sobrantes de uno ocupado no están libres. */
public record Marco(int numero, int inicio, int tamaño, int posicionesUtilizadas) {
    public boolean libre() { return posicionesUtilizadas == 0; }
    public int fin() { return inicio + tamaño; }
    public int fragmentacionInterna() { return libre() ? 0 : tamaño - posicionesUtilizadas; }
}
