package minipc.services.memoria.paginacion;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import minipc.model.Almacenamiento;
import minipc.model.ConfiguracionPaginacion;
import minipc.model.Instruccion;
import minipc.services.memoria.AsignacionMemoria;
import minipc.services.memoria.EstrategiaAsignacionMemoria;

// Implementa paginación virtual con carga por demanda y reemplazo FIFO.
// Las páginas se respaldan individualmente en la zona virtual de almacenamiento.
public final class GestorPaginasVirtuales implements EstrategiaAsignacionMemoria {
    private static final class Programa {
        private final AlmacenamientoPaginas respaldo;
        private final int cantidad;
        private final int[] marcos;

        private Programa(AlmacenamientoPaginas respaldo, int cantidad) {
            this.respaldo = respaldo;
            this.cantidad = cantidad;
            this.marcos = new int[respaldo.cantidadPaginas()];
            Arrays.fill(marcos, -1);
        }
    }

    private static final class MarcoOcupado {
        private final AsignacionMemoria asignacion;
        private final int pagina;
        private final List<Instruccion> instrucciones;

        private MarcoOcupado(AsignacionMemoria asignacion, int pagina, List<Instruccion> instrucciones) {
            this.asignacion = asignacion;
            this.pagina = pagina;
            this.instrucciones = instrucciones;
        }
    }

    public record EstadoPagina(int pagina, boolean presente, int marco, int direccionVirtual) {
    }

    public record EstadoMarco(int numero, int inicio, int tamaño, int pidRepresentativo,
            int pagina, int posicionesUtilizadas) {
        public boolean libre() {
            return pidRepresentativo < 0;
        }
    }

    public record ResultadoAcceso(Instruccion instruccion, int direccionFisica,
            boolean falloPagina, boolean reemplazo) {
    }

    private final Almacenamiento almacenamiento;
    private final int tamañoPagina;
    private final int inicioUsuario;
    private final MarcoOcupado[] marcos;
    private final Map<AsignacionMemoria, Programa> programas = new HashMap<>();
    private final ArrayDeque<Integer> fifo = new ArrayDeque<>();
    private int fallos;
    private int reemplazos;

    public GestorPaginasVirtuales(Almacenamiento almacenamiento, int inicioUsuario,
            int finMemoria, int tamañoPagina) {
        new ConfiguracionPaginacion(tamañoPagina, true);
        if (almacenamiento == null || inicioUsuario < 0 || finMemoria <= inicioUsuario
                || tamañoPagina > finMemoria - inicioUsuario) {
            throw new IllegalArgumentException("La memoria debe tener al menos un marco válido.");
        }
        this.almacenamiento = almacenamiento;
        this.inicioUsuario = inicioUsuario;
        this.tamañoPagina = tamañoPagina;
        this.marcos = new MarcoOcupado[(finMemoria - inicioUsuario) / tamañoPagina];
    }

    // Registra el respaldo completo antes de registrar la asignación en Memoria.
    public void registrarRespaldo(AsignacionMemoria asignacion, List<Instruccion> programa) {
        if (asignacion == null || programa == null || programa.isEmpty() || programa.contains(null)) {
            throw new IllegalArgumentException("La asignación y el programa deben ser válidos.");
        }
        if (programas.containsKey(asignacion)) {
            throw new IllegalStateException("La asignación ya tiene respaldo de páginas.");
        }
        AlmacenamientoPaginas respaldo = new AlmacenamientoPaginas(
                almacenamiento, programa, tamañoPagina);
        programas.put(asignacion, new Programa(respaldo, programa.size()));
    }

