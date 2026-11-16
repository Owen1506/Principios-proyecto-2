package minipc.model;


public record ConfiguracionDinamica(TipoEstrategiaMemoria estrategia)
        implements ConfiguracionMemoria {

    public ConfiguracionDinamica {
        if (estrategia == null) {
            throw new IllegalArgumentException(
                    "Debe indicar una estrategia para las particiones dinámicas.");
        }
        if (!estrategia.estaImplementada()) {
            throw new UnsupportedOperationException(
                    "La estrategia de memoria " + estrategia
                            + " todavía no está implementada.");
        }
    }

    @Override
    public MetodoAdministracionMemoria metodo() {
        return MetodoAdministracionMemoria.DINAMICAS;
    }
}
