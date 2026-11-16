
package minipc.model;

public interface AlgoritmoPlanificacion {

    BCP seleccionarSiguiente(int cpuId);

    default boolean hayProcesosPreparados(int cpuId) {

        return seleccionarSiguiente(cpuId) != null;
    }

    default boolean debeExpropiar(int cpuId, BCP actual) {

        return false;
    }

    default void alDespachar(int cpuId, BCP proceso) {

        //
    }

    default void alConsumirTick(int cpuId, BCP proceso) {

        //
    }

    /* ¿Qué hace? Prepara las cuentas del segundo global. ¿Quién lo llama? El controlador antes de ejecutar las CPU. */
    default void iniciarTick() {
    }

    /* ¿Qué hace? Cierra las cuentas del segundo global. ¿Quién lo llama? El controlador después de ejecutar las CPU. */
    default void finalizarTick() {
    }
}
