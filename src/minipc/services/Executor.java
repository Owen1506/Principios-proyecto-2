package minipc.services;

import minipc.model.BCP;
import minipc.model.CPU;
import minipc.model.Instruccion;

/**
 * Ejecuta las instrucciones de la Mini PC.
 *
 * El Executor modifica el estado de la CPU según la instrucción recibida.
 * Las interrupciones se delegan al GestorInterrupciones.
 */
public class Executor {

    private CPU cpu;
    private GestorProcesos gestorProcesos;
    private GestorInterrupciones gestorInterrupciones;

    public Executor(CPU cpu, GestorProcesos gestorProcesos, GestorInterrupciones gestorInterrupciones) {

        if (cpu == null) {
            throw new IllegalArgumentException("La CPU no puede ser null.");
        }

        if (gestorProcesos == null) {
            throw new IllegalArgumentException("El gestor de procesos no puede ser null.");
        }

        if (gestorInterrupciones == null) {
            throw new IllegalArgumentException("El gestor de interrupciones no puede ser null.");
        }

        this.cpu = cpu;
        this.gestorProcesos = gestorProcesos;
        this.gestorInterrupciones = gestorInterrupciones;
    }

    /**
     * Ejecuta una instrucción.
     *
     * @return true si la instrucción modificó directamente el PC
     */
    public boolean ejecutar(Instruccion instruccion) {

        String operacion = instruccion.getOperacion();

        switch (operacion) {

            case "MOV":
                ejecutarMOV(instruccion);
                return false;

            case "LOAD":
                ejecutarLOAD(instruccion);
                return false;

            case "STORE":
                ejecutarSTORE(instruccion);
                return false;

            case "ADD":
                ejecutarADD(instruccion);
                return false;

            case "SUB":
                ejecutarSUB(instruccion);
                return false;

            case "INC":
                ejecutarINC(instruccion);
                return false;

            case "DEC":
                ejecutarDEC(instruccion);
                return false;

            case "SWAP":
                ejecutarSWAP(instruccion);
                return false;

            case "JMP":
                ejecutarJMP(instruccion);
                return true;

            case "CMP":
                ejecutarCMP(instruccion);
                return false;

            case "JE":
                return ejecutarJE(instruccion);

            case "JNE":
                return ejecutarJNE(instruccion);

            case "PARAM":
                ejecutarPARAM(instruccion);
                return false;

            case "PUSH":
                ejecutarPUSH(instruccion);
                return false;

            case "POP":
                ejecutarPOP(instruccion);
                return false;

            case "INT":
                return ejecutarINT(instruccion);

            default:
                throw new IllegalArgumentException("Operación no soportada: " + operacion);
        }
    }

    /**
     * MOV puede mover un número, texto o el contenido de otro registro.
     */
    private void ejecutarMOV(Instruccion instruccion) {

        String registro = instruccion.getRegistro();

        // MOV registro, "texto"
        if (instruccion.getTexto() != null) {

            String texto = instruccion.getTexto();

            switch (registro) {

                case "DX":
                    cpu.getRegistros().setDX(texto);
                    break;

                case "AH":
                    cpu.getRegistros().setAH(texto);
                    break;

                case "AL":
                    cpu.getRegistros().setAL(texto);
                    break;

                default:
                    throw new IllegalArgumentException("El registro " + registro + " no puede recibir texto.");
            }

            return;
        }

        // MOV registro, registro
        if (instruccion.getRegistro2() != null) {

            String registro2 = instruccion.getRegistro2();

            if (registro.equals("DX")) {
                cpu.getRegistros().setDX(cpu.getRegistros().obtenerRegistroTexto(registro2));
                return;
            }

            if (registro.equals("AH")) {
                cpu.getRegistros().setAH(cpu.getRegistros().obtenerRegistroTexto(registro2));
                return;
            }

            if (registro.equals("AL")) {
                cpu.getRegistros().setAL(cpu.getRegistros().obtenerRegistroTexto(registro2));
                return;
            }

            int valor = cpu.getRegistros().obtenerRegistroNumerico(registro2);
            cpu.getRegistros().modificarRegistroNumerico(registro, valor);
            return;
        }

        // MOV registro, número
        int valor = instruccion.getValor();

        if (registro.equals("AH")) {
            cpu.getRegistros().setAH(String.valueOf(valor));
        } else if (registro.equals("AL")) {
            cpu.getRegistros().setAL(String.valueOf(valor));
        } else {
            cpu.getRegistros().modificarRegistroNumerico(registro, valor);
        }
    }

    /**
     * LOAD REGISTRO
     * AC obtiene el valor numérico del registro.
     */
    private void ejecutarLOAD(Instruccion instruccion) {

        String registro = instruccion.getRegistro();
        int valor = cpu.getRegistros().obtenerRegistroNumerico(registro);

        cpu.setAC(valor);
    }

    /**
     * STORE REGISTRO
     * Copia AC dentro del registro indicado.
     */
    private void ejecutarSTORE(Instruccion instruccion) {

        String registro = instruccion.getRegistro();
        cpu.getRegistros().modificarRegistroNumerico(registro, cpu.getAC());
    }

    /**
     * ADD REGISTRO
     */
    private void ejecutarADD(Instruccion instruccion) {

        int valor = cpu.getRegistros().obtenerRegistroNumerico(instruccion.getRegistro());
        cpu.setAC(cpu.getAC() + valor);
    }

