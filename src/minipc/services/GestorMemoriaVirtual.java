package minipc.services;

import java.util.ArrayList;
import java.util.List;
import minipc.model.Almacenamiento;
import minipc.model.BCP;
import minipc.model.EstadoProceso;
import minipc.model.Instruccion;
import minipc.model.Memoria;

/**
 * Administra el intercambio de procesos entre memoria principal
 * y memoria virtual.
 *
 * El BCP permanece en el área del Sistema Operativo de la memoria
 * principal. Las instrucciones del programa son las que se trasladan
 * hacia memoria virtual cuando un proceso es suspendido.
 */
public class GestorMemoriaVirtual {

    private Memoria memoria;
    private Almacenamiento almacenamiento;
    private GestorProcesos gestorProcesos;

    /*
     * Relaciona cada PID suspendido con la dirección donde inicia
     * su programa dentro de memoria virtual.
     */
    // Cada entrada contiene exactamente [PID, dirección inicial virtual].
    private List<List<Integer>> direccionesVirtuales;

    public GestorMemoriaVirtual(Memoria memoria, Almacenamiento almacenamiento, GestorProcesos gestorProcesos) {

        if (memoria == null) {
            throw new IllegalArgumentException("La memoria no puede ser null.");
        }

        if (almacenamiento == null) {
            throw new IllegalArgumentException("El almacenamiento no puede ser null.");
        }

        if (gestorProcesos == null) {
            throw new IllegalArgumentException("El gestor de procesos no puede ser null.");
        }

        this.memoria = memoria;
        this.almacenamiento = almacenamiento;
        this.gestorProcesos = gestorProcesos;
        this.direccionesVirtuales = new ArrayList<>();
    }

    /**
     * Suspende un proceso que actualmente se encuentra en memoria principal.
     *
     * Se permite suspender procesos PREPARADO o BLOQUEADO.
     *
     * @param proceso proceso que será llevado a memoria virtual
     */
    public void suspenderProceso(BCP proceso) {

        if (proceso == null) {
            throw new IllegalArgumentException("El proceso no puede ser null.");
        }

        if (gestorProcesos.buscarProceso(proceso.getPid()) != proceso) {
            throw new IllegalArgumentException("El proceso no pertenece al gestor.");
        }

        if (buscarDireccionVirtual(proceso.getPid()) != -1) {
            throw new IllegalStateException("El proceso ya se encuentra en memoria virtual.");
        }

        EstadoProceso estadoAnterior = proceso.getEstado();

        if (estadoAnterior != EstadoProceso.PREPARADO && estadoAnterior != EstadoProceso.BLOQUEADO) {
            throw new IllegalStateException("Solo se pueden suspender procesos preparados o bloqueados.");
        }

        List<Instruccion> programa = new ArrayList<>();

        for (int i = proceso.getInicioPrograma(); i <= proceso.getFinPrograma(); i++) {
            Instruccion instruccion = memoria.leer(i);

            if (instruccion == null) {
                throw new IllegalStateException("No existe una instrucción en la dirección " + i);
            }

            programa.add(instruccion);
        }

        /*
         * Primero se intenta guardar en memoria virtual.
         * Si no existe espacio, no se libera la memoria principal.
         */
        int inicioVirtual = almacenamiento.guardarProgramaVirtual(programa);
        registrarDireccionVirtual(proceso.getPid(), inicioVirtual);

        memoria.liberarPrograma(proceso.getInicioPrograma(), proceso.getTamañoPrograma());

        if (estadoAnterior == EstadoProceso.PREPARADO) {
            gestorProcesos.getColaPreparados().remove(proceso);
            proceso.setEstado(EstadoProceso.PREPARADO_SUSPENDIDO);
        } else {
            gestorProcesos.getBloqueados().remove(proceso);
            proceso.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
        }
    }

