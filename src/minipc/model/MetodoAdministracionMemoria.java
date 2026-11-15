package minipc.model;


public enum MetodoAdministracionMemoria {

    FIJAS_IGUALES("Particiones fijas iguales"),
    FIJAS_DIFERENTES("Particiones fijas diferentes"),
    DINAMICAS("Particiones dinámicas"),
    PAGINACION("Paginación");

    private final String etiqueta;

    MetodoAdministracionMemoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public boolean estaImplementado() {
        return true;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
