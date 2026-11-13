package minipc.config;

import minipc.model.BCP;
import minipc.model.TipoAlgoritmo;

public class Configuracion {

    /*
     * ¿Qué hace? Guarda los topes del sistema para que la interfaz y la
     * fábrica calculen el kernel con la misma regla.
     * ¿Quién lo llama? MainWindow (inputs) y paraInterfaz().
     */
    public static final int RAM_MINIMA = 500;
    public static final int ALMACENAMIENTO_MINIMO = 500;
    public static final int ESPACIO_INSTRUCCIONES_MINIMO = 128;
    public static final int CPUS_MAXIMAS = 5;
    public static final int PROCESOS_POR_CPU = 5;
    public static final int MAX_PROCESOS = CPUS_MAXIMAS * PROCESOS_POR_CPU;
    public static final int RAM_POR_DEFECTO = 500;
    public static final int ALMACENAMIENTO_POR_DEFECTO = 500;
    public static final int INDICE_POR_DEFECTO = 10;
    public static final int VIRTUAL_POR_DEFECTO = 64;

    private final int tamañoMemoria;
    private final int tamañoAlmacenamiento;
    private final int tamañoMemoriaVirtual;
    private final int tamañoIndice;
    private final int inicioUsuario;
    private final int maxProcesosEnRam;
    private final int maxProcesos;
    private final boolean conservarArchivosAlReiniciar;
    private final String algoritmo;

    public Configuracion(int tamañoMemoria, int tamañoAlmacenamiento,
            int tamañoMemoriaVirtual, int tamañoIndice, int inicioUsuario,
            int maxProcesosEnRam, int maxProcesos,
            boolean conservarArchivosAlReiniciar) {
        this(tamañoMemoria, tamañoAlmacenamiento, tamañoMemoriaVirtual, tamañoIndice,
                inicioUsuario, maxProcesosEnRam, maxProcesos, conservarArchivosAlReiniciar, "FCFS");
    }

    public Configuracion(int tamañoMemoria, int tamañoAlmacenamiento,
            int tamañoMemoriaVirtual, int tamañoIndice, int inicioUsuario,
            int maxProcesosEnRam, int maxProcesos,
            boolean conservarArchivosAlReiniciar, String algoritmo) {

        if (algoritmo == null) {

            throw new IllegalArgumentException(
                    "Debe indicar el algoritmo de planificación.");
        }
        this.algoritmo = algoritmo.trim()
                .toUpperCase(java.util.Locale.ROOT);

        TipoAlgoritmo seleccionado;

        try {

            seleccionado = TipoAlgoritmo.valueOf(this.algoritmo);

        } catch (IllegalArgumentException error) {

            throw new IllegalArgumentException(
                    "Algoritmo desconocido: " + algoritmo,
                    error);
        }

        if (seleccionado != TipoAlgoritmo.FCFS
                && seleccionado != TipoAlgoritmo.SPN
                && seleccionado != TipoAlgoritmo.SRT
                && seleccionado != TipoAlgoritmo.HRRN
                && seleccionado != TipoAlgoritmo.RR
                && seleccionado != TipoAlgoritmo.PRIORIDAD
                && seleccionado != TipoAlgoritmo.SELFISH_ROUND_ROBIN) {

            throw new IllegalArgumentException(
                    "Algoritmo aún no implementado: "
                            + seleccionado);
        }
        // this.algoritmo = algoritmo.trim().toUpperCase(java.util.Locale.ROOT);
        if (tamañoMemoria < 128 || tamañoAlmacenamiento <= 0 || tamañoIndice <= 0
                || tamañoMemoriaVirtual < 0
                || (long) tamañoIndice + tamañoMemoriaVirtual >= tamañoAlmacenamiento) {
            throw new IllegalArgumentException("Los tamaños de memoria o almacenamiento no son válidos.");
        }
        if (maxProcesosEnRam < 1 || maxProcesosEnRam > MAX_PROCESOS) {
            throw new IllegalArgumentException(
                    "Solo pueden residir entre 1 y " + MAX_PROCESOS
                            + " procesos simultáneamente en RAM.");
        }
        if (inicioUsuario <= 0 || inicioUsuario >= tamañoMemoria
                || maxProcesos < maxProcesosEnRam
                || maxProcesos > inicioUsuario) {
            throw new IllegalArgumentException(
                    "El kernel debe dejar espacio para el usuario, alojar los BCP y los límites deben ser positivos.");
        }
        this.tamañoMemoria = tamañoMemoria;
        this.tamañoAlmacenamiento = tamañoAlmacenamiento;
        this.tamañoMemoriaVirtual = tamañoMemoriaVirtual;
        this.tamañoIndice = tamañoIndice;
        this.inicioUsuario = inicioUsuario;
        this.maxProcesosEnRam = maxProcesosEnRam;
        this.maxProcesos = maxProcesos;
        this.conservarArchivosAlReiniciar = conservarArchivosAlReiniciar;
    }

