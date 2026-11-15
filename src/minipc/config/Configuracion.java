package minipc.config;

import minipc.model.BCP;
import minipc.model.ConfiguracionDinamica;
import minipc.model.ConfiguracionFijaIgual;
import minipc.model.ConfiguracionFijaDiferente;
import minipc.model.ConfiguracionMemoria;
import minipc.model.ConfiguracionPaginacion;
import minipc.model.MetodoAdministracionMemoria;
import minipc.model.TipoAlgoritmo;
import minipc.model.TipoEstrategiaMemoria;

public class Configuracion {

    /*
     * ¿Qué hace? Guarda los topes del sistema para que la interfaz y la
     * fábrica calculen el kernel con la misma regla.
     * ¿Quién lo llama? MainWindow (inputs) y paraInterfaz().
     */
    public static final int RAM_MINIMA = 200;
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
    private final MetodoAdministracionMemoria metodoMemoria;
    private final ConfiguracionMemoria configuracionMemoria;
    private final String estrategiaMemoria;

    public Configuracion(int tamañoMemoria, int tamañoAlmacenamiento,
            int tamañoMemoriaVirtual, int tamañoIndice, int inicioUsuario,
            int maxProcesosEnRam, int maxProcesos,
            boolean conservarArchivosAlReiniciar) {
        this(tamañoMemoria, tamañoAlmacenamiento, tamañoMemoriaVirtual, tamañoIndice,
                inicioUsuario, maxProcesosEnRam, maxProcesos,
                conservarArchivosAlReiniciar, "FCFS", "DINAMICAS", "FIRST_FIT");
    }

    public Configuracion(int tamañoMemoria, int tamañoAlmacenamiento,
            int tamañoMemoriaVirtual, int tamañoIndice, int inicioUsuario,
            int maxProcesosEnRam, int maxProcesos,
            boolean conservarArchivosAlReiniciar, String algoritmo) {
        this(tamañoMemoria, tamañoAlmacenamiento, tamañoMemoriaVirtual, tamañoIndice,
                inicioUsuario, maxProcesosEnRam, maxProcesos,
                conservarArchivosAlReiniciar, algoritmo, "DINAMICAS", "FIRST_FIT");
    }

    public Configuracion(int tamañoMemoria, int tamañoAlmacenamiento,
            int tamañoMemoriaVirtual, int tamañoIndice, int inicioUsuario,
            int maxProcesosEnRam, int maxProcesos,
            boolean conservarArchivosAlReiniciar, String algoritmo,
            String estrategiaMemoria) {
        this(tamañoMemoria, tamañoAlmacenamiento, tamañoMemoriaVirtual, tamañoIndice,
                inicioUsuario, maxProcesosEnRam, maxProcesos,
                conservarArchivosAlReiniciar, algoritmo, "DINAMICAS", estrategiaMemoria);
    }

    public Configuracion(int tamañoMemoria, int tamañoAlmacenamiento,
            int tamañoMemoriaVirtual, int tamañoIndice, int inicioUsuario,
            int maxProcesosEnRam, int maxProcesos,
            boolean conservarArchivosAlReiniciar, String algoritmo,
            String metodoMemoria, String estrategiaMemoria) {
        this(tamañoMemoria, tamañoAlmacenamiento, tamañoMemoriaVirtual, tamañoIndice,
                inicioUsuario, maxProcesosEnRam, maxProcesos, conservarArchivosAlReiniciar,
                algoritmo, metodoMemoria, estrategiaMemoria, null, null);
    }

    public Configuracion(int tamañoMemoria, int tamañoAlmacenamiento,
            int tamañoMemoriaVirtual, int tamañoIndice, int inicioUsuario,
            int maxProcesosEnRam, int maxProcesos,
            boolean conservarArchivosAlReiniciar, String algoritmo,
            String metodoMemoria, String estrategiaMemoria,
            String tamañoParticion, String tamañosParticiones) {
        this(tamañoMemoria, tamañoAlmacenamiento, tamañoMemoriaVirtual, tamañoIndice,
                inicioUsuario, maxProcesosEnRam, maxProcesos, conservarArchivosAlReiniciar,
                algoritmo, metodoMemoria, estrategiaMemoria, tamañoParticion, tamañosParticiones,
                Integer.toString(ConfiguracionPaginacion.TAMAÑO_POR_DEFECTO), "false");
    }

