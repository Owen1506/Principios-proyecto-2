package minipc.model;

/**
 * Representa la Unidad Central de Procesamiento (CPU) de la Mini PC.
 *
 * La CPU mantiene el estado actual de ejecución mediante:
 *
 * - PC (Program Counter): indica la dirección de la siguiente
 *   instrucción que debe ejecutarse.
 *
 * - IR (Instruction Register): almacena la línea ASM original
 *   de la instrucción que está siendo procesada.
 *
 * - AC (Accumulator): registro acumulador utilizado por operaciones
 *   aritméticas y de transferencia.
 *
 * - Registros generales: AX, BX, CX, DX, AH y AL.
 *
 * - Zero Flag: indica el resultado de una comparación realizada
 *   mediante CMP.
 */
public class CPU {

    private int pc;
    private String ir;
    private int ac;
    private boolean zeroFlag;

    private Registros registros;
    private int CpuActiva; //se usa para calcular cpu activa <=5

    /**
     * Crea una nueva CPU e inicializa todos sus componentes.
     */
    public CPU() {
        this.CpuActiva=+1;
        this.pc = 0;
        this.ir = "";
        this.ac = 0;
        this.zeroFlag = false;
        this.registros = new Registros();

    }

    /**
     * Obtiene el valor actual del Program Counter.
     *
     * @return dirección almacenada actualmente en el PC
     */
    public int getPC() {
        return pc;
    }

    /**
     * Modifica el valor del Program Counter.
     *
     * @param pc nueva dirección que será almacenada en el PC
     */
    public void setPC(int pc) {
        this.pc = pc;
    }

    /**
     * Obtiene el contenido actual del Instruction Register.
     *
     * @return instrucción ASM almacenada actualmente en el IR
     */
    public String getIR() {
        return ir;
    }

    /**
     * Modifica el contenido del Instruction Register.
     *
     * @param ir línea ASM original de la instrucción actual
     */
    public void setIR(String ir) {
        this.ir = ir;
    }

    /**
     * Obtiene el valor actual del acumulador.
     *
     * @return valor almacenado en el AC
     */
    public int getAC() {
        return ac;
    }

    /**
     * Modifica el valor almacenado en el acumulador.
     *
     * @param ac nuevo valor para el AC
     */
    public void setAC(int ac) {
        this.ac = ac;
    }

    /**
     * Obtiene el estado actual del Zero Flag.
     *
     * @return true si la última comparación produjo igualdad
     */
    public boolean isZeroFlag() {
        return zeroFlag;
    }

    /**
     * Modifica el estado del Zero Flag.
     *
     * @param zeroFlag nuevo estado del Zero Flag
     */
    public void setZeroFlag(boolean zeroFlag) {
        this.zeroFlag = zeroFlag;
    }

    /**
     * Obtiene el conjunto de registros de la CPU.
     *
     * @return objeto que contiene AX, BX, CX, DX, AH y AL
     */
    public Registros getRegistros() {
        return registros;
    }

    /**
     * Incrementa el Program Counter en una posición.
     */
    public void incrementarPC() {
        this.pc++;
    }

    /**
     * Reinicia el estado completo de la CPU.
     */
    public void reiniciarCPU() {
        this.pc = 0;
        this.ir = "";
        this.ac = 0;
        this.zeroFlag = false;
        this.registros.reiniciarRegistros();
    }

    /**
     * Genera una representación textual del estado actual de la CPU.
     *
     * @return cadena con el estado actual de la CPU
     */
    @Override
    public String toString() {
        return "PC=" + pc
                + "\nIR=" + ir
                + "\nAC=" + ac
                + "\nZeroFlag=" + zeroFlag
                + "\n" + registros.toString();
    }
}
