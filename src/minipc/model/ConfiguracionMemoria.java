package minipc.model;

public sealed interface ConfiguracionMemoria permits ConfiguracionDinamica,
        ConfiguracionFijaIgual, ConfiguracionFijaDiferente, ConfiguracionPaginacion {

  
    MetodoAdministracionMemoria metodo();

    default TipoEstrategiaMemoria estrategia() {
        return null;
    }
}
