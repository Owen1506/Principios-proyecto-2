package minipc.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import minipc.controller.Controlador;
import minipc.model.BCP;
import minipc.model.CPU;
import minipc.model.EstadoProceso;
import minipc.model.TipoAlgoritmo;

/* Conserva una fotografía inmutable de la simulación. La llama MainWindow al refrescar estadísticas. */
public final class InformeEjecucion {
    public record Proceso(int pid, String nombre, int cpu, int llegada, int inicio,
            int fin, int servicio, String estado, String resultado,
            Double estancia, Double proporcion, Integer servicioGarantizado,
            Double parteJusta, Double relacionGarantizada, String huella, String contexto) { }

    public record UsoCPU(int cpu, int ocupado, int ocioso) { }

    public record Hallazgo(String nivel, String mensaje) { }

    public final String algoritmos;
    public final int tiempo;
    public final int ram;
    public final int disco;
    public final List<Proceso> procesos;
    public final List<UsoCPU> cpus;
    public final String carga;
    public final String firmaCarga;
    public final boolean cargaVerificable;
    public final List<Hallazgo> hallazgos;
    public final Double estanciaMedia;
    public final Double proporcionMedia;

    private InformeEjecucion(String algoritmos, int tiempo, int ram, int disco,
            List<Proceso> procesos, List<UsoCPU> cpus, String carga, String firmaCarga,
            boolean cargaVerificable, List<Hallazgo> hallazgos,
            Double estanciaMedia, Double proporcionMedia) {
        this.algoritmos = algoritmos;
        this.tiempo = tiempo;
        this.ram = ram;
        this.disco = disco;
        this.procesos = List.copyOf(procesos);
        this.cpus = List.copyOf(cpus);
        this.carga = carga;
        this.firmaCarga = firmaCarga;
        this.cargaVerificable = cargaVerificable;
        this.hallazgos = List.copyOf(hallazgos);
        this.estanciaMedia = estanciaMedia;
        this.proporcionMedia = proporcionMedia;
    }

    /* Reúne resultados sin conservar referencias mutables. La llama MainWindow.informeActual. */
    public static InformeEjecucion capturar(Controlador c, Map<Integer, String> nombres,
            String algoritmos) {
        List<Proceso> procesos = new ArrayList<>();
        List<UsoCPU> cpus = new ArrayList<>();
        List<String> carga = new ArrayList<>();
        List<String> firmas = new ArrayList<>();
        boolean verificable = true;
        double sumaEstancia = 0, sumaProporcion = 0;
        int nEstancia = 0, nProporcion = 0;
        boolean garantizado = algoritmos.contains("GUARANTEED_SCHEDULING")
                && c.getAlgoritmoSeleccionado() == TipoAlgoritmo.GUARANTEED_SCHEDULING;

        for (BCP p : c.getGestorProcesos().getListaTrabajos()) {
            String nombre = nombres.getOrDefault(p.getPid(), "PID " + p.getPid());
            int fin = p.getTiempoFinalSimulado();
            boolean finalizado = p.getEstado() == EstadoProceso.FINALIZADO && fin >= 0;
            boolean correcto = finalizado && p.getMotivoError().isEmpty();
            Double estancia = finalizado ? (double) (fin - p.getTiempoLlegada()) : null;
            Double proporcion = estancia != null && p.getTiempoCPU() > 0
                    ? estancia / p.getTiempoCPU() : null;
            if (correcto && estancia != null) { sumaEstancia += estancia; nEstancia++; }
            if (correcto && proporcion != null) { sumaProporcion += proporcion; nProporcion++; }
            String resultado = !p.getMotivoError().isEmpty() ? "Error: " + p.getMotivoError()
                    : finalizado ? "Normal" : "Pendiente";
            String huella = c.getHuellaPrograma(p.getPid());
            Integer prioridadInicial = c.getPrioridadInicialInforme(p.getPid());
            if (huella == null || prioridadInicial == null) verificable = false;
            String contexto = "PC: " + p.getPC() + "\nIR: " + p.getIR()
                    + "\nAC: " + p.getAC() + "\nAX: " + p.getAX() + "\nBX: " + p.getBX()
                    + "\nCX: " + p.getCX() + "\nDX: " + p.getDX() + "\nAH: " + p.getAH()
                    + "\nAL: " + p.getAL() + "\nZF: " + p.isZeroFlag() + "\nPila: " + p.getPila()
                    + "\nHora real de inicio: " + p.getHoraInicio()
                    + "\nHora real de fin: " + p.getHoraFinal()
                    + "\nDuración real: " + (finalizado
                            ? String.format(Locale.ROOT, "%.3f segundos", p.getTiempoTotalSegundos()) : "Pendiente");
            procesos.add(new Proceso(p.getPid(), nombre, p.getCpuAsignada(), p.getTiempoLlegada(),
                    p.getTiempoInicioSimulado(), fin, p.getTiempoCPU(), p.getEstado().toString(),
                    resultado, estancia, proporcion,
                    garantizado ? c.getServicioGarantizado(p.getPid()) : null,
                    garantizado ? c.getParteJustaGarantizada(p.getPid()) : null,
                    garantizado ? c.getProporcionGarantizada(p.getPid()) : null, huella, contexto));
            carga.add(nombre + "@" + p.getTiempoLlegada());
            firmas.add(nombre + "@" + p.getTiempoLlegada() + "@" + (huella == null ? "?" : huella)
                    + "@" + (prioridadInicial == null ? "?" : prioridadInicial));
        }
        for (int i = 1; i <= c.getCantidadCPU(); i++) {
            CPU cpu = c.getCPU(i);
            cpus.add(new UsoCPU(i, cpu.getTiempoOcupado(), cpu.getTiempoOcioso()));
        }
        Collections.sort(carga);
        Collections.sort(firmas);
        Double promedioEstancia = nEstancia == 0 ? null : sumaEstancia / nEstancia;
        Double promedioProporcion = nProporcion == 0 ? null : sumaProporcion / nProporcion;
        List<Hallazgo> hallazgos = revisar(procesos, cpus, c.getTiempoSimulado(),
                algoritmos, verificable, promedioEstancia, promedioProporcion);
        return new InformeEjecucion(algoritmos, c.getTiempoSimulado(), c.getSizeMemoria(),
                c.getAlmacenamiento().getSize(), procesos, cpus, String.join(" | ", carga),
                String.join(" | ", firmas), verificable, hallazgos,
                promedioEstancia, promedioProporcion);
    }

