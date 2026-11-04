package minipc.model;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa el Bloque de Control de Proceso (BCP) de un programa
 * cargado en la Mini PC.
 *
 * El BCP almacena información necesaria para identificar y controlar
 * un proceso, incluyendo:
 *
 * - PID del proceso.
 * - Estado actual del proceso.
 * - Contexto de ejecución de la CPU.
 * - Valores de los registros generales.
 * - Pila del proceso.
 * - Ubicación y tamaño del programa en memoria.
 * - Prioridad.
 * - Información de tiempo.
 * - Archivos abiertos.
 * - Dirección del BCP y enlace al siguiente BCP.
 *
 * También permite guardar el contexto actual de la CPU y restaurarlo
 * posteriormente para realizar cambios de contexto entre procesos.
 */
public class BCP {
    private int pid;
    private EstadoProceso estado;

    // Contexto de CPU
    private int pc;
    private String ir;
    private int ac;

    private int ax;
    private int bx;
    private int cx;

    private String dx;
    private String ah;
    private String al;

    // Información del programa en memoria
    private int inicioPrograma;
    private int finPrograma;
    private int tamañoPrograma;

    // Información del proceso
    private int prioridad;
    private PilaProceso pila;

    // Ubicación del BCP dentro del área del sistema
    private int direccionBCP;
    private int direccionSiguienteBCP;

    // Información contable
    private LocalDateTime horaInicio;
    private LocalDateTime horaFinal;
    private int tiempoCPU;

    // Datos de planificación pendientes de utilizar (segundos simulados).
    private int rafaga;
    private int tiempoLlegada;
    private String motivoError = "";

    public String getMotivoError() { return motivoError; }

    public void setMotivoError(String motivoError) { this.motivoError = motivoError; }

    // Archivos abiertos por el proceso
    private List<String> archivosAbiertos;

    private boolean zeroFlag;

    /** Una posición de kernel por atributo normal del BCP. */
    public static int getTamañoKernel() {
        return 24;
    }

    /** Lee el valor actual, sin mantener una copia desactualizada del contexto. */
    public String obtenerAtributoKernel(int desplazamiento) {
        String atributo = switch (desplazamiento) {
            case 0 -> "PID = " + pid;
            case 1 -> "ESTADO = " + estado;
            case 2 -> "PC = " + pc;
            case 3 -> "IR = " + ir;
            case 4 -> "AC = " + ac;
            case 5 -> "AX = " + ax;
            case 6 -> "BX = " + bx;
            case 7 -> "CX = " + cx;
            case 8 -> "DX = " + dx;
            case 9 -> "AH = " + ah;
            case 10 -> "AL = " + al;
            case 11 -> "INICIO_PROGRAMA = " + inicioPrograma;
            case 12 -> "FIN_PROGRAMA = " + finPrograma;
            case 13 -> "TAMAÑO_PROGRAMA = " + tamañoPrograma;
            case 14 -> "PRIORIDAD = " + prioridad;
            case 15 -> "PILA = " + pila;
            case 16 -> "DIRECCION_BCP = " + direccionBCP;
            case 17 -> "DIRECCION_SIGUIENTE_BCP = " + direccionSiguienteBCP;
            case 18 -> "HORA_INICIO = " + (horaInicio == null ? "Pendiente" : horaInicio);
            case 19 -> "HORA_FINAL = " + (horaFinal == null ? "Pendiente" : horaFinal);
            case 20 -> "TIEMPO_CPU = " + tiempoCPU;
            case 21 -> "ARCHIVOS_ABIERTOS = " + archivosAbiertos;
            case 22 -> "ZERO_FLAG = " + zeroFlag;
            case 23 -> "MOTIVO_ERROR = " + motivoError;
            default -> throw new IllegalArgumentException("Posición de atributo del BCP inválida: " + desplazamiento);
        };
        return "PID " + pid + " | " + atributo;
    }


