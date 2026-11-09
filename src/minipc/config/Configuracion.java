package minipc.config;

import minipc.model.TipoAlgoritmo;

public class Configuracion {
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
                && seleccionado != TipoAlgoritmo.RR) {

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
        if (maxProcesosEnRam < 1 || maxProcesosEnRam > 5) {
            throw new IllegalArgumentException("Solo pueden residir entre 1 y 5 procesos simultáneamente en RAM.");
        }
        if (inicioUsuario <= 0 || inicioUsuario > tamañoMemoria / 2
                || maxProcesos < maxProcesosEnRam
                || maxProcesos > inicioUsuario) {
            throw new IllegalArgumentException(
                    "El kernel debe ocupar como máximo 50% de RAM, alojar los BCP y los límites deben ser positivos.");
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
}
