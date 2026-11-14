package minipc.services.memoria;

import java.util.HashSet;
import java.util.List;

/** Posiciones físicas de un programa, ordenadas por posición lógica. */
public final class AsignacionMemoria {
    private final List<Integer> posiciones;

    public AsignacionMemoria(List<Integer> posiciones) {
        if (posiciones == null || posiciones.isEmpty()
                || posiciones.stream().anyMatch(p -> p == null || p < 0)
                || new HashSet<>(posiciones).size() != posiciones.size()) {
            throw new IllegalArgumentException("Las posiciones deben ser válidas y distintas.");
        }
        this.posiciones = List.copyOf(posiciones);
    }

    public int obtenerDireccionFisica(int posicionLogica) {
        if (posicionLogica < 0 || posicionLogica >= posiciones.size()) {
            throw new IllegalArgumentException("Posición lógica fuera del programa: " + posicionLogica);
        }
        return posiciones.get(posicionLogica);
    }

    public int getCantidad() { return posiciones.size(); }
    public List<Integer> getPosiciones() { return posiciones; }
}