    /**
     * Crea un nuevo Bloque de Control de Proceso.
     *
     * Si inicioPrograma es -1, el proceso todavía no posee una
     * ubicación en memoria principal. Esto permite crear procesos
     * que inicialmente deban permanecer en memoria virtual.
     *
     * @param pid identificador único del proceso
     * @param inicioPrograma dirección inicial del programa en memoria,
     *                       o -1 si aún no está cargado en RAM
     * @param tamañoPrograma cantidad de posiciones de memoria ocupadas
     */
    public BCP(int pid, int inicioPrograma, int tamañoPrograma) {

        if (pid <= 0) {
            throw new IllegalArgumentException("El PID debe ser mayor que cero.");
        }

        if (inicioPrograma < -1) {
            throw new IllegalArgumentException("La dirección inicial del programa no es válida.");
        }

        if (tamañoPrograma <= 0) {
            throw new IllegalArgumentException("El tamaño del programa debe ser mayor que cero.");
        }

        this.pid = pid;
        this.estado = EstadoProceso.NUEVO;

        this.inicioPrograma = inicioPrograma;
        this.tamañoPrograma = tamañoPrograma;

        if (inicioPrograma == -1) {
            this.pc = -1;
            this.finPrograma = -1;
        } else {
            this.pc = inicioPrograma;
            this.finPrograma = inicioPrograma + tamañoPrograma - 1;
        }

        this.ir = "";
        this.ac = 0;

        this.ax = 0;
        this.bx = 0;
        this.cx = 0;
        this.dx = "0";
        this.ah = "";
        this.al = "";

        this.prioridad = 0;
        this.pila = new PilaProceso();

        this.direccionBCP = -1;
        this.direccionSiguienteBCP = -1;

        this.horaInicio = null;
        this.horaFinal = null;
        this.tiempoCPU = 0;

        this.archivosAbiertos = new ArrayList<>();

        this.zeroFlag = false;
    }

    public void guardarContexto(CPU cpu) {
        this.pc = cpu.getPC();
        this.ir = cpu.getIR();
        this.ac = cpu.getAC();
        this.zeroFlag = cpu.isZeroFlag();

        this.ax = cpu.getRegistros().getAX();
        this.bx = cpu.getRegistros().getBX();
        this.cx = cpu.getRegistros().getCX();

        this.dx = cpu.getRegistros().getDX();
        this.ah = cpu.getRegistros().getAH();
        this.al = cpu.getRegistros().getAL();
    }

    public void restaurarContexto(CPU cpu) {
        cpu.setPC(this.pc);
        cpu.setIR(this.ir);
        cpu.setAC(this.ac);
        cpu.setZeroFlag(this.zeroFlag);

        cpu.getRegistros().setAX(this.ax);
        cpu.getRegistros().setBX(this.bx);
        cpu.getRegistros().setCX(this.cx);

        cpu.getRegistros().setDX(this.dx);
        cpu.getRegistros().setAH(this.ah);
        cpu.getRegistros().setAL(this.al);
    }

    public int getPid() {
        return pid;
    }

    public EstadoProceso getEstado() {
        return estado;
    }

    public void setEstado(EstadoProceso estado) {
        this.estado = estado;
    }

    public int getPC() {
        return pc;
    }

    public String getIR() {
        return ir;
    }

    public int getAC() {
        return ac;
    }

    public int getAX() {
        return ax;
    }

    public int getBX() {
        return bx;
    }

    public int getCX() {
        return cx;
    }

    public String getDX() {
        return dx;
    }

    public void setDX(String dx) {
        this.dx = dx;
    }

    public String getAH() {
        return ah;
    }

    public String getAL() {
        return al;
    }

    public boolean isZeroFlag() {
        return zeroFlag;
    }