    public Configuracion(int tamañoMemoria, int tamañoAlmacenamiento,
            int tamañoMemoriaVirtual, int tamañoIndice, int inicioUsuario,
            int maxProcesosEnRam, int maxProcesos,
            boolean conservarArchivosAlReiniciar, String algoritmo,
            String metodoMemoria, String estrategiaMemoria,
            String tamañoParticion, String tamañosParticiones, String tamañoPagina,
            String paginacionVirtual) {

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
                && seleccionado != TipoAlgoritmo.SELFISH_ROUND_ROBIN
                && seleccionado != TipoAlgoritmo.LOTTERY
                && seleccionado != TipoAlgoritmo.MULTILEVEL_FEEDBACK_QUEUE
                && seleccionado != TipoAlgoritmo.GUARANTEED_SCHEDULING) {

            throw new IllegalArgumentException(
                    "Algoritmo aún no implementado: "
                            + seleccionado);
        }

        /*
         * Normaliza el método de memoria. Si la clave no viene en el archivo
         * de configuración, se asume DINAMICAS para que los archivos
         * anteriores a esta fase sigan funcionando.
         */
        String metodoTexto = metodoMemoria == null
                ? ""
                : metodoMemoria.trim().toUpperCase(java.util.Locale.ROOT);

        if (metodoTexto.isEmpty()) {
            this.metodoMemoria = MetodoAdministracionMemoria.DINAMICAS;
        } else {
            try {
                this.metodoMemoria = MetodoAdministracionMemoria.valueOf(metodoTexto);
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException(
                        "Método de memoria desconocido: " + metodoMemoria,
                        error);
            }
        }

        if (!this.metodoMemoria.estaImplementado()) {
            throw new IllegalArgumentException(
                    "Método de memoria aún no implementado: "
                            + this.metodoMemoria);
        }

        // La estrategia de ajuste se conserva por compatibilidad; solo la usa DINAMICAS.
        if (estrategiaMemoria == null) {
            throw new IllegalArgumentException(
                    "Debe indicar la estrategia de asignación de memoria.");
        }
        this.estrategiaMemoria = estrategiaMemoria.trim()
                .toUpperCase(java.util.Locale.ROOT);

        TipoEstrategiaMemoria estrategiaSeleccionada;

