package minipc.model;

public sealed interface ConfiguracionMemoria permits ConfiguracionDinamica,
        ConfiguracionFijaIgual, ConfiguracionFijaDiferente {

  
    MetodoAdministracionMemoria metodo();

    default TipoEstrategiaMemoria estrategia() {
        return null;
    }
}
