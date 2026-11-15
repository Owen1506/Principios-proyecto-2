package minipc.model;

/** Páginas y marcos del mismo tamaño, expresado en posiciones de memoria. */
public record ConfiguracionPaginacion(int tamañoPagina) implements ConfiguracionMemoria {
    public static final int TAMAÑO_POR_DEFECTO = 16;

    public ConfiguracionPaginacion {
        if (tamañoPagina <= 0 || (tamañoPagina & (tamañoPagina - 1)) != 0) {
            throw new IllegalArgumentException("El tamaño de página debe ser una potencia de 2 positiva.");
        }
    }

    @Override
    public MetodoAdministracionMemoria metodo() {
        return MetodoAdministracionMemoria.PAGINACION;
    }
}
