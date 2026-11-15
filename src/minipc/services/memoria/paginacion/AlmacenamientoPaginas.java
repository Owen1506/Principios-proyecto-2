package minipc.services.memoria.paginacion;

import java.util.ArrayList;
import java.util.List;
import minipc.model.Almacenamiento;
import minipc.model.ConfiguracionPaginacion;
import minipc.model.Instruccion;

// Conserva una copia de respaldo de cada página en la zona virtual existente.
// Cada instancia pertenece a un programa y libera únicamente sus propias reservas.
public final class AlmacenamientoPaginas {
    private final Almacenamiento almacenamiento;
    private final List<Integer> direcciones = new ArrayList<>();
    private final List<Integer> cantidades = new ArrayList<>();
    private boolean liberado;

    public AlmacenamientoPaginas(Almacenamiento almacenamiento,
            List<Instruccion> programa, int tamañoPagina) {
        if (almacenamiento == null || programa == null || programa.isEmpty()
                || programa.contains(null)) {
            throw new IllegalArgumentException("El almacenamiento y el programa deben ser válidos.");
        }
        new ConfiguracionPaginacion(tamañoPagina);
        this.almacenamiento = almacenamiento;

        // Si alguna página no cabe, se revierten las reservas de esta operación.
        try {
            for (int inicio = 0; inicio < programa.size();) {
                int fin = (int) Math.min((long) inicio + tamañoPagina, programa.size());
                List<Instruccion> pagina = programa.subList(inicio, fin);
                int direccion = almacenamiento.guardarProgramaVirtual(pagina);
                direcciones.add(direccion);
                cantidades.add(pagina.size());
                inicio = fin;
            }
        } catch (RuntimeException error) {
            liberarReservas();
            throw error;
        }
    }

    public int cantidadPaginas() {
        comprobarActivo();
        return direcciones.size();
    }

    public int direccionPagina(int pagina) {
        comprobarPagina(pagina);
        return direcciones.get(pagina);
    }

    public List<Instruccion> leerPagina(int pagina) {
        comprobarPagina(pagina);
        return almacenamiento.leerProgramaVirtual(direcciones.get(pagina), cantidades.get(pagina));
    }

    public void liberar() {
        comprobarActivo();
        liberarReservas();
        liberado = true;
    }

    private void liberarReservas() {
        for (int i = 0; i < direcciones.size(); i++) {
            almacenamiento.liberarProgramaVirtual(direcciones.get(i), cantidades.get(i));
        }
        direcciones.clear();
        cantidades.clear();
    }

    private void comprobarPagina(int pagina) {
        comprobarActivo();
        if (pagina < 0 || pagina >= direcciones.size()) {
            throw new IllegalArgumentException("Número de página fuera del programa.");
        }
    }

    private void comprobarActivo() {
        if (liberado) {
            throw new IllegalStateException("El respaldo de páginas ya fue liberado.");
        }
    }
}
