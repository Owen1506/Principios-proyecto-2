package minipc.services.memoria;

import java.util.HashSet;
import java.util.List;

/// Representa la asignación de memoria de un programa, que consiste en una lista de posiciones físicas distintas
/// que ocupan las posiciones lógicas del programa. La crea la estrategia de asignación
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
