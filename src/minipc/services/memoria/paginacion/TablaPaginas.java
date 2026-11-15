package minipc.services.memoria.paginacion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import minipc.model.ConfiguracionPaginacion;

/** Tabla inmutable de un programa completo. Se reconstruye al reactivar el proceso. */
public final class TablaPaginas {
    private final int tamañoPagina;
    private final int cantidadInstrucciones;
    private final List<EntradaPagina> entradas;

    public TablaPaginas(int inicioUsuario, int tamañoPagina, int cantidadInstrucciones,
            List<Integer> marcos) {
        new ConfiguracionPaginacion(tamañoPagina);
        long necesarias = ((long) cantidadInstrucciones + tamañoPagina - 1) / tamañoPagina;
        if (inicioUsuario < 0 || cantidadInstrucciones <= 0 || marcos == null
                || necesarias != marcos.size()
                || marcos.stream().anyMatch(m -> m == null || m < 0)
                || new HashSet<>(marcos).size() != marcos.size()) {
            throw new IllegalArgumentException("La tabla de páginas no coincide con el programa.");
        }
        List<EntradaPagina> paginas = new ArrayList<>();
        for (int pagina = 0; pagina < marcos.size(); pagina++) {
            int marco = marcos.get(pagina);
            long inicio = (long) inicioUsuario + (long) marco * tamañoPagina;
            if (inicio + tamañoPagina > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("La dirección del marco excede el rango permitido.");
            }
            paginas.add(new EntradaPagina(pagina, marco, (int) inicio,
                    Math.min(tamañoPagina, cantidadInstrucciones - pagina * tamañoPagina)));
        }
        this.tamañoPagina = tamañoPagina;
        this.cantidadInstrucciones = cantidadInstrucciones;
        this.entradas = List.copyOf(paginas);
    }

    public int obtenerDireccionFisica(int direccionLogica) {
        if (direccionLogica < 0 || direccionLogica >= cantidadInstrucciones) {
            throw new IllegalArgumentException("Posición lógica fuera del programa: " + direccionLogica);
        }
        int pagina = direccionLogica / tamañoPagina;
        int desplazamiento = direccionLogica % tamañoPagina;
        return entradas.get(pagina).inicioMarco() + desplazamiento;
    }

    public List<EntradaPagina> getEntradas() { return entradas; }
    public int getTamañoPagina() { return tamañoPagina; }
    public int getFragmentacionInterna() {
        return entradas.getLast().instrucciones() == tamañoPagina ? 0
                : tamañoPagina - entradas.getLast().instrucciones();
    }
}
