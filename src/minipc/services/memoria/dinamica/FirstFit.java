package minipc.services.memoria.dinamica;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import minipc.services.memoria.AsignacionMemoria;
import minipc.services.memoria.EstrategiaAsignacionMemoria;

/** Reserva el primer bloque contiguo con espacio suficiente. */
public final class FirstFit implements EstrategiaAsignacionMemoria {
    private final int inicioUsuario;
    private final boolean[] ocupadas;
    private final Set<AsignacionMemoria> reservas = new HashSet<>();

    public FirstFit(int inicioUsuario, int tamañoMemoria) {
        if (inicioUsuario < 0 || tamañoMemoria <= inicioUsuario) {
            throw new IllegalArgumentException("El área de usuario no es válida.");
        }
        this.inicioUsuario = inicioUsuario;
        ocupadas = new boolean[tamañoMemoria - inicioUsuario];
    }

    private int buscarInicio(int cantidad) {
        if (cantidad <= 0 || cantidad > ocupadas.length) return -1;
        int consecutivas = 0;
        for (int i = 0; i < ocupadas.length; i++) {
            consecutivas = ocupadas[i] ? 0 : consecutivas + 1;
            if (consecutivas == cantidad) return i - cantidad + 1;
        }
        return -1;
    }

    @Override
    public AsignacionMemoria asignar(int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva.");
        int inicio = buscarInicio(cantidad);
        if (inicio < 0) throw new IllegalStateException("No hay espacio suficiente en memoria para cargar el programa.");
        ArrayList<Integer> posiciones = new ArrayList<>();
        for (int i = inicio; i < inicio + cantidad; i++) posiciones.add(inicioUsuario + i);
        AsignacionMemoria asignacion = new AsignacionMemoria(posiciones);
        for (int i = inicio; i < inicio + cantidad; i++) ocupadas[i] = true;
        reservas.add(asignacion);
        return asignacion;
    }

    @Override
    public void liberar(AsignacionMemoria asignacion) {
        if (!reservas.remove(asignacion)) {
            throw new IllegalArgumentException("La asignación no pertenece a esta estrategia o ya fue liberada.");
        }
        for (int posicion : asignacion.getPosiciones()) ocupadas[posicion - inicioUsuario] = false;
    }

    @Override
    public boolean hayEspacioDisponible(int cantidad) { return buscarInicio(cantidad) >= 0; }

    @Override
    public int getPosicionesLibres() {
        int libres = 0;
        for (boolean ocupada : ocupadas) if (!ocupada) libres++;
        return libres;
    }

    @Override
    public void reiniciar() {
        java.util.Arrays.fill(ocupadas, false);
        reservas.clear();
    }
}