    /**
     * Devuelve un proceso suspendido desde memoria virtual hacia
     * memoria principal.
     *
     * @param proceso proceso que será reactivado
     */
    public void reactivarProceso(BCP proceso) {

        if (proceso == null) {
            throw new IllegalArgumentException("El proceso no puede ser null.");
        }
        if (gestorProcesos.buscarProceso(proceso.getPid()) != proceso) {
            throw new IllegalArgumentException("El proceso no pertenece al gestor.");
        }

        int inicioVirtual = buscarDireccionVirtual(proceso.getPid());

        if (inicioVirtual == -1) {
            throw new IllegalStateException("El proceso no se encuentra en memoria virtual.");
        }

        EstadoProceso estadoAnterior = proceso.getEstado();

        if (estadoAnterior != EstadoProceso.PREPARADO_SUSPENDIDO
                && estadoAnterior != EstadoProceso.BLOQUEADO_SUSPENDIDO) {
            throw new IllegalStateException("El proceso no se encuentra en un estado suspendido.");
        }

        List<Instruccion> programa = almacenamiento.leerProgramaVirtual(inicioVirtual, proceso.getTamañoPrograma());

        /*
         * Si no existe RAM suficiente, cargarPrograma lanza una excepción.
         * El programa permanece en memoria virtual porque todavía no
         * hemos liberado esa zona.
         */
        if (!gestorProcesos.hayEspacioParaPrograma(programa.size())) {
            throw new IllegalStateException("No hay espacio o cupo disponible para otro programa en RAM.");
        }
        int nuevoInicio = memoria.cargarPrograma(programa);

        proceso.reubicarPrograma(nuevoInicio);

        almacenamiento.liberarProgramaVirtual(inicioVirtual, proceso.getTamañoPrograma());
        eliminarDireccionVirtual(proceso.getPid());

        if (estadoAnterior == EstadoProceso.PREPARADO_SUSPENDIDO) {

            proceso.setEstado(EstadoProceso.PREPARADO);

            if (!gestorProcesos.getColaPreparados().contains(proceso)) {
                gestorProcesos.getColaPreparados().add(proceso);
            }

        } else {

            proceso.setEstado(EstadoProceso.BLOQUEADO);

            if (!gestorProcesos.getBloqueados().contains(proceso)) {
                gestorProcesos.getBloqueados().add(proceso);
            }
        }
    }

    /**
     * Guarda directamente un proceso nuevo en memoria virtual.
     *
     * Se utilizará cuando un programa sea admitido pero no exista
     * suficiente espacio para cargarlo en memoria principal.
     */
    public void guardarProcesoNuevo(BCP proceso, List<Instruccion> programa) {

        if (proceso == null) {
            throw new IllegalArgumentException("El proceso no puede ser null.");
        }

        if (programa == null || programa.isEmpty()) {
            throw new IllegalArgumentException("El programa está vacío.");
        }
        if (gestorProcesos.buscarProceso(proceso.getPid()) != proceso
                || proceso.getEstado() != EstadoProceso.NUEVO
                || proceso.getInicioPrograma() != -1
                || programa.size() != proceso.getTamañoPrograma()) {
            throw new IllegalArgumentException("La admisión virtual no coincide con un proceso nuevo del gestor.");
        }

        if (buscarDireccionVirtual(proceso.getPid()) != -1) {
            throw new IllegalStateException("El proceso ya se encuentra en memoria virtual.");
        }

        int inicioVirtual = almacenamiento.guardarProgramaVirtual(programa);

        registrarDireccionVirtual(proceso.getPid(), inicioVirtual);
       
    }

    private void registrarDireccionVirtual(int pid, int inicioVirtual) {
        List<Integer> entrada = new ArrayList<>();
        entrada.add(pid);
        entrada.add(inicioVirtual);
        direccionesVirtuales.add(entrada);
    }

    private int buscarDireccionVirtual(int pid) {
        for (List<Integer> entrada : direccionesVirtuales) {
            if (entrada.get(0) == pid) {
                return entrada.get(1);
            }
        }
        return -1;
    }

    private void eliminarDireccionVirtual(int pid) {
        for (int i = 0; i < direccionesVirtuales.size(); i++) {
            if (direccionesVirtuales.get(i).get(0) == pid) {
                direccionesVirtuales.remove(i);
                return;
            }
        }
    }

    public boolean estaEnMemoriaVirtual(BCP proceso) {

        if (proceso == null) {
            return false;
        }

        return buscarDireccionVirtual(proceso.getPid()) != -1;
    }

    public int getDireccionVirtual(BCP proceso) {

        if (!estaEnMemoriaVirtual(proceso)) {
            return -1;
        }

        return buscarDireccionVirtual(proceso.getPid());
    }

    public void reiniciar() {
        for (List<Integer> entrada : direccionesVirtuales) {
            BCP proceso = gestorProcesos.buscarProceso(entrada.get(0));
            if (proceso != null) {
                almacenamiento.liberarProgramaVirtual(entrada.get(1), proceso.getTamañoPrograma());
            }
        }
        direccionesVirtuales.clear();
    }
}