    /**
     * Reubica el programa en memoria principal conservando el avance
     * que ya llevaba el PC.
     *
     * Si el proceso todavía no había sido cargado en RAM
     * (inicioPrograma = -1), comenzará desde la primera instrucción.
     */
    public void reubicarPrograma(int nuevoInicio) {

        if (nuevoInicio < 0) {
            throw new IllegalArgumentException("La nueva dirección inicial no es válida.");
        }

        int desplazamientoPC = 0;

        if (inicioPrograma >= 0 && pc >= inicioPrograma) {
            desplazamientoPC = pc - inicioPrograma;
        }

        this.inicioPrograma = nuevoInicio;
        this.finPrograma = nuevoInicio + tamañoPrograma - 1;
        this.pc = nuevoInicio + desplazamientoPC;
    }

    public int getInicioPrograma() {
        return inicioPrograma;
    }

    public int getFinPrograma() {
        return finPrograma;
    }

    public int getTamañoPrograma() {
        return tamañoPrograma;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }

    public PilaProceso getPila() {
        return pila;
    }

    public int getDireccionBCP() {
        return direccionBCP;
    }

    public void setDireccionBCP(int direccionBCP) {
        this.direccionBCP = direccionBCP;
    }

    public int getDireccionSiguienteBCP() {
        return direccionSiguienteBCP;
    }

    public void setDireccionSiguienteBCP(int direccionSiguienteBCP) {
        this.direccionSiguienteBCP = direccionSiguienteBCP;
    }

    public LocalDateTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalDateTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalDateTime getHoraFinal() {
        return horaFinal;
    }

    public void setHoraFinal(LocalDateTime horaFinal) {
        this.horaFinal = horaFinal;
    }

    public int getTiempoCPU() {
        return tiempoCPU;
    }

    /** Tiempo real desde la primera asignación de CPU hasta la finalización. */
    public Duration getTiempoTotal() {
        if (horaInicio == null || horaFinal == null) {
            return Duration.ZERO;
        }
        return Duration.between(horaInicio, horaFinal);
    }

    public void incrementarTiempoCPU() {
        tiempoCPU++;
    }

    /** Duración real final, en segundos; no confundir con los ticks de CPU. */
    public double getTiempoTotalSegundos() {
        Duration duracion = getTiempoTotal();
        return duracion.getSeconds() + duracion.getNano() / 1_000_000_000.0;
    }

    public List<String> getArchivosAbiertos() {
        return archivosAbiertos;
    }

    public void agregarArchivoAbierto(String nombreArchivo) {
        if (!archivosAbiertos.contains(nombreArchivo)) {
            archivosAbiertos.add(nombreArchivo);
        }
    }

    public void cerrarArchivo(String nombreArchivo) {
        archivosAbiertos.remove(nombreArchivo);
    }

    @Override
    public String toString() {
        return "PID=" + pid
                + "\nEstado=" + estado
                + "\nMotivo error=" + motivoError
                + "\nPC=" + pc
                + "\nIR=" + ir
                + "\nAC=" + ac
                + "\nZeroFlag=" + zeroFlag
                + "\nAX=" + ax
                + " BX=" + bx
                + " CX=" + cx
                + " DX=" + dx
                + " AH=" + ah
                + " AL=" + al
                + "\nInicio programa=" + inicioPrograma
                + "\nFin programa=" + finPrograma
                + "\nTamaño programa=" + tamañoPrograma
                + "\nPrioridad=" + prioridad
                + "\nDireccion BCP=" + direccionBCP
                + "\nDireccion siguiente BCP=" + direccionSiguienteBCP
                + "\nPila=" + pila
                + "\nTiempo CPU=" + tiempoCPU + " s"
                + "\nHora inicio=" + horaInicio
                + "\nHora final=" + horaFinal
                + "\nDuracion real (s)=" + (horaFinal == null ? "Pendiente" : getTiempoTotalSegundos())
                + "\nArchivos abiertos=" + archivosAbiertos;
    }
}
