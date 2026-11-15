package minipc.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigLoader {
    public Configuracion cargar(Path archivo) throws IOException {
        Properties propiedades = new Properties();
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        }
        return new Configuracion(
                entero(propiedades, "ram"), entero(propiedades, "almacenamiento"),
                entero(propiedades, "memoriaVirtual"), entero(propiedades, "indice"),
                entero(propiedades, "inicioUsuario"),
                entero(propiedades, "maxProcesosEnRam"), entero(propiedades, "maxProcesos"),
                booleano(propiedades, "conservarArchivosAlReiniciar"),
                valor(propiedades, "algoritmo"),
                valorOpcional(propiedades, "metodoMemoria", "DINAMICAS"),
                valorOpcional(propiedades, "estrategiaMemoria", "FIRST_FIT"));
    }

    /*
     * Lee la estrategia de memoria y usa FIRST_FIT cuando la clave no existe,
     * para que los archivos de configuración anteriores sigan funcionando.
     * Lo llama cargar al construir Configuracion.
     */
    private String valorOpcional(Properties propiedades, String clave, String defecto) {
        String valor = propiedades.getProperty(clave);
        return valor == null || valor.trim().isEmpty() ? defecto : valor.trim();
    }

    private String valor(Properties propiedades, String clave) {
        String valor = propiedades.getProperty(clave);
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Falta el valor de configuración: " + clave);
        }
        return valor.trim();
    }

    private int entero(Properties propiedades, String clave) {
        try {
            return Integer.parseInt(valor(propiedades, clave));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El valor de " + clave + " debe ser un entero.", e);
        }
    }

    private boolean booleano(Properties propiedades, String clave) {
        String valor = valor(propiedades, clave);
        if (!valor.equalsIgnoreCase("true") && !valor.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("El valor de " + clave + " debe ser true o false.");
        }
        return Boolean.parseBoolean(valor);
    }
}