    /**
     * SUB REGISTRO
     */
    private void ejecutarSUB(Instruccion instruccion) {

        int valor = cpu.getRegistros().obtenerRegistroNumerico(instruccion.getRegistro());
        cpu.setAC(cpu.getAC() - valor);
    }

    /**
     * INC o INC REGISTRO.
     */
    private void ejecutarINC(Instruccion instruccion) {

        String registro = instruccion.getRegistro();

        if (registro == null) {
            cpu.setAC(cpu.getAC() + 1);
            return;
        }

        int valor = cpu.getRegistros().obtenerRegistroNumerico(registro);
        cpu.getRegistros().modificarRegistroNumerico(registro, valor + 1);
    }

    /**
     * DEC o DEC REGISTRO.
     */
    private void ejecutarDEC(Instruccion instruccion) {

        String registro = instruccion.getRegistro();

        if (registro == null) {
            cpu.setAC(cpu.getAC() - 1);
            return;
        }

        int valor = cpu.getRegistros().obtenerRegistroNumerico(registro);
        cpu.getRegistros().modificarRegistroNumerico(registro, valor - 1);
    }

    /**
     * Intercambia el valor de dos registros numéricos.
     */
    private void ejecutarSWAP(Instruccion instruccion) {

        String registro1 = instruccion.getRegistro();
        String registro2 = instruccion.getRegistro2();

        int valor1 = cpu.getRegistros().obtenerRegistroNumerico(registro1);
        int valor2 = cpu.getRegistros().obtenerRegistroNumerico(registro2);

        cpu.getRegistros().modificarRegistroNumerico(registro1, valor2);
        cpu.getRegistros().modificarRegistroNumerico(registro2, valor1);
    }

    /**
     * Salto relativo.
     */
    private void ejecutarJMP(Instruccion instruccion) {
        aplicarSalto(instruccion);
    }

    /** Valida el destino antes de modificar el PC, dentro del programa actual. */
    private void aplicarSalto(Instruccion instruccion) {
        BCP proceso = obtenerProcesoActual();
        // ASMValidator ya comprobó que el desplazamiento es numérico antes del parseo.
        // Usa long para detectar también el desbordamiento de un desplazamiento int.
        long destino = (long) cpu.getPC() + instruccion.getValor();
        if (destino < proceso.getInicioPrograma() || destino > proceso.getFinPrograma()) {
            throw new IllegalStateException("Salto " + instruccion.getOperacion()
                    + " fuera de rango en PID " + proceso.getPid()
                    + ": PC=" + cpu.getPC() + ", destino=" + destino
                    + ", rango permitido=" + proceso.getInicioPrograma()
                    + ".." + proceso.getFinPrograma());
        }
        cpu.setPC((int) destino);
    }

    /**
     * Compara dos registros y modifica Zero Flag.
     */
    private void ejecutarCMP(Instruccion instruccion) {

        int valor1 = cpu.getRegistros().obtenerRegistroNumerico(instruccion.getRegistro());
        int valor2 = cpu.getRegistros().obtenerRegistroNumerico(instruccion.getRegistro2());

        cpu.setZeroFlag(valor1 == valor2);
    }

    /**
     * Salta si Zero Flag es verdadero.
     */
    private boolean ejecutarJE(Instruccion instruccion) {

        if (cpu.isZeroFlag()) {
            aplicarSalto(instruccion);
            return true;
        }

        return false;
    }

    /**
     * Salta si Zero Flag es falso.
     */
    private boolean ejecutarJNE(Instruccion instruccion) {

        if (!cpu.isZeroFlag()) {
            aplicarSalto(instruccion);
            return true;
        }

        return false;
    }

    /**
     * Agrega los parámetros a la pila del proceso actual.
     */
    private void ejecutarPARAM(Instruccion instruccion) {

        BCP proceso = obtenerProcesoActual();

        if (instruccion.getParametros().size() > proceso.getPila().getCapacidad() - proceso.getPila().getCantidad()) {
            throw new IllegalStateException("Desbordamiento de pila en PARAM: necesita "
                    + instruccion.getParametros().size() + " posiciones y quedan "
                    + (proceso.getPila().getCapacidad() - proceso.getPila().getCantidad()) + ".");
        }

        for (Integer parametro : instruccion.getParametros()) {
            proceso.getPila().push(parametro);
        }
    }

    /**
     * Coloca en la pila el valor de un registro.
     */
    private void ejecutarPUSH(Instruccion instruccion) {

        BCP proceso = obtenerProcesoActual();

        int valor = cpu.getRegistros().obtenerRegistroNumerico(instruccion.getRegistro());
        proceso.getPila().push(valor);
    }

    /**
     * Extrae el último valor de la pila y lo almacena en un registro.
     */
    private void ejecutarPOP(Instruccion instruccion) {

        BCP proceso = obtenerProcesoActual();

        int valor = proceso.getPila().pop();
        cpu.getRegistros().modificarRegistroNumerico(instruccion.getRegistro(), valor);
    }

    /**
     * Delega las interrupciones al GestorInterrupciones.
     */
    private boolean ejecutarINT(Instruccion instruccion) {
        return gestorInterrupciones.atender(instruccion);
    }

    /**
     * Obtiene el proceso que actualmente tiene la CPU.
     */
    private BCP obtenerProcesoActual() {

        // Cada CPU debe consultar su propio proceso.
        BCP proceso = gestorProcesos.getProcesoActual(cpu.getId());

        if (proceso == null) {
            throw new IllegalStateException(
                    "No existe un proceso actualmente en ejecución en CPU "
                            + cpu.getId());
        }

        return proceso;
    }
}