    /* Comprueba la coherencia de los datos sin corregir fórmulas ni valores. Lo llama capturar. */
    private static List<Hallazgo> revisar(List<Proceso> procesos, List<UsoCPU> cpus,
            int tiempo, String algoritmos, boolean cargaVerificable,
            Double estanciaMedia, Double proporcionMedia) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        if (procesos.isEmpty()) {
            hallazgos.add(new Hallazgo("Aviso", "No hay procesos en esta ejecución."));
            return hallazgos;
        }
        if (tiempo <= 0) {
            hallazgos.add(new Hallazgo("Aviso", "La simulación no registró ticks."));
        }

        int ocupadoTotal = 0;
        for (UsoCPU cpu : cpus) {
            ocupadoTotal += cpu.ocupado();
            if (cpu.ocupado() < 0 || cpu.ocioso() < 0) {
                hallazgos.add(new Hallazgo("Inconsistencia", "CPU " + cpu.cpu()
                        + " tiene tiempos negativos (ocupada " + cpu.ocupado()
                        + ", ociosa " + cpu.ocioso() + ")."));
            }
            if (cpu.ocupado() + cpu.ocioso() != tiempo) {
                hallazgos.add(new Hallazgo("Aviso", "CPU " + cpu.cpu() + " registra "
                        + (cpu.ocupado() + cpu.ocioso()) + " ticks, pero el reloj global es "
                        + tiempo + ". Puede ocurrir si la CPU se agregó después de iniciar."));
            }
        }