        try {
            estrategiaSeleccionada = TipoEstrategiaMemoria.valueOf(this.estrategiaMemoria);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "Estrategia de memoria desconocida: " + estrategiaMemoria,
                    error);
        }

        if (!estrategiaSeleccionada.estaImplementada()) {
            throw new IllegalArgumentException(
                    "Estrategia de memoria aún no implementada: "
                            + estrategiaSeleccionada);
        }

        if (this.metodoMemoria == MetodoAdministracionMemoria.PAGINACION) {
            try {
                boolean virtual = paginacionVirtual != null
                        && paginacionVirtual.trim().equalsIgnoreCase("true");
                this.configuracionMemoria = new ConfiguracionPaginacion(
                        Integer.parseInt(tamañoPagina.trim()), virtual);
            } catch (NullPointerException | NumberFormatException error) {
                throw new IllegalArgumentException("Indique el tamaño entero de la página.", error);
            }
        } else if (this.metodoMemoria == MetodoAdministracionMemoria.FIJAS_IGUALES) {
            try {
                this.configuracionMemoria = new ConfiguracionFijaIgual(Integer.parseInt(tamañoParticion.trim()));
            } catch (NullPointerException | NumberFormatException error) {
                throw new IllegalArgumentException("Indique el tamaño entero de la partición.", error);
            }
        } else if (this.metodoMemoria == MetodoAdministracionMemoria.FIJAS_DIFERENTES) {
            if (tamañosParticiones == null || tamañosParticiones.isBlank()) {
                throw new IllegalArgumentException("Indique los tamaños de las particiones.");
            }
            java.util.List<Integer> tamaños = new java.util.ArrayList<>();
            for (String texto : tamañosParticiones.split(",", -1)) {
                try {
                    tamaños.add(Integer.parseInt(texto.trim()));
                } catch (NumberFormatException error) {
                    throw new IllegalArgumentException("Los tamaños deben ser enteros separados por comas.", error);
                }
            }
            this.configuracionMemoria = new ConfiguracionFijaDiferente(tamaños);
        } else {
            this.configuracionMemoria = new ConfiguracionDinamica(estrategiaSeleccionada);
        }

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
        int areaUsuario = tamañoMemoria - inicioUsuario;
        if (configuracionMemoria instanceof ConfiguracionPaginacion paginacion
                && paginacion.tamañoPagina() > areaUsuario) {
            throw new IllegalArgumentException("Debe caber al menos un marco en el área de usuario.");
        }
        if (configuracionMemoria instanceof ConfiguracionFijaIgual fija
                && fija.tamañoParticion() > areaUsuario) {
            throw new IllegalArgumentException("No cabe ninguna partición en el área de usuario.");
        }
        if (configuracionMemoria instanceof ConfiguracionFijaDiferente fija
                && fija.tamaños().stream().mapToLong(Integer::longValue).sum() > areaUsuario) {
            throw new IllegalArgumentException("Las particiones exceden el área de usuario.");
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

    public MetodoAdministracionMemoria getMetodoMemoria() {
        return metodoMemoria;
    }

    public ConfiguracionMemoria getConfiguracionMemoria() {
        return configuracionMemoria;
    }

    /*
     * Devuelve el nombre persistido de la estrategia de memoria.
     * Lo usan la interfaz y ConfigLoader al leer la configuración.
     */
    public String getEstrategiaMemoria() {
        return estrategiaMemoria;
    }

    /*
     * Devuelve la estrategia ya convertida a enum para la fábrica de Memoria.
     * Lo usa Controlador al inicializar y al cambiar la asignación de memoria.
     */
    public TipoEstrategiaMemoria getTipoEstrategiaMemoria() {
        return TipoEstrategiaMemoria.valueOf(estrategiaMemoria);
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
        return paraInterfaz(ram, disco, cpus, algoritmo, "FIRST_FIT",
                conservarArchivosAlReiniciar);
    }

    /*
     * Arma la configuración con la estrategia de memoria elegida en la ventana.
     * La usa MainWindow al pulsar Aplicar y al arrancar la sesión por defecto.
     */
    public static Configuracion paraInterfaz(int ram, int disco, int cpus,
            String algoritmo, String estrategiaMemoria,
            boolean conservarArchivosAlReiniciar) {

        return paraInterfaz(ram, disco, cpus, algoritmo, "DINAMICAS",
                estrategiaMemoria, conservarArchivosAlReiniciar);
    }

    public static Configuracion paraInterfaz(int ram, int disco, int cpus,
            String algoritmo, String metodoMemoria, String estrategiaMemoria,
            boolean conservarArchivosAlReiniciar) {
        return paraInterfaz(ram, disco, cpus, algoritmo, metodoMemoria, estrategiaMemoria,
                null, null, Integer.toString(ConfiguracionPaginacion.TAMAÑO_POR_DEFECTO),
                "false", conservarArchivosAlReiniciar);
    }

    public static Configuracion paraInterfaz(int ram, int disco, int cpus,
            String algoritmo, String metodoMemoria, String estrategiaMemoria,
            String tamañoParticion, String tamañosParticiones,
            boolean conservarArchivosAlReiniciar) {
        return paraInterfaz(ram, disco, cpus, algoritmo, metodoMemoria, estrategiaMemoria,
                tamañoParticion, tamañosParticiones,
                Integer.toString(ConfiguracionPaginacion.TAMAÑO_POR_DEFECTO), "false",
                conservarArchivosAlReiniciar);
    }

    public static Configuracion paraInterfaz(int ram, int disco, int cpus,
            String algoritmo, String metodoMemoria, String estrategiaMemoria,
            String tamañoParticion, String tamañosParticiones, String tamañoPagina,
            String paginacionVirtual, boolean conservarArchivosAlReiniciar) {
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
                algoritmo,
                metodoMemoria,
                estrategiaMemoria,
                tamañoParticion,
                tamañosParticiones,
                tamañoPagina,
                paginacionVirtual);
    }
}
