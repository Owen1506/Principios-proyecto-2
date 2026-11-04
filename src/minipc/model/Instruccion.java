/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package minipc.model;

import java.util.List;

/**
 * Representa una instrucción del lenguaje ensamblador simplificado
 * utilizado por la Mini PC.
 *
 * Cada instrucción almacena la operación a ejecutar, el registro
 * asociado, un segundo registro cuando corresponde, un servicio
 * para instrucciones INT, un valor inmediato y sus parámetros,
 * además de la línea ASM original y su peso de ejecución.
 *
 * Ejemplos de instrucciones:
 *
 * MOV AX, 5
 * LOAD AX
 * ADD BX
 * PARAM 5, 10, 20
 *
 * @author CR TECH
 */
public class Instruccion {

    private String operacion;
    private String registro;
    private String registro2; // Para operaciones como CMP
    private String servicio; // Para operaciones como INT 20H
    private Integer valor; // Para movimientos inmediatos y desplazamientos
    private List<Integer> parametros; // Para operaciones como PARAM
    private String textoOriginal;
    private String texto;
    private final int peso = 1;

    /**
     * Crea una nueva instrucción con todos los datos necesarios
     * para su procesamiento y ejecución.
     *
     * @param operacion operación de la instrucción, por ejemplo MOV, LOAD o ADD
     * @param registro registro asociado a la instrucción, por ejemplo AX o BX
     * @param registro2 segundo registro cuando la instrucción lo requiere
     * @param servicio servicio asociado a una instrucción INT
     * @param valor valor inmediato o desplazamiento; puede ser null si no aplica
     * @param parametros lista de parámetros numéricos; puede ser null si no aplica
     * @param textoOriginal línea ASM original de la instrucción
     * @param texto contenido de un operando de texto o null si no aplica
     */
    public Instruccion(String operacion,String registro,String registro2,String servicio,Integer valor,List<Integer> parametros,String textoOriginal,String texto) {
        this.operacion = operacion;
        this.registro = registro;
        this.registro2 = registro2;
        this.servicio = servicio;
        this.valor = valor;
        this.parametros = parametros;
        this.textoOriginal = textoOriginal;
        this.texto = texto;
    }

    /**
     * Obtiene la operación asociada a la instrucción.
     *
     * @return nombre de la operación
     */
    public String getOperacion() {
        return operacion;
    }

    /**
     * Obtiene el registro utilizado por la instrucción.
     *
     * @return nombre del registro
     */
    public String getRegistro() {
        return registro;
    }

    /**
     * Obtiene el segundo registro utilizado por la instrucción.
     *
     * @return nombre del segundo registro o null si no aplica
     */
    public String getRegistro2() {
        return registro2;
    }

    /**
     * Obtiene el servicio asociado a una instrucción INT.
     *
     * @return servicio de la interrupción o null si no aplica
     */
    public String getServicio() {
        return servicio;
    }

    /**
     * Obtiene el valor inmediato o desplazamiento de la instrucción.
     *
     * @return valor de la instrucción o null si no aplica
     */
    public Integer getValor() {
        return valor;
    }

    /**
     * Obtiene los parámetros numéricos de la instrucción.
     *
     * @return lista de parámetros o null si la instrucción no utiliza parámetros
     */
    public List<Integer> getParametros() {
        return parametros;
    }

    /**
     * Obtiene la línea ASM original de la instrucción.
     *
     * @return línea ASM original
     */
    public String getTextoOriginal() {
        return textoOriginal;
    }

    public String getTexto() {
        return texto;
    }

    public int getPeso() {
        return peso;
    }

    /**
     * Muestra la instrucción tal como aparece en el programa ASM.
     */
    @Override
    public String toString() {
        return textoOriginal;
    }
}