package minipc.model;

/** Páginas y marcos del mismo tamaño, expresado en posiciones de memoria. */
public record ConfiguracionPaginacion(int tamañoPagina, boolean virtual) implements ConfiguracionMemoria {
    public static final int TAMAÑO_POR_DEFECTO = 16;

    public ConfiguracionPaginacion {
        if (tamañoPagina <= 0 || (tamañoPagina & (tamañoPagina - 1)) != 0) {
            throw new IllegalArgumentException("El tamaño de página debe ser una potencia de 2 positiva.");
        }
    }

    public ConfiguracionPaginacion(int tamañoPagina) {
        this(tamañoPagina, false);
    }

    @Override
    public MetodoAdministracionMemoria metodo() {
        return MetodoAdministracionMemoria.PAGINACION;
    }
}
