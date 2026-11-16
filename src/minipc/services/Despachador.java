package minipc.services;

import minipc.model.BCP;
import minipc.model.CPU;
import minipc.model.AlgoritmoPlanificacion;

public class Despachador {

    private CPU cpu;

    private GestorProcesos gestorProcesos;

    private AlgoritmoPlanificacion planificador;

    // se define el constructor de la clase Despachador, que recibe como parámetros
    // una CPU, un GestorProcesos y un AlgoritmoPlanificacion. Se realizan
    // validaciones para asegurarse de que ninguno de los parámetros sea nulo. Si
    // alguno es nulo, se lanza una excepción IllegalArgumentException con un
    // mensaje correspondiente. Luego, se asignan los valores de los parámetros a
    // los atributos de la clase.
    public Despachador(
            CPU cpu,
            GestorProcesos gestorProcesos,
            AlgoritmoPlanificacion planificador) {

        if (cpu == null) {
            throw new IllegalArgumentException(
                    "La CPU no puede ser null.");
        }

        if (gestorProcesos == null) {
            throw new IllegalArgumentException(
                    "El gestor de procesos no puede ser null.");
        }

        if (planificador == null) {
            throw new IllegalArgumentException(
                    "El planificador no puede ser null.");
        }

        this.cpu = cpu;
        this.gestorProcesos = gestorProcesos;
        this.planificador = planificador;
    }

    // establece el despachador correspondiente
    public void establecerPlanificador(
            AlgoritmoPlanificacion nuevo) {

        if (nuevo == null) {
            throw new IllegalArgumentException(
                    "El planificador no puede ser null.");
        }

        this.planificador = nuevo;
    }

    // guarda la info en el bcp, se debe activar antes de cambios de contexto
    public void guardarContextoActual() {

        BCP actual = gestorProcesos.getProcesoActual(
                cpu.getId());

        if (actual != null) {

            actual.guardarContexto(cpu);
        }
    }

    // despacha el siguiente proceso preparado, si no hay procesos preparados, deja
    // la CPU libre
    public BCP despacharSiguiente() {

        if (gestorProcesos.getProcesoActual(
                cpu.getId()) != null) {

            throw new IllegalStateException(
                    "La CPU ya está siendo utilizada por un proceso.");
        }

        BCP siguiente = planificador.seleccionarSiguiente(
                cpu.getId());

        if (siguiente == null) {

            cpu.reiniciarCPU();

            return null;
        }

        siguiente.restaurarContexto(cpu);

        gestorProcesos.iniciarEjecucion(
                cpu.getId(),
                siguiente);

        planificador.alDespachar(
                cpu.getId(),
                siguiente);

        return siguiente;
    }

    // si el proceso actual debe ser expropiado, se guarda su contexto y se devuelve
    // a la cola de preparados
    public BCP expropiarActual() {

        BCP actual = gestorProcesos.getProcesoActual(
                cpu.getId());

        if (actual == null) {

            throw new IllegalStateException(
                    "No existe un proceso para expropiar.");
        }

        guardarContextoActual();

        gestorProcesos.expropiarProcesoActual(
                cpu.getId());

        return actual;
    }
}
