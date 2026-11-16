package minipc.services.memoria.fija;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import minipc.services.memoria.AsignacionMemoria;
import minipc.services.memoria.EstrategiaAsignacionMemoria;
import minipc.services.memoria.seleccion.SeleccionBloques;

// Cada reserva ocupa una partición completa, aunque el programa sea menor.
public final class GestorParticionesFijas implements EstrategiaAsignacionMemoria {
    private final List<Particion> particiones = new ArrayList<>();
    private final Map<AsignacionMemoria, Particion> reservas = new IdentityHashMap<>();
    private final boolean mejorAjuste;

    public GestorParticionesFijas(int inicio, int fin, List<Integer> tamaños, boolean mejorAjuste) {
        if (inicio < 0 || fin <= inicio || tamaños == null || tamaños.isEmpty()) {
            throw new IllegalArgumentException("La distribución de particiones no es válida.");
        }
        long siguiente = inicio;
        for (Integer tamaño : tamaños) {
            if (tamaño == null || tamaño <= 0 || siguiente + tamaño > fin) {
                throw new IllegalArgumentException(
                        "Las particiones exceden el área de usuario o tienen tamaño inválido.");
            }
            particiones.add(new Particion((int) siguiente, tamaño));
            siguiente += tamaño;
        }
        this.mejorAjuste = mejorAjuste;
    }

    @Override
    // Se encarga de reservar una partición completa para un proceso, actualizando
    // la lista de particiones libres y registrando la reserva. La llama Memoria
    // cuando un proceso solicita memoria.
    public AsignacionMemoria asignar(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }
        List<Particion> libres = particiones.stream().filter(Particion::libre).toList();
        Particion elegida = mejorAjuste
                ? SeleccionBloques.mejor(libres, cantidad, Particion::tamaño)
                : SeleccionBloques.primero(libres, cantidad, Particion::tamaño);
        if (elegida == null) {
            throw new IllegalStateException("No hay una partición libre suficientemente grande.");
        }
        List<Integer> posiciones = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            posiciones.add(elegida.inicio() + i);
        }
        AsignacionMemoria asignacion = new AsignacionMemoria(posiciones);
        elegida.ocupar(cantidad);
        reservas.put(asignacion, elegida);
        return asignacion;
    }

    @Override
    public void liberar(AsignacionMemoria asignacion) {
        Particion particion = reservas.remove(asignacion);
        if (particion == null) {
            throw new IllegalArgumentException("La reserva no pertenece a estas particiones o ya fue liberada.");
        }
        particion.liberar();
    }

    @Override
    public boolean hayEspacioDisponible(int cantidad) {
        return cantidad > 0 && particiones.stream().anyMatch(p -> p.libre() && p.tamaño() >= cantidad);
    }

    // El espacio libre cuenta particiones enteras, no fragmentación interna.
    @Override
    public int getPosicionesLibres() {
        return particiones.stream().filter(Particion::libre).mapToInt(Particion::tamaño).sum();
    }

    @Override
    public void reiniciar() {
        reservas.clear();
        particiones.forEach(Particion::liberar);
    }

    public List<Particion> getParticiones() {
        return Collections.unmodifiableList(particiones);
    }

    public int tamañoMaximo() {
        return particiones.stream().mapToInt(Particion::tamaño).max().orElse(0);
    }
}
