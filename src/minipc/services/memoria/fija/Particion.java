package minipc.services.memoria.fija;

// El tamaño y la dirección nunca cambian; solo cambia la ocupación.
public final class Particion {
    private final int inicio;
    private final int tamaño;
    private int tamañoPrograma;

    Particion(int inicio, int tamaño) {
        this.inicio = inicio;
        this.tamaño = tamaño;
    }

    public int inicio() { return inicio; }
    public int tamaño() { return tamaño; }
    public int fin() { return inicio + tamaño; }
    public int tamañoPrograma() { return tamañoPrograma; }
    public boolean libre() { return tamañoPrograma == 0; }
    public int fragmentacionInterna() { return libre() ? 0 : tamaño - tamañoPrograma; }

    void ocupar(int cantidad) { tamañoPrograma = cantidad; }
    void liberar() { tamañoPrograma = 0; }
}
