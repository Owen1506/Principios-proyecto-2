package minipc.model;

import java.util.List;

// La copia inmutable evita que se alteren los tamaños durante la simulación.
public record ConfiguracionFijaDiferente(List<Integer> tamaños) implements ConfiguracionMemoria {
    public ConfiguracionFijaDiferente {
        if (tamaños == null || tamaños.isEmpty() || tamaños.stream().anyMatch(t -> t == null || t <= 0)) {
            throw new IllegalArgumentException("Indique tamaños de partición positivos.");
        }
        tamaños = List.copyOf(tamaños);
    }

    @Override
    public MetodoAdministracionMemoria metodo() {
        return MetodoAdministracionMemoria.FIJAS_DIFERENTES;
    }
}
