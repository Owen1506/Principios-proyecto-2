package minipc.services.memoria.paginacion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import minipc.model.ConfiguracionPaginacion;
import minipc.services.memoria.AsignacionMemoria;
import minipc.services.memoria.EstrategiaAsignacionMemoria;

/** Paginación simple: reserva todos los marcos necesarios antes de cargar un programa. */
public final class GestorPaginacion implements EstrategiaAsignacionMemoria {
    private final int inicioUsuario;
    private final int tamañoPagina;
    private final int sobrante;
    private final int[] ocupacion;
    private final Map<AsignacionMemoria, TablaPaginas> tablas = new IdentityHashMap<>();

    public GestorPaginacion(int inicioUsuario, int finMemoria, int tamañoPagina) {
        new ConfiguracionPaginacion(tamañoPagina);
        if (inicioUsuario < 0 || finMemoria <= inicioUsuario
                || tamañoPagina > finMemoria - inicioUsuario) {
            throw new IllegalArgumentException("Debe caber al menos un marco en el área de usuario.");
        }
        this.inicioUsuario = inicioUsuario;
        this.tamañoPagina = tamañoPagina;
        this.ocupacion = new int[(finMemoria - inicioUsuario) / tamañoPagina];
        this.sobrante = (finMemoria - inicioUsuario) % tamañoPagina;
    }

    @Override
    public AsignacionMemoria asignar(int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva.");
        if (!hayEspacioDisponible(cantidad)) {
            throw new IllegalStateException("No hay suficientes marcos libres para cargar todas las páginas.");
        }
        int necesarias = (int) (((long) cantidad + tamañoPagina - 1) / tamañoPagina);
        List<Integer> elegidos = new ArrayList<>(necesarias);
        for (int marco = 0; marco < ocupacion.length && elegidos.size() < necesarias; marco++) {
            if (ocupacion[marco] == 0) elegidos.add(marco);
        }
        TablaPaginas tabla = new TablaPaginas(inicioUsuario, tamañoPagina, cantidad, elegidos);
        List<Integer> posiciones = new ArrayList<>(cantidad);
        for (int logica = 0; logica < cantidad; logica++) {
            posiciones.add(tabla.obtenerDireccionFisica(logica));
        }
        AsignacionMemoria asignacion = new AsignacionMemoria(posiciones);
        for (EntradaPagina pagina : tabla.getEntradas()) {
            ocupacion[pagina.marco()] = pagina.instrucciones();
        }
        tablas.put(asignacion, tabla);
        return asignacion;
    }

    @Override
    public void liberar(AsignacionMemoria asignacion) {
        TablaPaginas tabla = tablas.remove(asignacion);
        if (tabla == null) throw new IllegalArgumentException("La reserva no pertenece a esta paginación.");
        for (EntradaPagina pagina : tabla.getEntradas()) ocupacion[pagina.marco()] = 0;
    }

    @Override
    public boolean hayEspacioDisponible(int cantidad) {
        return cantidad > 0 && (long) cantidad <= getPosicionesLibres();
    }

    @Override
    public int getPosicionesLibres() {
        int libres = 0;
        for (int usadas : ocupacion) if (usadas == 0) libres++;
        return libres * tamañoPagina;
    }

    public int getCapacidad() { return ocupacion.length * tamañoPagina; }
    public int getSobrante() { return sobrante; }

    public TablaPaginas getTabla(AsignacionMemoria asignacion) { return tablas.get(asignacion); }

    public List<Marco> getMarcos() {
        List<Marco> marcos = new ArrayList<>(ocupacion.length);
        for (int numero = 0; numero < ocupacion.length; numero++) {
            marcos.add(new Marco(numero, inicioUsuario + numero * tamañoPagina,
                    tamañoPagina, ocupacion[numero]));
        }
        return List.copyOf(marcos);
    }

    @Override
    public void reiniciar() {
        Arrays.fill(ocupacion, 0);
        tablas.clear();
    }
}
