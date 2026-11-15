package minipc.services.memoria.dinamica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import minipc.services.memoria.AsignacionMemoria;
import minipc.services.memoria.EstrategiaAsignacionMemoria;
import minipc.services.memoria.Hueco;

//esta clase es abstracta porque no define la política de selección de huecos; cada subclase implementa su propia estrategia (First Fit, Next Fit, Best Fit).
public abstract class AsignacionDeRegistros implements EstrategiaAsignacionMemoria {

    private final int inicioUsuario;
    private final int tamañoUsuario;
    private final List<Hueco> huecos = new ArrayList<>();
    private final Map<AsignacionMemoria, Hueco> reservas = new IdentityHashMap<>();

    protected AsignacionDeRegistros(int inicioUsuario, int tamañoMemoria) {
        if (inicioUsuario < 0 || tamañoMemoria <= inicioUsuario) {
            throw new IllegalArgumentException("El área de usuario no es válida.");
        }
        this.inicioUsuario = inicioUsuario;
        this.tamañoUsuario = tamañoMemoria - inicioUsuario;
        this.huecos.add(new Hueco(inicioUsuario, tamañoUsuario));
    }

    // Cada subclase implementa su propia política de selección de huecos. La llama
    // {@link #asignar(int)} para elegir un hueco libre que alcance la cantidad
    // solicitada.
    protected abstract Hueco seleccionarHueco(int cantidad);

    @Override
    // se encarga de reservar un bloque de memoria para un proceso, actualizando la
    // lista de huecos libres y registrando la reserva. La llama Memoria cuando un
    // proceso solicita memoria.
    public final AsignacionMemoria asignar(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }

        Hueco elegido = seleccionarHueco(cantidad);
        if (elegido == null) {
            throw new IllegalStateException(
                    "No hay espacio suficiente en memoria para cargar el programa.");
        }

        int indice = huecos.indexOf(elegido);
        if (indice < 0) {
            throw new IllegalStateException(
                    "La estrategia seleccionó un hueco que no está libre.");
        }

        Hueco libre = huecos.get(indice);
        if (!libre.alcanzaPara(cantidad)) {
            throw new IllegalStateException(
                    "El hueco seleccionado no tiene espacio suficiente.");
        }

        if (libre.tamaño() == cantidad) {
            huecos.remove(indice);
        } else {
            huecos.set(indice, new Hueco(
                    libre.inicio() + cantidad,
                    libre.tamaño() - cantidad));
        }

        List<Integer> posiciones = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            posiciones.add(libre.inicio() + i);
        }
        // aca se crea un objeto AsignacionMemoria que representa la reserva de memoria
        // para un proceso, y se registra en el mapa de reservas. Luego se llama al
        // método alAsignar() para que la subclase pueda actualizar su estado propio
        // después de la asignación.
        AsignacionMemoria asignacion = new AsignacionMemoria(posiciones);
        Hueco reservado = new Hueco(libre.inicio(), cantidad);
        reservas.put(asignacion, reservado);
        alAsignar(reservado);
        return asignacion;
    }

    @Override
    public final void liberar(AsignacionMemoria asignacion) {
        if (asignacion == null) {
            throw new IllegalArgumentException("La asignación no puede ser null.");
        }

        Hueco liberado = reservas.remove(asignacion);
        if (liberado == null) {
            throw new IllegalArgumentException(
                    "La asignación no pertenece a esta estrategia o ya fue liberada.");
        }

        insertarYFusionar(liberado);
        alLiberar(liberado);
    }

    // Inserta un hueco libre en la lista de huecos y fusiona con los vecinos si es
    // posible. La llama {@link #liberar(AsignacionMemoria)} después de liberar un
    private void insertarYFusionar(Hueco nuevo) {
        int indice = 0;
        while (indice < huecos.size()
                && huecos.get(indice).inicio() < nuevo.inicio()) {
            indice++;
        }
        huecos.add(indice, nuevo);

        if (indice + 1 < huecos.size()) {
            Hueco siguiente = huecos.get(indice + 1);
            if (nuevo.fin() == siguiente.inicio()) {
                huecos.set(indice, new Hueco(
                        nuevo.inicio(),
                        nuevo.tamaño() + siguiente.tamaño()));
                huecos.remove(indice + 1);
            }
        }

        if (indice > 0) {
            Hueco anterior = huecos.get(indice - 1);
            Hueco actual = huecos.get(indice);
            if (anterior.fin() == actual.inicio()) {
                huecos.set(indice - 1, new Hueco(
                        anterior.inicio(),
                        anterior.tamaño() + actual.tamaño()));
                huecos.remove(indice);
            }
        }
    }

    @Override
    public final boolean hayEspacioDisponible(int cantidad) {
        return cantidad > 0
                && huecos.stream().anyMatch(hueco -> hueco.alcanzaPara(cantidad));
    }

    @Override
    public final int getPosicionesLibres() {
        return huecos.stream().mapToInt(Hueco::tamaño).sum();
    }

    @Override
    public final void reiniciar() {
        reservas.clear();
        huecos.clear();
        if (tamañoUsuario > 0) {
            huecos.add(new Hueco(inicioUsuario, tamañoUsuario));
        }
        alReiniciar();
    }

    // Devuelve una lista inmutable de los huecos libres, para que la subclase pueda
    // consultarlos sin poder modificarlos. La llama {@link #asignar(int)} para
    // saber qué huecos están disponibles y {@link #verificarIntegridad()} para
    // comprobar que la lista de huecos y reservas es consistente.
    protected final List<Hueco> huecos() {
        return Collections.unmodifiableList(huecos);
    }

    /** Expone los huecos a la interfaz y al diagnóstico sin permitir cambios. */
    public final List<Hueco> getHuecos() {
        return Collections.unmodifiableList(huecos);
    }

    public final int getInicioUsuario() {
        return inicioUsuario;
    }

    public final int getTamañoUsuario() {
        return tamañoUsuario;
    }


    protected void alReiniciar() {
    }


    protected void alAsignar(Hueco reservado) {
    }

    protected void alLiberar(Hueco liberado) {
    }


    public final boolean verificarIntegridad() {
        List<Hueco> bloques = new ArrayList<>(huecos);
        bloques.addAll(reservas.values());
        bloques.sort(Comparator.comparingInt(Hueco::inicio));

        int esperado = inicioUsuario;
        for (Hueco bloque : bloques) {
            if (bloque.inicio() != esperado) {
                return false;
            }
            esperado = bloque.fin();
        }
        return esperado == inicioUsuario + tamañoUsuario;
    }
}