    public int getTamañoMemoria() {
        return tamañoMemoria;
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public int getTamañoAlmacenamiento() {
        return tamañoAlmacenamiento;
    }

    public int getTamañoMemoriaVirtual() {
        return tamañoMemoriaVirtual;
    }

    public int getTamañoIndice() {
        return tamañoIndice;
    }

    public int getInicioUsuario() {
        return inicioUsuario;
    }

    public int getMaxProcesosEnRam() {
        return maxProcesosEnRam;
    }

    public int getMaxProcesos() {
        return maxProcesos;
    }

    public boolean isConservarArchivosAlReiniciar() {
        return conservarArchivosAlReiniciar;
    }

    /*
     * ¿Qué hace? Calcula cuántas posiciones de RAM se lleva el kernel para
     * meter hasta 5 BCP por CPU, pero sin comerse la reserva de instrucciones.
     * ¿Quién lo llama? La vista previa de Configuración y paraInterfaz().
     */
    public static int calcularKernel(int ram, int cpus) {

        int bcpDeseados = PROCESOS_POR_CPU * cpus;
        int bcpPorRam = (ram - ESPACIO_INSTRUCCIONES_MINIMO)
                / BCP.getTamañoKernel();
        int bcpReales = Math.min(bcpDeseados, bcpPorRam);

        if (bcpReales < cpus) {
            throw new IllegalArgumentException(
                    "La RAM no alcanza para un BCP por CPU. "
                            + "Con " + cpus + " CPU necesitas al menos "
                            + (ESPACIO_INSTRUCCIONES_MINIMO
                                    + cpus * BCP.getTamañoKernel())
                            + " posiciones.");
        }

        return bcpReales * BCP.getTamañoKernel();
    }

    /*
     * ¿Qué hace? Devuelve cuántos BCP caben realmente en el kernel calculado.
     * ¿Quién lo llama? La interfaz para mostrar la capacidad y validar.
     */
    public static int calcularMaxProcesos(int ram, int cpus) {
        return calcularKernel(ram, cpus) / BCP.getTamañoKernel();
    }

    /*
     * ¿Qué hace? Revisa que RAM, disco y CPU sean números razonables antes de
     * construir la configuración.
     * ¿Quién lo llama? El botón Aplicar de Configuración.
     */
    public static void validarRecursos(int ram, int disco, int cpus) {

        if (ram < RAM_MINIMA) {
            throw new IllegalArgumentException(
                    "La RAM debe ser un entero mayor o igual a "
                            + RAM_MINIMA + ".");
        }

        if (disco < ALMACENAMIENTO_MINIMO) {
            throw new IllegalArgumentException(
                    "El almacenamiento debe ser un entero mayor o igual a "
                            + ALMACENAMIENTO_MINIMO + ".");
        }

        if (cpus < 1 || cpus > CPUS_MAXIMAS) {
            throw new IllegalArgumentException(
                    "Solo se permiten entre 1 y " + CPUS_MAXIMAS + " CPU.");
        }

        // Reutiliza el cálculo para validar el mínimo de un BCP por CPU.
        calcularKernel(ram, cpus);

        if (INDICE_POR_DEFECTO + VIRTUAL_POR_DEFECTO >= disco) {
            throw new IllegalArgumentException(
                    "El almacenamiento debe dejar espacio para archivos: "
                            + "índice (" + INDICE_POR_DEFECTO + ") + memoria "
                            + "virtual (" + VIRTUAL_POR_DEFECTO + ") < disco.");
        }
    }

    /*
     * ¿Qué hace? Arma la configuración que sale de la ventana: RAM y disco
     * escritos por el usuario, kernel calculado y máximo de procesos según
     * las CPU configuradas.
     * ¿Quién lo llama? MainWindow al arrancar y al pulsar Aplicar.
     */
    public static Configuracion paraInterfaz(int ram, int disco, int cpus,
            String algoritmo, boolean conservarArchivosAlReiniciar) {

        validarRecursos(ram, disco, cpus);

        int kernel = calcularKernel(ram, cpus);
        int maxProcesos = kernel / BCP.getTamañoKernel();

        return new Configuracion(
                ram,
                disco,
                VIRTUAL_POR_DEFECTO,
                INDICE_POR_DEFECTO,
                kernel,
                maxProcesos,
                maxProcesos,
                conservarArchivosAlReiniciar,
                algoritmo);
    }
}
