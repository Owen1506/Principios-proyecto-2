package minipc.services;

import minipc.model.BCP;
import minipc.model.CPU;
import minipc.model.Instruccion;

import java.util.List;
import java.util.ArrayList;

/**
 * Administra las interrupciones generadas durante la ejecución
 * de los procesos de la Mini PC.
 *
 * Interrupciones soportadas:
 *
 * INT 10H -> salida en pantalla.
 * INT 20H -> finalización del proceso.
 * INT 21H -> manejo de archivos.
 */
public class GestorInterrupciones {

    private CPU cpu;
    private GestorProcesos gestorProcesos;
    private Despachador despachador;
    private SistemaArchivos sistemaArchivos;

    private String ultimaSalida;
    private final List<String> salidas = new ArrayList<>();

    public GestorInterrupciones(CPU cpu, GestorProcesos gestorProcesos, Despachador despachador, SistemaArchivos sistemaArchivos) {

        if (cpu == null) {
            throw new IllegalArgumentException("La CPU no puede ser null.");
        }

        if (gestorProcesos == null) {
            throw new IllegalArgumentException("El gestor de procesos no puede ser null.");
        }

        if (despachador == null) {
            throw new IllegalArgumentException("El despachador no puede ser null.");
        }

        if (sistemaArchivos == null) {
            throw new IllegalArgumentException("El sistema de archivos no puede ser null.");
        }

        this.cpu = cpu;
        this.gestorProcesos = gestorProcesos;
        this.despachador = despachador;
        this.sistemaArchivos = sistemaArchivos;

        this.ultimaSalida = "";
    }

    /**
     * Atiende una interrupción.
     *
     * @param instruccion instrucción INT que será atendida
     * @return true si la interrupción modificó el flujo normal del PC
     */
    public boolean atender(Instruccion instruccion) {

        String servicio = instruccion.getServicio();

        switch (servicio) {

            case "10H":
                atenderINT10();
                return false;

            case "20H":
                atenderINT20();
                return true;

            case "21H":
                atenderINT21();
                return false;

            default:
                throw new IllegalArgumentException("Interrupción no soportada: " + servicio);
        }
    }

    /**
     * INT 10H.
     *
     * Copia el contenido de DX como la última salida generada
     * por la Mini PC.
     */
    private void atenderINT10() {
        ultimaSalida = cpu.getRegistros().getDX();
        salidas.add(ultimaSalida);
    }

    /**
     * INT 20H.
     *
     * Finaliza el proceso actual y entrega la CPU al siguiente
     * proceso preparado, si existe.
     */
    private void atenderINT20() {

        if (gestorProcesos.getProcesoActual() == null) {
            throw new IllegalStateException("No existe un proceso en ejecución.");
        }

        despachador.guardarContextoActual();
        gestorProcesos.finalizarProcesoActual();
        despachador.despacharSiguiente();
    }

    /**
     * INT 21H.
     *
     * Ejecuta el servicio de archivos indicado en AH.
     */
    private void atenderINT21() {

        BCP proceso = gestorProcesos.getProcesoActual();

        if (proceso == null) {
            throw new IllegalStateException("No existe un proceso en ejecución.");
        }

        sistemaArchivos.ejecutarServicio(cpu, proceso);
    }

    public String getUltimaSalida() {
        return ultimaSalida;
    }

    public void reiniciar() {
        salidas.clear();
        ultimaSalida = "";
    }

    public List<String> getSalidas() {
        return new ArrayList<>(salidas);
    }
}
