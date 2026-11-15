package minipc.services.memoria.paginacion;

/** Relación de una página lógica con su marco mientras el programa reside en RAM. */
public record EntradaPagina(int pagina, int marco, int inicioMarco, int instrucciones) { }
