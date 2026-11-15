package minipc.model;

// La cantidad de particiones se calcula con el espacio de usuario disponible.
public record ConfiguracionFijaIgual(int tamañoParticion) implements ConfiguracionMemoria {
    public ConfiguracionFijaIgual {
        if (tamañoParticion <= 0) {
            throw new IllegalArgumentException("El tamaño de partición debe ser positivo.");
        }
    }

    @Override
    public MetodoAdministracionMemoria metodo() {
        return MetodoAdministracionMemoria.FIJAS_IGUALES;
    }
}
