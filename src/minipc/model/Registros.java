package minipc.model;

/**
 * Administra los registros generales AX, BX, CX y DX y AH, AL
 * utilizados por la CPU de la Mini PC.
 *
 * Permite consultar, modificar y reiniciar el valor de cada registro,
 * tanto de forma individual como utilizando el nombre del registro.
 */
public class Registros {

    private int ax; // Para este archivo se debe hacer parte baja y alta pero no como tal su representacion como seria en un ambiente ensamblador sino que unicamente algo simulado.

    private int bx;
    private int cx;
    
    private String dx; // Ya que se podran hacer movimientos a dx como MOV DX, "prueba" y posteriormente interrupcion para manejo de archivos.
    private String al;
    private String ah;


    /**
     * Crea el conjunto de registros generales e inicializa
     * todos sus valores en cero.
     */
    public Registros() {
        this.ax = 0;
        this.bx = 0;
        this.cx = 0;
        this.dx = "0";
        this.al = "";
        this.ah = "";
    }


    /**
     * Obtiene el valor actual del registro AX.
     *
     * @return valor almacenado en AX
     */
    public int getAX() {
        return ax;
    }


    /**
     * Modifica el valor almacenado en el registro AX.
     *
     * @param valor nuevo valor para AX
     */
    public void setAX(int valor) {
        this.ax = valor;
    }


    /**
     * Obtiene el valor actual del registro BX.
     *
     * @return valor almacenado en BX
     */
    public int getBX() {
        return bx;
    }


    /**
     * Modifica el valor almacenado en el registro BX.
     *
     * @param valor nuevo valor para BX
     */
    public void setBX(int valor) {
        this.bx = valor;
    }


    /**
     * Obtiene el valor actual del registro CX.
     *
     * @return valor almacenado en CX
     */
    public int getCX() {
        return cx;
    }


    /**
     * Modifica el valor almacenado en el registro CX.
     *
     * @param valor nuevo valor para CX
     */
    public void setCX(int valor) {
        this.cx = valor;
    }


    /**
     * Obtiene el valor actual del registro DX.
     *
     * @return valor almacenado en DX
     */
    public String getDX() {
        return dx;
    }
    /**
     * Modifica el valor almacenado en el registro DX.
     *
     * @param valor nuevo valor para DX
     */
    public void setDX(String valor) {
        this.dx = valor;
    }
    public void setDX(int valor) {
        this.dx = String.valueOf(valor);
    }

    public boolean dxEsNumerico() {
        try {
            Integer.parseInt(dx);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public int getDXComoEntero() {

        try {
            return Integer.parseInt(dx);

        } catch (NumberFormatException e) {

            throw new IllegalStateException(
                "DX no contiene un valor numérico: " + dx
            );
        }
    }


    public String getAH() {
        return ah;
    }

    public void setAH(String ah) {
        this.ah = ah.toUpperCase();
    }

    public String getAL() {
        return al;
    }

    public void setAL(String al) {
        this.al = al;
    }
    /**
     * Obtiene el valor de un registro general a partir de su nombre.
     *
     * El nombre recibido se convierte a mayúsculas para permitir
     * entradas como "ax", "Ax" o "AX".
     *
     * @param nombreRegistro nombre del registro a consultar
     * @return valor almacenado en el registro solicitado
     * @throws IllegalArgumentException si el registro no es AX, BX, CX o DX
     */
  //  public int obtenerRegistro(String nombreRegistro) {

    //    nombreRegistro = nombreRegistro.toUpperCase();

    //    switch (nombreRegistro) {

    //        case "AX":
    //            return getAX();

    //        case "BX":
    //            return getBX();

     //       case "CX":
       //         return getCX();
//
            //case "DX": 
            //    return getDX(); Cambiar esto y en el executor.java nada mas llamar al respectivo getDX

        //    default:
       //         throw new IllegalArgumentException(
       //                 "Nombre de registro inválido: " + nombreRegistro
      //          );
     //  }
   // }

public int obtenerRegistroNumerico(String nombreRegistro) {

    switch (nombreRegistro.toUpperCase()) {

        case "AX":
            return ax;

        case "BX":
            return bx;

        case "CX":
            return cx;

        case "DX":
            return getDXComoEntero();

        default:
            throw new IllegalArgumentException(
                "Registro inválido: " + nombreRegistro
            );
    }
}
public String obtenerRegistroTexto(String nombreRegistro) {

    switch (nombreRegistro.toUpperCase()) {

        case "AX":
            return String.valueOf(ax);

        case "BX":
            return String.valueOf(bx);

        case "CX":
            return String.valueOf(cx);

        case "DX":
            return dx;
        case "AL":
            return al;
        case "AH":
            return ah;

        default:
            throw new IllegalArgumentException(
                "Registro inválido: " + nombreRegistro
            );
    }
}
    /**
     * Modifica el valor de un registro general a partir de su nombre.
     *
     * @param nombreRegistro nombre del registro que se desea modificar
     * @param valor nuevo valor que se almacenará en el registro
     * @throws IllegalArgumentException si el registro no es AX, BX, CX o DX
     */
    public void modificarRegistroNumerico(String nombreRegistro,int valor) {

        nombreRegistro = nombreRegistro.toUpperCase();

        switch (nombreRegistro) {

            case "AX":
                setAX(valor);
                break;

            case "BX":
                setBX(valor);
                break;

            case "CX":
                setCX(valor);
                break;

            case "DX":
                setDX(valor);
                break;

            default:
                throw new IllegalArgumentException(
                        "Nombre de registro inválido: " + nombreRegistro
                );
        }
    }


    /**
     * Reinicia todos los registros generales a cero.
     */
    public void reiniciarRegistros() {

        setAX(0);
        setBX(0);
        setCX(0);
        setDX(0);
        setAL("");
        setAH("");
    }


    /**
     * Retorna una representación textual del estado actual
     * de los registros generales.
     *
     * @return cadena con los valores de AX, BX, CX y DX
     */
    @Override
    public String toString() {

        return "AX=" + getAX()
                + " BX=" + getBX()
                + " CX=" + getCX()
                + " DX=" + getDX()
                + " AH=" + getAH()
                + " AL=" + getAL();
    }
}