/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package minipc.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Se encarga de validar la sintaxis básica de las instrucciones ASM
 * utilizadas por la Mini PC.
 *
 * Verifica que:
 *
 * - Las operaciones sean válidas.
 * - Los registros utilizados existan.
 * - Las instrucciones posean los operandos requeridos.
 * - Los valores utilizados en MOV sean números enteros válidos.
 * - Los valores inmediatos se encuentren dentro del rango permitido
 *   por el formato de 8 bits utilizado por el simulador.
 *
 * La clase no crea objetos Instruccion ni ejecuta operaciones;
 * únicamente detecta errores antes de que el programa sea procesado
 * por ASMParser.
 *
 * @author CR TECH
 */
public class ASMValidator {

    /**
     * Registros generales permitidos por la Mini PC.
     */
    private static final List<String> REGISTROS = List.of("AX", "BX", "CX", "DX","AH","AL");

    private static final List<String> SERVICIOS = List.of("21H", "10H", "20H");
    private static final List<String> VALORESAH = List.of("3CH","3DH","4DH","40H","41H");
    /**
     * Operaciones admitidas por el lenguaje ASM simplificado.
     */
    private static final List<String> OPERACIONES = List.of("MOV", "LOAD", "STORE", "ADD", "SUB", "INT", "SWAP", "PUSH", "POP", "JMP", "JE", "JNE", "CMP", "DEC" ,"INC" , "PARAM");


    /**
     * Valida todas las líneas de un programa ASM.
     *
     * Las líneas vacías son ignoradas. Cada error encontrado se agrega
     * a una lista indicando el número de línea correspondiente.
     *
     * Si la lista retornada está vacía, el programa no presenta
     * errores de validación detectados por esta clase.
     *
     * @param lineas líneas del programa ASM que se desea validar
     * @return lista con los errores encontrados
     */
    public List<String> validarPrograma(List<String> lineas) {

        List<String> errores =
                new ArrayList<>();

        for (int i = 0; i < lineas.size(); i++) {

            String linea =
                    lineas.get(i).trim();

            // Las líneas vacías no representan instrucciones.
            if (linea.isEmpty()) {
                continue;
            }

            String error =
                    validarLinea(linea);

            /*
             * Si la línea contiene un error, se guarda junto
             * con su posición original dentro del archivo.
             */
            if (error != null) {

                errores.add(
                        "Línea "
                        + (i + 1)
                        + ": "
                        + error
                );
            }
        }

        return errores;
    }


    /**
     * Valida una única instrucción ASM.
     *
     * Primero comprueba que la operación exista y posteriormente
     * delega la validación de sus operandos al método correspondiente.
     *
     * @param linea instrucción ASM que se desea validar
     * @return descripción del error encontrado, o null si la línea es válida
     */
    private String validarLinea(String linea) {

        /*
         * La instrucción se divide únicamente en dos partes:
         *
         * partes[0] -> operación
         * partes[1] -> operandos
         *
         * Ejemplo:
         *
         * MOV AX, 5
         *
         * partes[0] = "MOV"
         * partes[1] = "AX, 5"
         */
        String[] partes = linea.split("\\s+", 2);

        String operacion = partes[0].toUpperCase();


        /*
         * Se verifica primero que la operación pertenezca
         * al conjunto de instrucciones soportadas.
         */
        if (!OPERACIONES.contains(operacion)) {

            return "Operación no válida: "
                    + operacion;
        }


        /*
         * Toda operación válida requiere al menos un operando.
         */
        if ( (operacion.equals("INC") || operacion.equals("DEC")) && partes.length == 1){
        }
        else if (partes.length < 2) {

            return "La instrucción está incompleta.";
        }

        String operandos = "";
        if (partes.length == 1){
                operandos = null;
        }
        
        else if (!partes[1].equals(null)){
                operandos = partes[1].trim();
        }
                
        
        /*
         * La validación específica depende del tipo de operación.
         */
        switch (operacion) {

            case "MOV":
                return validarMOV(operandos);
            case "INT": //
            case "DEC": //
            case "INC": //
            case "PUSH"://
            case "POP": //
            case "JMP": //
            case "JE"://
            case "JNE": //
            case "PARAM": //

            case "SWAP": //
            case "CMP": //
            case "LOAD":
            case "STORE":
            case "ADD":
            case "SUB":
                return validarOperacionRegistro(operacion,operandos);
            default:
                return "Operación no reconocida.";
        }
    }


