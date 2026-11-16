package minipc.model;

import minipc.services.memoria.EstrategiaAsignacionMemoria;
import minipc.services.memoria.dinamica.BestFit;
import minipc.services.memoria.dinamica.FirstFit;
import minipc.services.memoria.dinamica.NextFit;


public enum TipoEstrategiaMemoria {

    FIRST_FIT,
    NEXT_FIT,
    BEST_FIT;

    /**
     * Construye la estrategia para el área [inicioUsuario, tamañoMemoria).
     * La llama Memoria al crear el área de usuario o al cambiar la estrategia.
     */
    public EstrategiaAsignacionMemoria crear(int inicioUsuario, int tamañoMemoria) {
        return switch (this) {
            case FIRST_FIT -> new FirstFit(inicioUsuario, tamañoMemoria);
            case NEXT_FIT -> new NextFit(inicioUsuario, tamañoMemoria);
            case BEST_FIT -> new BestFit(inicioUsuario, tamañoMemoria);
        };
    }
    public boolean estaImplementada() {
        return true;
    }
}