    @Override
    public AsignacionMemoria asignar(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }
        if (!hayEspacioDisponible(cantidad)) {
            throw new IllegalStateException("No hay respaldo virtual suficiente para admitir el programa.");
        }
        // Las posiciones físicas son marcadores; los marcos reales se asignan bajo demanda.
        List<Integer> posiciones = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            posiciones.add(inicioUsuario + i);
        }
        return new AsignacionMemoria(posiciones);
    }

    @Override
    public void liberar(AsignacionMemoria asignacion) {
        Programa programa = programas.remove(asignacion);
        if (programa == null) {
            throw new IllegalArgumentException("La reserva no pertenece a esta paginación virtual.");
        }
        for (int marco = 0; marco < marcos.length; marco++) {
            MarcoOcupado ocupado = marcos[marco];
            if (ocupado != null && ocupado.asignacion == asignacion) {
                marcos[marco] = null;
                fifo.remove(Integer.valueOf(marco));
            }
        }
        programa.respaldo.liberar();
    }

    @Override
    public boolean hayEspacioDisponible(int cantidad) {
        return cantidad > 0 && almacenamiento.hayEspacioVirtualDisponible(cantidad);
    }

    @Override
    public int getPosicionesLibres() {
        int libres = 0;
        for (MarcoOcupado marco : marcos) {
            if (marco == null) libres++;
        }
        return libres * tamañoPagina;
    }

    @Override
    public void reiniciar() {
        for (AsignacionMemoria asignacion : List.copyOf(programas.keySet())) {
            liberar(asignacion);
        }
        fallos = 0;
        reemplazos = 0;
    }

    public Instruccion leer(AsignacionMemoria asignacion, int posicionLogica) {
        return acceder(asignacion, posicionLogica).instruccion();
    }

    public ResultadoAcceso acceder(AsignacionMemoria asignacion, int posicionLogica) {
        Programa programa = buscarPrograma(asignacion);
        if (posicionLogica < 0 || posicionLogica >= programa.cantidad) {
            throw new IllegalArgumentException("Posición lógica fuera del programa.");
        }
        int pagina = posicionLogica / tamañoPagina;
        int desplazamiento = posicionLogica % tamañoPagina;
        int marco = programa.marcos[pagina];
        boolean fallo = marco < 0;
        boolean reemplazo = false;
        if (fallo) {
            List<Instruccion> instrucciones = List.copyOf(programa.respaldo.leerPagina(pagina));
            int elegido = -1;
            for (int i = 0; i < marcos.length; i++) {
                if (marcos[i] == null) {
                    elegido = i;
                    break;
                }
            }
            if (elegido < 0) {
                elegido = fifo.removeFirst();
                MarcoOcupado victima = marcos[elegido];
                buscarPrograma(victima.asignacion).marcos[victima.pagina] = -1;
                reemplazo = true;
                reemplazos++;
            }
            marcos[elegido] = new MarcoOcupado(asignacion, pagina, instrucciones);
            programa.marcos[pagina] = elegido;
            fifo.addLast(elegido);
            fallos++;
            marco = elegido;
        }
        Instruccion instruccion = marcos[marco].instrucciones.get(desplazamiento);
        int direccionFisica = inicioUsuario + marco * tamañoPagina + desplazamiento;
        return new ResultadoAcceso(instruccion, direccionFisica, fallo, reemplazo);
    }

    public int direccionFisica(AsignacionMemoria asignacion, int posicionLogica) {
        Programa programa = buscarPrograma(asignacion);
        if (posicionLogica < 0 || posicionLogica >= programa.cantidad) {
            throw new IllegalArgumentException("Posición lógica fuera del programa.");
        }
        int marco = programa.marcos[posicionLogica / tamañoPagina];
        if (marco < 0) {
            throw new IllegalStateException("La página no está presente en RAM.");
        }
        return inicioUsuario + marco * tamañoPagina + posicionLogica % tamañoPagina;
    }

    public List<EstadoPagina> paginas(AsignacionMemoria asignacion) {
        Programa programa = buscarPrograma(asignacion);
        List<EstadoPagina> resultado = new ArrayList<>();
        for (int pagina = 0; pagina < programa.marcos.length; pagina++) {
            int marco = programa.marcos[pagina];
            resultado.add(new EstadoPagina(pagina, marco >= 0, marco,
                    programa.respaldo.direccionPagina(pagina)));
        }
        return List.copyOf(resultado);
    }

    public List<EstadoMarco> marcos() {
        List<EstadoMarco> resultado = new ArrayList<>();
        for (int i = 0; i < marcos.length; i++) {
            MarcoOcupado ocupado = marcos[i];
            resultado.add(new EstadoMarco(i, inicioUsuario + i * tamañoPagina, tamañoPagina,
                    ocupado == null ? -1 : 0, ocupado == null ? -1 : ocupado.pagina,
                    ocupado == null ? 0 : ocupado.instrucciones.size()));
        }
        return List.copyOf(resultado);
    }

    public int getFallos() {
        return fallos;
    }

    public int getReemplazos() {
        return reemplazos;
    }

    public int getCapacidad() {
        return marcos.length * tamañoPagina;
    }

    public void evictResident(AsignacionMemoria asignacion) {
        Programa programa = buscarPrograma(asignacion);
        for (int marco = 0; marco < marcos.length; marco++) {
            MarcoOcupado ocupado = marcos[marco];
            if (ocupado != null && ocupado.asignacion == asignacion) {
                marcos[marco] = null;
                fifo.remove(Integer.valueOf(marco));
                programa.marcos[ocupado.pagina] = -1;
            }
        }
    }

    private Programa buscarPrograma(AsignacionMemoria asignacion) {
        Programa programa = programas.get(asignacion);
        if (programa == null) {
            throw new IllegalArgumentException("El proceso no tiene páginas registradas.");
        }
        return programa;
    }
}
