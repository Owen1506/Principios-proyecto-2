package minipc.model;

public sealed interface ConfiguracionMemoria permits ConfiguracionDinamica {

  
    MetodoAdministracionMemoria metodo();

    default TipoEstrategiaMemoria estrategia() {
        return null;
    }
}
