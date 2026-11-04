package minipc.parser;

import minipc.model.Instruccion;

import java.util.ArrayList;
import java.util.List;

/**
 * Se encarga de transformar las líneas de código ASM previamente
 * validadas en objetos de tipo Instruccion.
 *
 * El parser identifica la operación, registros, valores inmediatos,
 * cadenas de texto, servicios de interrupción y parámetros cuando
 * corresponde.
 *
 * El resultado es una lista de instrucciones lista para ser cargada
 * en memoria y posteriormente ejecutada por la Mini PC.
 *
 * @author CR TECH
 */
public class ASMParser {

    private static final List<String> REGISTROS = List.of("AX", "BX", "CX", "DX", "AH", "AL");

    /**
     * Convierte una lista de líneas ASM en objetos Instruccion.
     *
     * @param lineas líneas del archivo ASM previamente leído y validado
     * @return lista de instrucciones interpretadas
     */
    public List<Instruccion> parsear(List<String> lineas) {

        List<Instruccion> instrucciones = new ArrayList<>();

        for (String linea : lineas) {

            linea = linea.trim();

            if (linea.isEmpty()) {
                continue;
            }

            Instruccion instruccion = parsearLinea(linea);
            instrucciones.add(instruccion);
        }

        return instrucciones;
    }

    /**
     * Interpreta una única línea ASM y genera el objeto Instruccion
     * correspondiente.
     *
     * @param linea línea ASM que se desea interpretar
     * @return objeto Instruccion generado
     */
    private Instruccion parsearLinea(String linea) {

        String[] partes = linea.split("\\s+", 2);

        String operacion = partes[0].toUpperCase();
        String operandos = "";

        if (partes.length > 1) {
            operandos = partes[1].trim();
        }

/*
         * MOV puede recibir:
         *
         * MOV AX, 5
         * MOV AX, BX
         * MOV DX, "archivo.txt"
         * MOV AH, "3CH"
         * MOV AL, "Hola mundo"
         */
        if (operacion.equals("MOV")) {

            /*
             *  Mejor poner un limite en el split para que asi no se hagan separaciones de mas y ocurran problemas en ejecucion.
             *
             * MOV AL, "Hola, mundo"
             */
            String[] partesMOV = operandos.split(",", 2);

            String registro = partesMOV[0].trim().toUpperCase();
            String segundoOperando = partesMOV[1].trim();

            // MOV con cadena de texto.
            if (segundoOperando.startsWith("\"") && segundoOperando.endsWith("\"")) {

                // Se eliminan las comillas exteriores.
                String texto = segundoOperando.substring(1, segundoOperando.length() - 1);

                return new Instruccion(operacion, registro, null, null, null, null, linea, texto);
            }

            // MOV entre registros.
            if (REGISTROS.contains(segundoOperando.toUpperCase())) {
                String registro2 = segundoOperando.toUpperCase();
                return new Instruccion(operacion, registro, registro2, null, null, null, linea, null);
            }

            // MOV con valor numérico inmediato.
            int valor = Integer.parseInt(segundoOperando);
            return new Instruccion(operacion, registro, null, null, valor, null, linea, null);
        }

        /*
         * Interrupciones.
         */
        else if (operacion.equals("INT")) {

            String servicio = operandos.toUpperCase();

            switch (servicio) {

                case "21H":
                    return new Instruccion(operacion, null, null, servicio, null, null, linea, null);

                case "10H":
                    return new Instruccion(operacion, null, null, servicio, null, null, linea, null);

                case "20H":
                    return new Instruccion(operacion, null, null, servicio, null, null, linea, null);

                default:
                    throw new IllegalArgumentException("Servicio de interrupción inválido: " + servicio);
            }
        }

        /*
         * Saltos relativos.
         */
        else if (operacion.equals("JMP") || operacion.equals("JE") || operacion.equals("JNE")) {

            int valor = Integer.parseInt(operandos);
            return new Instruccion(operacion, null, null, null, valor, null, linea, null);
        }

        /*
         * INC y DEC pueden utilizarse con o sin registro.
         *
         * INC      -> acumulador
         * INC AX   -> registro AX
         */
        else if (operacion.equals("INC") || operacion.equals("DEC")) {

            if (operandos.isEmpty()) {
                return new Instruccion(operacion, null, null, null, 1, null, linea, null);
            }

            String registro = operandos.toUpperCase();
            return new Instruccion(operacion, registro, null, null, 1, null, linea, null);
        }

        /*
         * SWAP y CMP reciben dos registros.
         */
        else if (operacion.equals("SWAP") || operacion.equals("CMP")) {

            String[] partesOperacion = operandos.split(",", 2);

            String registro = partesOperacion[0].trim().toUpperCase();
            String registro2 = partesOperacion[1].trim().toUpperCase();

            if (operacion.equals("SWAP")) {
                return new Instruccion(operacion, registro, registro2, null, null, null, linea, null);
            }

            return new Instruccion(operacion, registro, registro2, null, null, null, linea, null);
        }

        /*
         * PARAM recibe hasta tres valores numéricos.
         */
        else if (operacion.equals("PARAM")) {

            List<Integer> params = new ArrayList<>();
            String[] parametros = operandos.split(",");

            for (String parametro : parametros) {
                int valor = Integer.parseInt(parametro.trim());
                params.add(valor);
            }

            return new Instruccion(operacion, null, null, null, null, params, linea, null);
        }

        /*
         * Instrucciones que reciben únicamente un registro.
         */
        String registro = operandos.toUpperCase();

        switch (operacion) {

            case "ADD":
                return new Instruccion(operacion, registro, null, null, null, null, linea, null);

            case "LOAD":
                return new Instruccion(operacion, registro, null, null, null, null, linea, null);

            case "SUB":
                return new Instruccion(operacion, registro, null, null, null, null, linea, null);

            case "STORE":
                return new Instruccion(operacion, registro, null, null, null, null, linea, null);

            case "PUSH":
                return new Instruccion(operacion, registro, null, null, null, null, linea, null);

            case "POP":
                return new Instruccion(operacion, registro, null, null, null, null, linea, null);

            default:
                throw new IllegalArgumentException("Operación no reconocida por el parser: " + operacion);
        }
    }
}