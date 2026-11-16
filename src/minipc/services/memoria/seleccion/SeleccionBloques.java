package minipc.services.memoria.seleccion;

import java.util.List;
import java.util.function.ToIntFunction;

// Comparte la elección entre huecos dinámicos y particiones fijas libres.
public final class SeleccionBloques {
    private SeleccionBloques() {
    }

    public static <T> T primero(List<T> bloques, int cantidad, ToIntFunction<T> tamaño) {
        for (T bloque : bloques) {
            if (tamaño.applyAsInt(bloque) >= cantidad) {
                return bloque;
            }
        }
        return null;
    }

    public static <T> T mejor(List<T> bloques, int cantidad, ToIntFunction<T> tamaño) {
        T elegido = null;
        for (T bloque : bloques) {
            int espacio = tamaño.applyAsInt(bloque);
            if (espacio >= cantidad && (elegido == null || espacio < tamaño.applyAsInt(elegido))) {
                elegido = bloque;
            }
        }
        return elegido;
    }
}