    /**
     * Valida los operandos de una instrucción MOV.
     *
     * MOV debe utilizar el formato:
     *
     * MOV REGISTRO, VALOR
     *
     * El registro debe ser AX, BX, CX o DX y el valor debe ser
     * un número entero comprendido entre -127 y 127.
     *
     * @param operandos contenido ubicado después de la operación MOV
     * @return descripción del error encontrado, o null si es válido
     */
    private String validarMOV(String operandos) {

        /*
         * MOV posee dos operandos separados por una coma:
         *
         * registro, valor
         * registro, registro
         */
        String[] partes = operandos.split(",", 2);

        if (partes.length != 2) {
            return "MOV debe tener el formato: " + "MOV REGISTRO, VALOR o MOV REGISTRO, REGISTRO";
        }

        String registro = partes[0].trim().toUpperCase();

        String valorTexto = partes[1].trim();


        // Verificar que el registro exista.
        if (!REGISTROS.contains(registro)) {

            return "Registro no válido: " + registro;
        }

        if (registro.equals("DX") || registro.equals("AH") || registro.equals("AL")){

        }

        else if (!REGISTROS.contains(valorTexto)){
                try {

                int valor = Integer.parseInt(valorTexto);

                /*
                * El formato entero utilizado por la Mini PC posee:
                *
                * 1 bit para el signo.
                * 7 bits para la magnitud.
                *
                * Por esta razón se permite un rango de -127 a 127.
                */
                if (valor < -127 || valor > 127) {

                        return "El valor debe estar entre -127 y 127.";
                }

                } catch (NumberFormatException e) {

                return "El valor de MOV debe ser un número entero o un registro valido.";
                }
        }

        // null indica que no se encontró ningún error.
        return null;
    }


    /**
     * Valida las instrucciones que reciben únicamente un registro
     * como operando.
     *
     * Este método es utilizado por LOAD, STORE, ADD y SUB.
     *
     * @param operacion nombre de la operación que se está validando
     * @param operando registro recibido por la instrucción
     * @return descripción del error encontrado, o null si es válido
     */
    private String validarOperacionRegistro(String operacion,String operando) {
        if (operando != null){
                String registro = operando.trim().toUpperCase();
                String registro2[] = operando.split(",");
                if (operando.toUpperCase().equals("AH") || operando.toUpperCase().equals("AL")){
                        if (!operacion.equals("MOV")){
                                return operacion + " AH y AL solo pueden ser modificados por MOV";
                        }
                }
                
                else if (operacion.equals("SWAP")){
                        if (registro2.length != 2){
                                return operacion + " No hay registros o no estan separados por una coma. Ej SWAP AX, BX";
                        }
                        else {
                                if (registro2[0].trim().equals(registro2[1].trim().toUpperCase())){
                                        return operacion + " Los registros son los mismos";
                                }
                                else if (!REGISTROS.contains(registro2[0].trim().toUpperCase()) || !REGISTROS.contains(registro2[1].trim().toUpperCase())) {
                                        return operacion + " debe recibir un registro válido: " + "AX, BX, CX o DX.";
                                }
                        }
                }
                else if (operacion.equals("CMP")){
                        if (registro2.length != 2){
                                return operacion + " No hay registros o no estan separados por una coma. Ej SWAP AX, BX";
                        }
                        else if (!REGISTROS.contains(registro2[0].trim().toUpperCase()) || !REGISTROS.contains(registro2[1].trim().toUpperCase())) {
                                return operacion + " debe recibir un registro válido: " + "AX, BX, CX o DX.";
                        }
                        
                }
                else if (operacion.equals("INT")){
                        if (!SERVICIOS.contains(operando.trim().toUpperCase())){
                           return operacion + " debe recibir una llamada de sistema válido: " + "21H, 20H o 10H.";     
                        }
                } 
                else if (operacion.equals("JMP") || operacion.equals("JE") || operacion.equals("JNE")){
                        try {
                                int desplazamiento = Integer.parseInt(operando);
                                return null;

                        } catch (NumberFormatException e) {
                                return operacion + " debe recibir un desplazamiento numérico.";
                        }  
                }
                else if (operacion.equals("PARAM")){
                        if (registro2.length > 3){
                                return operacion + " Cantidad de parametros no debe ser mayor a 3";
                        }
                        if (registro2.length < 1){
                                return operacion + " Cantidad de parametros no puede ser menor a 1";
                        }
                        for (int i = 0; i < registro2.length; i++) {

                                String num = registro2[i].trim();

                                try {

                                Integer.parseInt(num);

                                } catch (NumberFormatException e) {

                                return operacion + " debe recibir como parametro un valor numerico.";
                                }
                        }
                       

                }
                else if (!REGISTROS.contains(registro)) {
                
                        return operacion + " debe recibir un registro válido: " + "AX, BX, CX o DX.";
                        
                }
        }
        return null;
    }
}