        int servicioTotal = 0;
        long normales = 0, normalesConServicio = 0;
        boolean garantiaUsada = algoritmos.contains("GUARANTEED_SCHEDULING");
        boolean garantiaDisponible = false;
        for (Proceso p : procesos) {
            servicioTotal += p.servicio();
            String quien = "PID " + p.pid() + " (" + p.nombre() + ")";
            if (p.servicio() < 0) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien + " tiene tiempo de CPU negativo."));
            }
            if (p.llegada() < 0) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien + " tiene llegada negativa."));
            }
            if (p.llegada() > tiempo) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien + " llega después del tiempo simulado."));
            }
            if (p.inicio() < -1 || p.fin() < -1) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien + " tiene marcas de inicio o fin inválidas."));
            }
            if (p.inicio() >= 0 && p.inicio() < p.llegada()) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien + " inicia antes de su llegada."));
            }
            if (p.fin() >= 0 && p.inicio() >= 0 && p.fin() < p.inicio()) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien + " finaliza antes de iniciar."));
            }
            if (p.fin() >= 0 && p.fin() < p.llegada()) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien + " finaliza antes de su llegada."));
            }
            if (p.fin() > tiempo) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien + " finaliza después del tiempo simulado."));
            }
            if (p.cpu() > cpus.size()) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien + " está asignado a una CPU inexistente."));
            }
            if (p.cpu() < 1 && p.servicio() > 0) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien + " consumió CPU sin una CPU asignada."));
            }
            if (p.estancia() != null && p.estancia() < 0) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien + " tiene retorno negativo."));
            }
            if (p.estancia() != null && p.servicio() >= 0 && p.estancia() < p.servicio()) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien
                        + " tiene retorno menor que su tiempo de CPU."));
            }
            if (p.proporcion() != null && p.proporcion() < 1 - 1e-9) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien
                        + " tiene retorno/servicio menor que 1."));
            }
            if (p.servicio() == 0 && "Normal".equals(p.resultado())) {
                hallazgos.add(new Hallazgo("Aviso", quien
                        + " finalizó normalmente sin ticks de CPU registrados."));
            }
            if ("Normal".equals(p.resultado())) {
                normales++;
                if (p.servicio() > 0) normalesConServicio++;
            }
            if (!"Pendiente".equals(p.resultado()) && p.fin() < 0) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien
                        + " terminó sin una marca de finalización."));
            }
            if ("Pendiente".equals(p.resultado()) && p.fin() >= 0) {
                hallazgos.add(new Hallazgo("Inconsistencia", quien
                        + " figura pendiente pero tiene finalización registrada."));
            }
            if (p.servicioGarantizado() != null) {
                garantiaDisponible = true;
                if (p.servicioGarantizado() < 0) {
                    hallazgos.add(new Hallazgo("Inconsistencia", quien
                            + " tiene servicio garantizado negativo."));
                }
                if (p.servicioGarantizado() > p.servicio()) {
                    hallazgos.add(new Hallazgo("Inconsistencia", quien
                            + " recibió más servicio garantizado que tiempo real de CPU."));
                }
                if (p.parteJusta() == null || p.parteJusta() < 0) {
                    hallazgos.add(new Hallazgo("Inconsistencia", quien
                            + " tiene una parte justa inválida."));
                }
                if (p.relacionGarantizada() == null || p.relacionGarantizada() < 0) {
                    hallazgos.add(new Hallazgo("Inconsistencia", quien
                            + " tiene una relación de justicia inválida."));
                }
            }
        }

        if (servicioTotal != ocupadoTotal) {
            hallazgos.add(new Hallazgo("Aviso", "La suma del tiempo de CPU de los procesos ("
                    + servicioTotal + " ticks) no coincide con la ocupación registrada ("
                    + ocupadoTotal + " ticks). Puede deberse a un cambio de configuración o a "
                    + "estadísticas reiniciadas durante la ejecución."));
        }
        if (normales > 0 && estanciaMedia == null) {
            hallazgos.add(new Hallazgo("Inconsistencia",
                    "Hay finalizaciones normales, pero no se pudo calcular el retorno promedio."));
        }
        if (normales == 0 && estanciaMedia != null) {
            hallazgos.add(new Hallazgo("Inconsistencia",
                    "Se calculó un retorno promedio sin finalizaciones normales."));
        }
        if (normalesConServicio > 0 && proporcionMedia == null) {
            hallazgos.add(new Hallazgo("Inconsistencia",
                    "Hay procesos normales con servicio, pero no se pudo calcular "
                            + "el promedio de retorno/servicio."));
        }
        if (garantiaUsada && !garantiaDisponible) {
            hallazgos.add(new Hallazgo("Aviso",
                    "La política incluyó GUARANTEED_SCHEDULING, pero el informe no conserva "
                            + "las métricas de reparto garantizado. Pudo cambiar la política antes "
                            + "de capturar la fotografía."));
        }
        if (!cargaVerificable) {
            hallazgos.add(new Hallazgo("Aviso",
                    "No se pudo verificar el contenido de los programas. La comparación se limita "
                            + "a nombres, llegadas y valores iniciales."));
        }
        return hallazgos;
    }

    /* Presenta valores pendientes sin dividir por cero. La usa PanelEstadisticas. */
    public static String numero(Double n) {
        return n == null ? "—" : String.format(Locale.ROOT, "%.2f", n);
    }
}
