package minipc.ui;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import minipc.config.ConfigLoader;
import minipc.config.Configuracion;
import minipc.controller.ContextoNucleo;
import minipc.controller.Controlador;
import minipc.model.*;

/**
 * Interfaz principal del simulador Mini PC.
 *
 * Muestra procesos, BCP, memoria, disco, archivos,
 * reloj global y hasta cinco CPU independientes.
 *
 * Toda la lógica de simulación se delega al Controlador.
 */
public class MainWindow extends JFrame {

    private final Path raiz;

    private Controlador controlador;
    private Configuracion configuracion;

    private final Map<Integer, String> nombres = new HashMap<>();

    // =========================================================
    // COLORES
    // =========================================================

    private static final Color FONDO = new Color(242, 246, 251);

    private static final Color PANEL = Color.WHITE;

    private static final Color BORDE = new Color(221, 229, 238);

    private static final Color TEXTO = new Color(28, 42, 60);

    private static final Color TEXTO_SECUNDARIO = new Color(92, 104, 116);

    private static final Color AZUL = new Color(37, 99, 235);

    private static final Color AZUL_SUAVE = new Color(230, 240, 252);

    private static final Color VERDE = new Color(53, 145, 91);

    private static final Color VERDE_SUAVE = new Color(231, 246, 236);

    private static final Color NARANJA = new Color(205, 127, 42);

    private static final Color ROJO = new Color(184, 69, 69);

    private static final Color ROJO_SUAVE = new Color(249, 229, 229);

    private static final Color MORADO = new Color(118, 83, 169);

    private static final Color MORADO_SUAVE = new Color(239, 233, 248);

    private static final Color GRIS_SUAVE = new Color(236, 239, 242);

    /** La escala de CPU se mantiene igual en todos los paneles. */
    private static final Color[] COLORES_CPU = {
            new Color(37, 99, 235),
            new Color(29, 142, 94),
            new Color(213, 124, 38),
            new Color(132, 83, 174),
            new Color(191, 70, 118)
    };

    private static final Color[] FONDOS_CPU = {
            new Color(226, 238, 255),
            new Color(224, 246, 235),
            new Color(255, 240, 222),
            new Color(241, 234, 251),
            new Color(253, 232, 242)
    };

    // En la matriz transpuesta las filas son atributos y las columnas procesos.
    private static final int FILA_PID = 0;
    private static final int FILA_CPU = 1;
    private static final int FILA_ESTADO = 3;
    private static final int FILA_LLEGADA = 9;
    private static final int FILA_PRIORIDAD = 10;

    private static final String[] CAMPOS_PROCESO = {
            "PID", "CPU asignada", "Programa", "Estado", "Ubicación",
            "Base", "Instrucciones", "Tiempo CPU (s)", "Ráfaga",
            "Llegada ✎", "Prioridad ✎", "Restante"
    };
    private static final int SEGUNDOS_CRONOGRAMA_INICIALES = 80;

    // =========================================================
    // INDICADORES GENERALES
    // =========================================================

    private final JLabel valorReloj = new JLabel("0 s");

    private final JLabel valorNucleos = new JLabel("1 / 5");

    private final JLabel valorRAM = new JLabel("-");

    private final JLabel valorKernel = new JLabel("-");

    private final JLabel valorProgramasEnRAM = new JLabel("-");

    private final JLabel valorDisco = new JLabel("-");

    private final JProgressBar barraRAM = new JProgressBar();

    private final JProgressBar barraDisco = new JProgressBar();

    private final JLabel estadoSimulacion = new JLabel("Listo para configurar");

    private final JLabel algoritmoSuperior = new JLabel("Algoritmo: sin seleccionar");

    // =========================================================
    // CONFIGURACION DE CPU Y ALGORITMO
    // =========================================================

    private final JSpinner cantidadCPU = new JSpinner(
            new SpinnerNumberModel(1, 1, 5, 1));

    /** Mostrar solo algoritmos funcionales; los restantes siguen en desarrollo. */
    private final JComboBox<TipoAlgoritmo> algoritmos = new JComboBox<>(
            new TipoAlgoritmo[] {
                    TipoAlgoritmo.FCFS,
                    TipoAlgoritmo.SPN,
                    TipoAlgoritmo.SRT,
                    TipoAlgoritmo.HRRN,
                    TipoAlgoritmo.RR,
                    TipoAlgoritmo.PRIORIDAD,
                    TipoAlgoritmo.SELFISH_ROUND_ROBIN
            });

    private final JSpinner quantumRR = new JSpinner(
            new SpinnerNumberModel(3, 1, 100, 1));

    private boolean ajustandoControles = false;

    // =========================================================
    // NAVEGACIÓN Y RESUMEN DE LA CPU ACTIVA
    // =========================================================

    private int cpuSeleccionada = 1;

    private final JLabel procesoCPU = new JLabel("CPU 1 · Libre");

    private final JLabel tiempoOcupadoCPU = valorRegistro("0 s");

    private final JLabel tiempoOciosoCPU = valorRegistro("0 s");

    private final JButton cpuAnterior = new JButton("◀");

    private final JButton cpuSiguiente = new JButton("▶");

    // =========================================================
    // REGISTROS DE LA CPU SELECCIONADA
    // =========================================================

    private final JLabel valorPC = valorRegistro("0");

    private final JLabel valorIR = valorRegistro("");

    private final JLabel valorAC = valorRegistro("0");

    private final JLabel valorAX = valorRegistro("0");

    private final JLabel valorBX = valorRegistro("0");

    private final JLabel valorCX = valorRegistro("0");

    private final JLabel valorDX = valorRegistro("0");

    private final JLabel valorAH = valorRegistro("");

    private final JLabel valorAL = valorRegistro("");

    private final JLabel valorZF = valorRegistro("false");

    // =========================================================
    // CONSOLA Y CONTROLES
    // =========================================================

    private final JLabel estadoMonitor = new JLabel("Sin entrada solicitada");


    /** Vista organizada del BCP en tres grupos independientes. */
    private final JTabbedPane pestañasBCP = new JTabbedPane();

    /** Tarjetas de procesos distribuidas de izquierda a derecha. */
    private final JPanel contenedorTarjetas = new JPanel();

    private final JScrollPane scrollTarjetas = new JScrollPane(contenedorTarjetas);

    private final JPanel contenedorFinalizados = new JPanel();
    private final JScrollPane scrollFinalizados = new JScrollPane(contenedorFinalizados);

    private final JLabel contadorProcesos = new JLabel("Sin procesos cargados");

    private final JTextArea pantalla = texto();

    private final JTextArea estadisticas = texto();

    private final JTextField entrada = new JTextField(10);

    private final JCheckBox conservar = new JCheckBox(
            "Conservar archivos al reiniciar",
            true);

    private final JButton cargar = new JButton("Cargar ASM");

    private final JButton disco = new JButton("Ejecutar desde disco");

    private final JButton siguiente = new JButton("Siguiente");

    private final JButton automatico = new JButton("Ejecutar");

    private final JButton pausa = new JButton("Pausar");

    private final JButton enviar = new JButton("Enviar");

    // =========================================================
    // MODELO DE PROCESOS
    // =========================================================

    private final DefaultTableModel trabajos = new DefaultTableModel(
            new Object[] { "Dato" }, 0) {

        @Override
        public boolean isCellEditable(
                int fila,
                int columna) {

            if ((fila != FILA_LLEGADA && fila != FILA_PRIORIDAD)
                    || columna <= 0
                    || controlador == null
                    || controlador.getTiempoSimulado() != 0
                    || continuarAutomatico) {
                return false;
            }

            int pid = Integer.parseInt(
                    getValueAt(FILA_PID, columna).toString());

            BCP proceso = controlador
                    .getGestorProcesos()
                    .buscarProceso(pid);

            if (proceso == null) {
                return false;
            }

            // La llegada y la prioridad se editan antes del primer tick.
            return proceso.getEstado() == EstadoProceso.NUEVO;
        }

        @Override
        public void setValueAt(
                Object valor,
                int fila,
                int columna) {

            if ((fila != FILA_LLEGADA && fila != FILA_PRIORIDAD)
                    || columna <= 0) {
                super.setValueAt(valor, fila, columna);
                return;
            }

            if (!isCellEditable(fila, columna)) {
                return;
            }

            String campo = fila == FILA_LLEGADA
                    ? "El tiempo de llegada"
                    : "La prioridad";

            try {

                int numero = Integer.parseInt(
                        valor.toString().trim());

                if (numero < 0) {
                    throw new NumberFormatException();
                }

                int pid = Integer.parseInt(
                        getValueAt(FILA_PID, columna).toString());

                if (fila == FILA_LLEGADA) {
                    controlador.configurarLlegada(pid, numero);
                } else {
                    controlador.configurarPrioridad(pid, numero);
                }

                super.setValueAt(
                        Integer.toString(numero),
                        fila,
                        columna);

                actualizarDetalle();
                actualizarCronograma();

            } catch (RuntimeException ex) {

                JOptionPane.showMessageDialog(
                        MainWindow.this,
                        fila == FILA_PRIORIDAD
                                && controlador.getAlgoritmoSeleccionado() == TipoAlgoritmo.PRIORIDAD
                                ? "La prioridad debe ser un entero entre 1 y 10."
                                : campo + " debe ser un entero mayor o igual a cero.",
                        fila == FILA_LLEGADA
                                ? "Llegada inválida"
                                : "Prioridad inválida",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    };

    // =========================================================
    // TABLAS DE RECURSOS
    // =========================================================

    private final DefaultTableModel ram = modelo(
            "Dirección",
            "Área",
            "Contenido");

    private final DefaultTableModel almacenamiento = modelo(
            "Dirección",
            "Área",
            "Contenido");

    private final DefaultTableModel archivos = modelo(
            "Nombre",
            "Dirección",
            "Peso",
            "Espacio",
            "Tipo");

    private final JTable tablaTrabajos = new JTable(trabajos);

    private final JTable tablaRam = new JTable(ram);

    private final JTable tablaAlmacenamiento = new JTable(almacenamiento);

    private final JTable tablaArchivos = new JTable(archivos);

    // =========================================================
    // CRONOGRAMA DE EJECUCIÓN MULTINÚCLEO
    // =========================================================

    /** Una fila por proceso; cada columna representa un segundo simulado. */
    private final DefaultTableModel modeloCronograma = modelo("Núcleo", "Proceso");

    private final JTable tablaCronograma = new JTable(modeloCronograma);

    private final JLabel resumenCronograma = new JLabel("0 s registrados · 1 celda = 1 segundo");

    private final JCheckBox seguirCronograma = new JCheckBox("Seguir reloj", true);

    /** Segundo -> (CPU -> PID que consumió ese segundo). */
    private final Map<Integer, Map<Integer, Integer>> historialEjecucion = new HashMap<>();

    /** Vincula cada fila del cronograma con el PID real. */
    private final List<Integer> pidsCronograma = new ArrayList<>();

    // =========================================================
    // ESTADO DE LA INTERFAZ
    // =========================================================

    private final Timer reloj;

    private boolean continuarAutomatico;
    private boolean actualizando;

    private Integer seleccionado;

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    public MainWindow() {
        this(Paths.get("."));
    }

    public MainWindow(Path raiz) {

        super("Mini PC - Gestor de Procesos");

        this.raiz = raiz.toAbsolutePath().normalize();

        // Un tick automático por segundo real.
        this.reloj = new Timer(
                1000,
                e -> ejecutarTick());

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE);

        setMinimumSize(
                new Dimension(1180, 740));

        setSize(1550, 920);

        setLocationRelativeTo(null);

        getContentPane().setBackground(FONDO);

        // El usuario elige explícitamente el algoritmo en Configuración.
        algoritmos.setSelectedIndex(-1);

        configurarBotones();
        configurarTablas();
        configurarCronograma();
        construirInterfaz();
        registrarEventos();

        actualizar();

        aplicarConfiguracionInicial();
    }

    // =========================================================
    // CONSTRUCCION GENERAL
    // =========================================================

    private void construirInterfaz() {

        setLayout(new BorderLayout(10, 10));

        add(
                crearEncabezado(),
                BorderLayout.NORTH);

        JPanel principal = new JPanel(new BorderLayout(10, 10));

        principal.setOpaque(false);

        principal.setBorder(
                new EmptyBorder(0, 10, 10, 10));

        JSplitPane izquierdaCentro = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                crearPanelProcesos(),
                crearPanelCPUYBCP());

        configurarSplit(
                izquierdaCentro,
                0.59,
                610);

        JSplitPane contenido = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                izquierdaCentro,
                crearPanelRecursosConMonitor());

        configurarSplit(
                contenido,
                0.73,
                1050);

        /*
         * El cronograma puede ampliarse o reducirse arrastrando
         * el separador, sin ocultar los paneles existentes.
         */
        JSplitPane vistaCompleta = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                contenido,
                crearPanelCronograma());

        // Reservar menos altura al cronograma para aprovechar el área superior.
        configurarSplit(vistaCompleta, 0.82, 580);

        principal.add(vistaCompleta, BorderLayout.CENTER);

        add(
                principal,
                BorderLayout.CENTER);
    }

    // =========================================================
    // ENCABEZADO
    // =========================================================

    private JPanel crearEncabezado() {

        JPanel exterior = new JPanel(new BorderLayout(0, 8));

        exterior.setOpaque(false);

        exterior.setBorder(
                new EmptyBorder(10, 10, 0, 10));

        JPanel barra = new JPanel(new BorderLayout(18, 0));

        barra.setBackground(
                new Color(19, 35, 56));

        barra.setBorder(
                new EmptyBorder(16, 18, 16, 18));

        JLabel titulo = new JLabel(
                "MINI PC  /  CENTRO DE CONTROL");

        titulo.setForeground(Color.WHITE);

        titulo.setFont(
                titulo.getFont().deriveFont(
                        Font.BOLD,
                        19f));

        JLabel subtitulo = new JLabel(
                "Planificación de procesos · "
                        + "Ejecución multinúcleo");

        subtitulo.setForeground(
                new Color(205, 215, 225));

        subtitulo.setFont(
                subtitulo.getFont().deriveFont(
                        Font.PLAIN,
                        12f));

        JPanel identidad = new JPanel(new GridLayout(2, 1));

        identidad.setOpaque(false);

        identidad.add(titulo);
        identidad.add(subtitulo);

        JPanel acciones = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT,
                        5,
                        0));

        acciones.setOpaque(false);

        JButton configurar = new JButton("Configuración");

        JButton acerca = new JButton("Acerca del dispositivo");

        JButton reiniciar = new JButton("Reiniciar");

        aplicarBotonSecundario(configurar);
        aplicarBotonSecundario(acerca);
        aplicarBotonPrincipal(cargar);
        aplicarBotonSecundario(disco);
        aplicarBotonPeligro(reiniciar);

        conservar.setOpaque(false);
        conservar.setForeground(Color.WHITE);

        acciones.add(configurar);
        acciones.add(acerca);
        acciones.add(cargar);
        acciones.add(disco);
        acciones.add(reiniciar);
        acciones.add(conservar);

        barra.add(
                identidad,
                BorderLayout.WEST);

        JPanel relojCabecera = new JPanel(new GridLayout(2, 1, 0, 3));
        relojCabecera.setOpaque(false);

        valorReloj.setForeground(Color.WHITE);
        valorReloj.setFont(fuenteInterfaz(Font.BOLD, 23f));
        valorReloj.setHorizontalAlignment(SwingConstants.CENTER);

        algoritmoSuperior.setForeground(new Color(204, 221, 244));
        algoritmoSuperior.setFont(fuenteInterfaz(Font.BOLD, 11f));
        algoritmoSuperior.setHorizontalAlignment(SwingConstants.CENTER);

        relojCabecera.add(valorReloj);
        relojCabecera.add(algoritmoSuperior);

        barra.add(relojCabecera, BorderLayout.CENTER);

        barra.add(
                acciones,
                BorderLayout.EAST);

        exterior.add(
                barra,
                BorderLayout.NORTH);

        exterior.add(
                crearPanelEjecucion(),
                BorderLayout.SOUTH);

        configurar.addActionListener(
                e -> mostrarConfiguracion());

        acerca.addActionListener(
                e -> mostrarAcercaDispositivo());

        reiniciar.addActionListener(e -> {

            detener();

            if (controlador != null) {

                controlador.reiniciar(
                        conservar.isSelected());
            }

            nombres.clear();
            seleccionado = null;
            historialEjecucion.clear();
            cpuSeleccionada = 1;

            actualizar();
        });

        return exterior;
    }

    private JPanel crearTarjetaResumen(
            String titulo,
            JLabel valor,
            Color acento,
            JProgressBar barra) {

        JPanel panel = new JPanel(new BorderLayout(4, 6));

        panel.setBackground(PANEL);

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                0, 4, 0, 0, acento),
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(BORDE),
                                new EmptyBorder(10, 14, 10, 14))));

        JLabel etiqueta = new JLabel(titulo);

        etiqueta.setForeground(TEXTO_SECUNDARIO);

        etiqueta.setFont(
                etiqueta.getFont().deriveFont(
                        Font.BOLD,
                        11f));

        valor.setForeground(TEXTO);

        valor.setFont(
                valor.getFont().deriveFont(
                        Font.BOLD,
                        17f));

        panel.add(
                etiqueta,
                BorderLayout.NORTH);

        panel.add(
                valor,
                BorderLayout.CENTER);

        if (barra != null) {

            barra.setStringPainted(true);

            barra.setPreferredSize(
                    new Dimension(100, 15));

            panel.add(
                    barra,
                    BorderLayout.SOUTH);
        }

        return panel;
    }

    // =========================================================
    // VENTANAS DE CONFIGURACIÓN Y ACERCA DEL DISPOSITIVO
    // =========================================================

    /*
     * ¿Qué hace? Abre la ventana donde se eligen CPU, algoritmo, quantum, RAM
     * y disco. La RAM y el disco ya no salen de un TXT: se escriben aquí, se
     * validan como enteros y el kernel se calcula solo.
     * ¿Quién lo llama? El botón "Configurar" de la cabecera.
     */
    private void mostrarConfiguracion() {

        if (controlador == null) {
            JOptionPane.showMessageDialog(this,
                    "Primero debe cargar la configuración inicial.",
                    "Configuración", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        boolean editable = controlador.getTiempoSimulado() == 0
                && !continuarAutomatico;

        ajustandoControles = true;
        try {
            cantidadCPU.setValue(controlador.getCantidadCPU());
            algoritmos.setSelectedItem(controlador.getAlgoritmoSeleccionado());
            quantumRR.setValue(controlador.getQuantumRR());
        } finally {
            ajustandoControles = false;
        }

        cantidadCPU.setEnabled(editable);
        algoritmos.setEnabled(editable);
        quantumRR.setEnabled(editable);

        JSpinner ram = new JSpinner(new SpinnerNumberModel(
                configuracion.getTamañoMemoria(),
                Configuracion.RAM_MINIMA,
                1_000_000,
                1));
        JSpinner almacenamiento = new JSpinner(new SpinnerNumberModel(
                configuracion.getTamañoAlmacenamiento(),
                Configuracion.ALMACENAMIENTO_MINIMO,
                1_000_000,
                1));

        ram.setEditor(new JSpinner.NumberEditor(ram, "0"));
        almacenamiento.setEditor(new JSpinner.NumberEditor(almacenamiento, "0"));
        ram.setEnabled(editable);
        almacenamiento.setEnabled(editable);
        JSpinner tasaA = new JSpinner(new SpinnerNumberModel(
                controlador.getTasaEsperaSRR(), 1, 100000, 1));
        JSpinner tasaB = new JSpinner(new SpinnerNumberModel(
                controlador.getTasaServicioSRR(), 1, 100000, 1));
        tasaA.setEnabled(editable);
        tasaB.setEnabled(editable);
        JCheckBox expropiativo = new JCheckBox("Permitir expropiación",
                controlador.isPrioridadExpropiativa());
        expropiativo.setEnabled(editable);
        expropiativo.setOpaque(false);

        JDialog ventana = new JDialog(this, "Configuración de la Mini PC", true);
        ventana.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        ventana.setLayout(new BorderLayout(12, 12));

        JPanel contenido = new JPanel(new BorderLayout(12, 14));
        contenido.setBackground(FONDO);
        contenido.setBorder(new EmptyBorder(18, 18, 8, 18));

        JLabel titulo = new JLabel("Configuración del procesador y los recursos");
        titulo.setFont(fuenteInterfaz(Font.BOLD, 18f));
        titulo.setForeground(TEXTO);
        contenido.add(titulo, BorderLayout.NORTH);

        JPanel formulario = new JPanel();
        formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        formulario.setOpaque(false);

        formulario.add(filaConfiguracion("Cantidad de CPU (1 a 5):", cantidadCPU));
        formulario.add(filaConfiguracion("Algoritmo de planificación:", algoritmos));
        JPanel filaModo = filaConfiguracion("Modo del algoritmo PRIORIDAD:", expropiativo);
        JPanel filaQuantum = filaConfiguracion("Quantum RR/SRR (segundos):", quantumRR);
        JPanel filaA = filaConfiguracion("SRR tasa espera a:", tasaA);
        JPanel filaB = filaConfiguracion("SRR tasa servicio b:", tasaB);
        formulario.add(filaModo);
        formulario.add(filaQuantum);
        formulario.add(filaA);
        formulario.add(filaB);
        formulario.add(filaConfiguracion("RAM total (mínimo 500):", ram));
        formulario.add(filaConfiguracion("Almacenamiento (mínimo 500):", almacenamiento));
        Runnable mostrarOpciones = () -> {
            TipoAlgoritmo seleccionado = (TipoAlgoritmo) algoritmos.getSelectedItem();
            filaModo.setVisible(seleccionado == TipoAlgoritmo.PRIORIDAD);
            filaQuantum.setVisible(seleccionado == TipoAlgoritmo.RR
                    || seleccionado == TipoAlgoritmo.SELFISH_ROUND_ROBIN);
            filaA.setVisible(seleccionado == TipoAlgoritmo.SELFISH_ROUND_ROBIN);
            filaB.setVisible(seleccionado == TipoAlgoritmo.SELFISH_ROUND_ROBIN);
            formulario.revalidate();
            formulario.repaint();
        };
        java.awt.event.ActionListener cambioAlgoritmo = e -> mostrarOpciones.run();
        algoritmos.addActionListener(cambioAlgoritmo);
        mostrarOpciones.run();

        JPanel centro = new JPanel(new BorderLayout(4, 12));
        centro.setOpaque(false);
        centro.add(formulario, BorderLayout.NORTH);

        JLabel vistaPrevia = new JLabel(" ");
        vistaPrevia.setForeground(TEXTO_SECUNDARIO);

        JLabel ayuda = new JLabel("<html><div style='width:420px'>"
                + (editable
                        ? "Los cambios se guardan al presionar Aplicar. "
                        : "La simulación ya comenzó: no se permiten cambios. ")
                + "La RAM y el disco se escriben aquí; el kernel se calcula "
                + "automáticamente y siempre deja 128 posiciones para las "
                + "instrucciones.</div></html>");
        ayuda.setForeground(TEXTO_SECUNDARIO);

        JPanel informacion = new JPanel(new GridLayout(2, 1, 0, 6));
        informacion.setOpaque(false);
        informacion.add(vistaPrevia);
        informacion.add(ayuda);

        centro.add(informacion, BorderLayout.CENTER);

        contenido.add(centro, BorderLayout.CENTER);

        javax.swing.event.ChangeListener escucharCambio =
                e -> actualizarVistaPrevia(vistaPrevia, ram, cantidadCPU);
        ram.addChangeListener(escucharCambio);
        cantidadCPU.addChangeListener(escucharCambio);
        actualizarVistaPrevia(vistaPrevia, ram, cantidadCPU);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botones.setOpaque(false);

        JButton cancelar = new JButton("Cancelar");
        JButton aplicar = new JButton("Aplicar");
        aplicarBotonPrincipal(aplicar);

        aplicar.setEnabled(editable);

        cancelar.addActionListener(e -> {
            ventana.dispose();
            actualizar();
        });

        aplicar.addActionListener(e -> {
            try {
                TipoAlgoritmo seleccionadoAlgoritmo =
                        (TipoAlgoritmo) algoritmos.getSelectedItem();

                if (seleccionadoAlgoritmo == null) {
                    throw new IllegalArgumentException(
                            "Seleccione un algoritmo antes de continuar.");
                }

                int ramValor = leerEntero(ram, "La RAM");
                int discoValor = leerEntero(almacenamiento,
                        "El almacenamiento");
                int cpus = ((Number) cantidadCPU.getValue()).intValue();
                int quantum = ((Number) quantumRR.getValue()).intValue();
                int a = seleccionadoAlgoritmo == TipoAlgoritmo.SELFISH_ROUND_ROBIN
                        ? leerEntero(tasaA, "La tasa a") : controlador.getTasaEsperaSRR();
                int b = seleccionadoAlgoritmo == TipoAlgoritmo.SELFISH_ROUND_ROBIN
                        ? leerEntero(tasaB, "La tasa b") : controlador.getTasaServicioSRR();
                if (b <= 0 || a <= b) {
                    throw new IllegalArgumentException("Las tasas SRR deben cumplir 0 < b < a.");
                }

                Configuracion.validarRecursos(ramValor, discoValor, cpus);

                Configuracion nueva = Configuracion.paraInterfaz(
                        ramValor,
                        discoValor,
                        cpus,
                        seleccionadoAlgoritmo.name(),
                        conservar.isSelected());

                boolean cambiaHardware = ramValor != configuracion.getTamañoMemoria()
                        || discoValor != configuracion.getTamañoAlmacenamiento()
                        || cpus != controlador.getCantidadCPU();

                if (cambiaHardware && hayProcesosOArchivos()
                        && JOptionPane.showConfirmDialog(
                                ventana,
                                "Cambiar RAM, disco o CPU reinicia la "
                                        + "simulación y borra los procesos y "
                                        + "archivos simulados. ¿Continuar?",
                                "Cambiar configuración",
                                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    return;
                }

                if (cambiaHardware || controlador == null) {
                    aplicarConfiguracion(nueva, cpus,
                            seleccionadoAlgoritmo, quantum);
                } else {
                    controlador.configurarQuantumRR(quantum);
                    controlador.configurarAlgoritmo(seleccionadoAlgoritmo);
                    configuracion = nueva;
                    actualizar();
                }
                controlador.configurarTasasSRR(a, b);
                controlador.configurarExpropiacionPrioridad(expropiativo.isSelected());
                actualizar();

                ventana.dispose();

            } catch (RuntimeException error) {
                mostrarError(error);
                actualizar();
            }
        });

        botones.add(cancelar);
        botones.add(aplicar);

        ventana.add(contenido, BorderLayout.CENTER);
        ventana.add(botones, BorderLayout.SOUTH);
        ventana.setSize(620, 610);
        ventana.setLocationRelativeTo(this);
        ventana.setVisible(true);

        cantidadCPU.removeChangeListener(escucharCambio);
        algoritmos.removeActionListener(cambioAlgoritmo);
    }

    /* ¿Qué hace? Ordena una etiqueta con su control. ¿Quién lo llama? La ventana de configuración. */
    private JPanel filaConfiguracion(String nombre, Component control) {
        JPanel fila = new JPanel(new GridLayout(1, 2, 10, 0));
        fila.setOpaque(false);
        fila.setBorder(new EmptyBorder(0, 0, 10, 0));
        fila.add(new JLabel(nombre));
        fila.add(control);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return fila;
    }

    /*
     * ¿Qué hace? Pinta cuánto kernel, cuántos BCP y cuántas instrucciones
     * quedan con la RAM y las CPU que hay ahora en la ventana.
     * ¿Quién lo llama? Los cambios de RAM/CPU dentro de Configuración.
     */
    private void actualizarVistaPrevia(JLabel etiqueta, JSpinner ram,
            JSpinner cpus) {

        try {
            int ramValor = ((Number) ram.getValue()).intValue();
            int cpusValor = ((Number) cpus.getValue()).intValue();

            int kernel = Configuracion.calcularKernel(ramValor, cpusValor);
            int bcp = kernel / BCP.getTamañoKernel();
            int instrucciones = ramValor - kernel;
            int bcpDeseados = Configuracion.PROCESOS_POR_CPU * cpusValor;
            int ramParaTodos = bcpDeseados * BCP.getTamañoKernel()
                    + Configuracion.ESPACIO_INSTRUCCIONES_MINIMO;

            String aviso = bcp < bcpDeseados
                    ? " · para " + bcpDeseados + " BCP necesitas "
                            + ramParaTodos + " de RAM"
                    : "";

            etiqueta.setText("<html>Kernel: " + kernel
                    + " posiciones · BCP: " + bcp + " de " + bcpDeseados
                    + " · Instrucciones: " + instrucciones
                    + aviso + "</html>");
            etiqueta.setForeground(TEXTO_SECUNDARIO);

        } catch (RuntimeException error) {
            etiqueta.setText(error.getMessage());
            etiqueta.setForeground(ROJO);
        }
    }

    /*
     * ¿Qué hace? Obliga al spinner a soltar un entero válido; si el usuario
     * escribió basura o un valor fuera del mínimo, se lanza el error.
     * ¿Quién lo llama? El botón Aplicar de Configuración.
     */
    private int leerEntero(JSpinner spinner, String nombre) {

        String texto = "";

        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            texto = editor.getTextField().getText().trim();
        }

        int numero;

        try {
            numero = Integer.parseInt(texto);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    nombre + " debe ser un número entero.");
        }

        SpinnerNumberModel modelo = (SpinnerNumberModel) spinner.getModel();
        int minimo = ((Number) modelo.getMinimum()).intValue();
        int maximo = ((Number) modelo.getMaximum()).intValue();

        if (numero < minimo) {
            throw new IllegalArgumentException(
                    nombre + " debe ser un entero mayor o igual a "
                            + minimo + ".");
        }

        if (numero > maximo) {
            throw new IllegalArgumentException(
                    nombre + " debe ser un entero menor o igual a "
                            + maximo + ".");
        }

        try {
            spinner.commitEdit();
        } catch (java.text.ParseException error) {
            throw new IllegalArgumentException(
                    nombre + " debe ser un número entero.");
        }

        return numero;
    }

    /*
     * ¿Qué hace? Dice si ya hay procesos cargados o archivos en el disco
     * simulado, para avisar antes de tirar la simulación.
     * ¿Quién lo llama? El botón Aplicar de Configuración.
     */
    private boolean hayProcesosOArchivos() {

        return controlador != null
                && (!controlador.getGestorProcesos()
                        .getListaTrabajos().isEmpty()
                        || controlador.getAlmacenamiento()
                                .getEspacioArchivosOcupado() > 0);
    }

    /*
     * ¿Qué hace? Arranca la Mini PC con una configuración por defecto sin leer
     * ningún TXT: RAM y disco en el mínimo, una CPU y sin algoritmo elegido.
     * ¿Quién lo llama? El constructor de la ventana principal.
     */
    private void aplicarConfiguracionInicial() {

        Configuracion inicial = Configuracion.paraInterfaz(
                Configuracion.RAM_POR_DEFECTO,
                Configuracion.ALMACENAMIENTO_POR_DEFECTO,
                1,
                "FCFS",
                true);

        configuracion = inicial;
        controlador = new Controlador(inicial);

        conservar.setSelected(true);

        actualizar();
    }

    /*
     * ¿Qué hace? Tira el controlador viejo y crea uno nuevo con la RAM, el
     * disco y las CPU elegidas; la memoria y el disco no se pueden
     * redimensionar en caliente.
     * ¿Quién lo llama? Aplicar de Configuración cuando cambian los recursos.
     */
    private void aplicarConfiguracion(Configuracion nueva, int cpus,
            TipoAlgoritmo algoritmo, int quantum) {

        detener();

        Controlador nuevo = new Controlador(nueva);
        nuevo.configurarQuantumRR(quantum);
        nuevo.configurarCantidadCPU(cpus);
        nuevo.configurarAlgoritmo(algoritmo);

        configuracion = nueva;
        controlador = nuevo;

        conservar.setSelected(nueva.isConservarArchivosAlReiniciar());

        nombres.clear();
        seleccionado = null;
        historialEjecucion.clear();
        cpuSeleccionada = 1;

        actualizar();
    }

    /** Muestra las características y los recursos del dispositivo. */
    private void mostrarAcercaDispositivo() {

        actualizar();

        JDialog ventana = new JDialog(this, "Acerca del dispositivo", true);
        ventana.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        ventana.setLayout(new BorderLayout(12, 12));

        JPanel contenido = new JPanel(new BorderLayout(12, 12));
        contenido.setBackground(FONDO);
        contenido.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel titulo = new JLabel("Mini PC · Información del dispositivo");
        titulo.setFont(fuenteInterfaz(Font.BOLD, 19f));
        titulo.setForeground(TEXTO);

        JLabel subtitulo = new JLabel(
                "Estado actual del procesador, la memoria y el almacenamiento.");
        subtitulo.setForeground(TEXTO_SECUNDARIO);

        JPanel cabecera = new JPanel(new GridLayout(2, 1, 0, 5));
        cabecera.setOpaque(false);
        cabecera.add(titulo);
        cabecera.add(subtitulo);
        contenido.add(cabecera, BorderLayout.NORTH);

        JPanel tarjetas = new JPanel(new GridLayout(2, 3, 12, 12));
        tarjetas.setOpaque(false);

        tarjetas.add(crearTarjetaResumen("CPU CONFIGURADAS", valorNucleos, VERDE, null));
        tarjetas.add(crearTarjetaResumen("RAM USUARIO", valorRAM, AZUL, barraRAM));
        tarjetas.add(crearTarjetaResumen("KERNEL / SO", valorKernel, MORADO, null));
        tarjetas.add(crearTarjetaResumen("PROGRAMAS EN RAM", valorProgramasEnRAM, VERDE, null));
        tarjetas.add(crearTarjetaResumen("DISCO", valorDisco, NARANJA, barraDisco));

        JLabel algoritmo = new JLabel(controlador == null
                || controlador.getAlgoritmoSeleccionado() == null
                        ? "Sin seleccionar"
                        : controlador.getAlgoritmoSeleccionado().name());
        tarjetas.add(crearTarjetaResumen("ALGORITMO", algoritmo, AZUL, null));

        contenido.add(tarjetas, BorderLayout.CENTER);

        JButton cerrar = new JButton("Cerrar");
        aplicarBotonPrincipal(cerrar);
        cerrar.addActionListener(e -> ventana.dispose());

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pie.setOpaque(false);
        pie.add(cerrar);

        ventana.add(contenido, BorderLayout.CENTER);
        ventana.add(pie, BorderLayout.SOUTH);
        ventana.setSize(760, 395);
        ventana.setLocationRelativeTo(this);
        ventana.setVisible(true);
    }

    // =========================================================
    // PROCESOS Y BCP
    // =========================================================

    private JPanel crearPanelProcesos() {

        JPanel panel = panelSeccion("Procesos · Vista general");

        contadorProcesos.setForeground(TEXTO_SECUNDARIO);
        contadorProcesos.setFont(fuenteInterfaz(Font.PLAIN, 11f));
        panel.add(contadorProcesos, BorderLayout.NORTH);

        contenedorTarjetas.setLayout(new BoxLayout(contenedorTarjetas, BoxLayout.Y_AXIS));
        contenedorTarjetas.setBackground(new Color(247, 249, 252));
        contenedorTarjetas.setBorder(new EmptyBorder(8, 8, 8, 8));

        scrollTarjetas.setBorder(BorderFactory.createLineBorder(BORDE));
        scrollTarjetas.getVerticalScrollBar().setUnitIncrement(24);
        scrollTarjetas.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        contenedorFinalizados.setLayout(new BoxLayout(contenedorFinalizados, BoxLayout.Y_AXIS));
        contenedorFinalizados.setBackground(new Color(247, 249, 252));
        contenedorFinalizados.setBorder(new EmptyBorder(8, 8, 8, 8));
        scrollFinalizados.setBorder(BorderFactory.createLineBorder(BORDE));
        scrollFinalizados.getVerticalScrollBar().setUnitIncrement(24);
        scrollFinalizados.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JTabbedPane estados = new JTabbedPane();
        estados.addTab("En curso", scrollTarjetas);
        estados.addTab("Finalizados", scrollFinalizados);
        panel.add(estados, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Ordena visualmente los procesos por estado y luego por PID.
     * Los nuevos aparecen primero y los finalizados al final.
     */
    private static int ordenEstado(EstadoProceso estado) {
        return switch (estado) {
            case NUEVO -> 0;
            case PREPARADO -> 1;
            case EJECUCION -> 2;
            case BLOQUEADO -> 3;
            case PREPARADO_SUSPENDIDO -> 4;
            case BLOQUEADO_SUSPENDIDO -> 5;
            case FINALIZADO -> 6;
        };
    }

    /** Obtiene el título legible de cada agrupación de procesos. */
    private static String nombreGrupo(EstadoProceso estado) {
        return switch (estado) {
            case NUEVO -> "Nuevos";
            case PREPARADO -> "Preparados";
            case EJECUCION -> "En ejecución";
            case BLOQUEADO -> "Bloqueados";
            case PREPARADO_SUSPENDIDO -> "Preparados suspendidos";
            case BLOQUEADO_SUSPENDIDO -> "Bloqueados suspendidos";
            case FINALIZADO -> "Finalizados";
        };
    }

    /**
     * Construye los grupos de tarjetas en orden de estado.
     * Cada grupo coloca sus procesos de izquierda a derecha.
     */
    private void actualizarTarjetasProcesos() {

        int desplazamiento = scrollTarjetas.getVerticalScrollBar().getValue();

        contenedorTarjetas.removeAll();
        contenedorFinalizados.removeAll();

        if (controlador == null) {
            contadorProcesos.setText("Sin procesos cargados");
            contenedorTarjetas.revalidate();
            contenedorTarjetas.repaint();
            contenedorFinalizados.revalidate();
            contenedorFinalizados.repaint();
            return;
        }

        java.util.List<BCP> procesos = new java.util.ArrayList<>(
                controlador.getGestorProcesos().getListaTrabajos());

        procesos.sort((a, b) -> {
            int comparacion = Integer.compare(ordenEstado(a.getEstado()),
                    ordenEstado(b.getEstado()));
            return comparacion != 0
                    ? comparacion : Integer.compare(a.getPid(), b.getPid());
        });

        int visibles = 0;
        int finalizados = 0;

        for (BCP p : procesos) {
            if (p.getEstado() == EstadoProceso.FINALIZADO) {
                finalizados++;
            }
        }

        for (EstadoProceso estado : new EstadoProceso[] {
                EstadoProceso.NUEVO,
                EstadoProceso.PREPARADO,
                EstadoProceso.EJECUCION,
                EstadoProceso.BLOQUEADO,
                EstadoProceso.PREPARADO_SUSPENDIDO,
                EstadoProceso.BLOQUEADO_SUSPENDIDO,
                EstadoProceso.FINALIZADO
        }) {

            java.util.List<BCP> grupo = new java.util.ArrayList<>();

            for (BCP proceso : procesos) {
                if (proceso.getEstado() != estado) {
                    continue;
                }

                grupo.add(proceso);
            }

            if (grupo.isEmpty()) {
                continue;
            }

            visibles += grupo.size();

            JPanel seccion = new JPanel(new BorderLayout(0, 7));
            seccion.setOpaque(false);
            seccion.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel titulo = new JLabel(nombreGrupo(estado)
                    + "  ·  " + grupo.size());
            titulo.setForeground(estado == EstadoProceso.FINALIZADO
                    ? TEXTO_SECUNDARIO : TEXTO);
            titulo.setFont(fuenteInterfaz(Font.BOLD, 13f));
            titulo.setBorder(new EmptyBorder(2, 2, 3, 0));

            JPanel tarjetas = new JPanel(new GridLayout(0, 2, 10, 10));
            tarjetas.setOpaque(false);

            for (BCP proceso : grupo) {
                tarjetas.add(crearTarjetaProceso(proceso));
            }

            seccion.add(titulo, BorderLayout.NORTH);
            seccion.add(tarjetas, BorderLayout.CENTER);
            seccion.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    seccion.getPreferredSize().height));

            JPanel destino = estado == EstadoProceso.FINALIZADO
                    ? contenedorFinalizados : contenedorTarjetas;
            destino.add(seccion);
            destino.add(Box.createVerticalStrut(12));
        }

        contadorProcesos.setText(procesos.size() + " procesos · "
                + visibles + " visibles · " + finalizados + " finalizados");

        if (visibles == 0) {
            JLabel vacio = new JLabel("No hay procesos cargados.");
            vacio.setForeground(TEXTO_SECUNDARIO);
            vacio.setBorder(new EmptyBorder(15, 10, 15, 10));
            vacio.setAlignmentX(Component.LEFT_ALIGNMENT);
            contenedorTarjetas.add(vacio);
        }

        contenedorTarjetas.revalidate();
        contenedorTarjetas.repaint();
        contenedorFinalizados.revalidate();
        contenedorFinalizados.repaint();

        SwingUtilities.invokeLater(() -> scrollTarjetas.getVerticalScrollBar()
                .setValue(desplazamiento));
    }

    /**
     * Crea una tarjeta compacta con PID, CPU, estado, ráfaga y llegada.
     * Incluye acceso al BCP sin desplazar horizontalmente una tabla.
     */
    /* ¿Qué hace? Dice si el algoritmo usa el campo de prioridad. ¿Quién lo llama? Las vistas de procesos. */
    private boolean usaPrioridad() {
        if (controlador == null) return false;
        TipoAlgoritmo tipo = controlador.getAlgoritmoSeleccionado();
        return tipo == TipoAlgoritmo.PRIORIDAD || tipo == TipoAlgoritmo.SELFISH_ROUND_ROBIN;
    }

    /* ¿Qué hace? Pone nombre visible al campo de una tarjeta. ¿Quién lo llama? crearTarjetaProceso(). */
    private JPanel filaDatoProceso(String texto, JSpinner valor) {
        JPanel fila = new JPanel(new BorderLayout(4, 0));
        fila.setOpaque(false);
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuenteInterfaz(Font.PLAIN, 11f));
        fila.add(etiqueta, BorderLayout.WEST);
        fila.add(valor, BorderLayout.CENTER);
        return fila;
    }

    private JPanel crearTarjetaProceso(BCP proceso) {

        int cpuId = proceso.getCpuAsignada();
        boolean seleccionadoAhora = seleccionado != null
                && seleccionado == proceso.getPid();

        JPanel tarjeta = new JPanel(new BorderLayout(5, 8));
        tarjeta.setBackground(seleccionadoAhora
                ? fondoCPU(cpuId) : PANEL);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 5, 1, 1, colorCPU(cpuId)),
                new EmptyBorder(9, 10, 9, 10)));
        tarjeta.setPreferredSize(new Dimension(210, 185));

        JPanel superior = new JPanel(new BorderLayout(4, 0));
        superior.setOpaque(false);

        JLabel pid = new JLabel("PID " + proceso.getPid());
        pid.setFont(fuenteInterfaz(Font.BOLD, 15f));
        pid.setForeground(TEXTO);

        JLabel cpu = new JLabel(cpuId < 1 ? "Sin CPU" : "CPU " + cpuId);
        cpu.setFont(fuenteInterfaz(Font.BOLD, 11f));
        cpu.setForeground(colorCPU(cpuId));

        superior.add(pid, BorderLayout.WEST);
        superior.add(cpu, BorderLayout.EAST);

        JPanel informacion = new JPanel(new GridLayout(3, 1, 0, 3));
        informacion.setOpaque(false);

        String nombre = nombres.getOrDefault(proceso.getPid(),
                "Programa " + proceso.getPid());
        JLabel programa = new JLabel(nombre);
        programa.setToolTipText(nombre);
        programa.setForeground(TEXTO);
        programa.setFont(fuenteInterfaz(Font.BOLD, 12f));

        JLabel estado = new JLabel(nombreGrupo(proceso.getEstado())
                + "  ·  Restante: " + proceso.getTiempoRestante() + " s");
        estado.setForeground(TEXTO_SECUNDARIO);
        estado.setFont(fuenteInterfaz(Font.PLAIN, 11f));

        JProgressBar avance = new JProgressBar(0, Math.max(1, proceso.getRafaga()));
        avance.setValue(Math.min(proceso.getTiempoCPU(), avance.getMaximum()));
        avance.setForeground(colorCPU(cpuId));
        avance.setBackground(GRIS_SUAVE);
        avance.setPreferredSize(new Dimension(100, 8));

        informacion.add(programa);
        informacion.add(estado);
        informacion.add(avance);

        JPanel inferior = new JPanel(new BorderLayout(5, 0));
        inferior.setOpaque(false);

        boolean editable = controlador.getTiempoSimulado() == 0
                && proceso.getEstado() == EstadoProceso.NUEVO
                && !continuarAutomatico;

        boolean usaPrioridad = usaPrioridad();
        JPanel datosEdicion = new JPanel(new GridLayout(usaPrioridad ? 2 : 1, 1, 0, 3));
        datosEdicion.setOpaque(false);

        if (editable) {
            JSpinner llegada = new JSpinner(new SpinnerNumberModel(
                    proceso.getTiempoLlegada(), 0, 100000, 1));
            llegada.setToolTipText("Editar llegada de este proceso");
            llegada.setPreferredSize(new Dimension(90, 29));
            llegada.addChangeListener(e -> {
                try {
                    controlador.configurarLlegada(proceso.getPid(),
                            (Integer) llegada.getValue());
                    actualizarDetalle();
                } catch (RuntimeException error) {
                    mostrarError(error);
                }
            });

            datosEdicion.add(filaDatoProceso("Llegada:", llegada));

            if (usaPrioridad) {
                boolean prioridadEstatica = controlador.getAlgoritmoSeleccionado() == TipoAlgoritmo.PRIORIDAD;
                JSpinner prioridad = new JSpinner(new SpinnerNumberModel(
                    proceso.getPrioridad(), prioridadEstatica ? 1 : 0,
                    prioridadEstatica ? 10 : 100000, 1));
                prioridad.setToolTipText(prioridadEstatica
                    ? "Prioridad de 1 (mayor) a 10 (menor)"
                    : "Editar prioridad de este proceso");
                prioridad.setPreferredSize(new Dimension(70, 29));
                prioridad.addChangeListener(e -> {
                try {
                    controlador.configurarPrioridad(proceso.getPid(),
                            (Integer) prioridad.getValue());
                    actualizarDetalle();
                } catch (RuntimeException error) {
                    mostrarError(error);
                }
                });
                datosEdicion.add(filaDatoProceso("Prioridad:", prioridad));
            }
        } else {
            JLabel llegada = new JLabel("Llegada: "
                    + proceso.getTiempoLlegada() + " s");
            llegada.setFont(fuenteInterfaz(Font.PLAIN, 11f));
            llegada.setForeground(TEXTO_SECUNDARIO);

            JLabel prioridad = new JLabel("Prioridad: "
                    + proceso.getPrioridad());
            prioridad.setFont(fuenteInterfaz(Font.PLAIN, 11f));
            prioridad.setForeground(TEXTO_SECUNDARIO);

            datosEdicion.add(llegada);
            if (usaPrioridad) datosEdicion.add(prioridad);
        }

        inferior.add(datosEdicion, BorderLayout.WEST);

        JButton consultar = new JButton("Ver BCP");
        consultar.setFont(fuenteInterfaz(Font.BOLD, 11f));
        consultar.addActionListener(e -> seleccionarProcesoCronograma(proceso.getPid()));
        inferior.add(consultar, BorderLayout.EAST);

        tarjeta.add(superior, BorderLayout.NORTH);
        tarjeta.add(informacion, BorderLayout.CENTER);
        tarjeta.add(inferior, BorderLayout.SOUTH);

        return tarjeta;
    }

    // =========================================================
    // CRONOGRAMA DE EJECUCIÓN
    // =========================================================

    /**
     * Construye la línea de tiempo solicitada: núcleo, proceso
     * y una columna por segundo simulado.
     *
     * Una CPU puede aparecer en varias filas si tiene varios
     * procesos asignados. Sus celdas conservan el mismo color.
     */
    private JPanel crearPanelCronograma() {

        JPanel panel = panelSeccion("Cronograma de ejecución · 1 columna = 1 segundo");

        JPanel encabezado = new JPanel(new BorderLayout(10, 0));
        encabezado.setOpaque(false);

        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leyenda.setOpaque(false);

        for (int cpuId = 1; cpuId <= COLORES_CPU.length; cpuId++) {
            JLabel indicador = new JLabel("■ CPU " + cpuId);
            indicador.setForeground(colorCPU(cpuId));
            indicador.setFont(fuenteInterfaz(Font.BOLD, 11f));
            leyenda.add(indicador);
        }

        encabezado.add(leyenda, BorderLayout.CENTER);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controles.setOpaque(false);
        seguirCronograma.setOpaque(false);

        JButton volverInicio = new JButton("Inicio");
        volverInicio.addActionListener(e -> desplazarCronograma(1));

        JButton verActual = new JButton("Último segundo");
        verActual.addActionListener(e -> desplazarCronograma(
                controlador == null ? 1 : controlador.getTiempoSimulado()));

        controles.add(resumenCronograma);
        controles.add(seguirCronograma);
        controles.add(volverInicio);
        controles.add(verActual);

        encabezado.add(controles, BorderLayout.EAST);
        panel.add(encabezado, BorderLayout.NORTH);

        JScrollPane desplazamiento = new JScrollPane(tablaCronograma);
        desplazamiento.setBorder(BorderFactory.createLineBorder(BORDE));
        desplazamiento.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        panel.add(desplazamiento, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================
    // CPU COMPACTA Y BCP
    // =========================================================

    private JPanel crearPanelCPUYBCP() {

        JPanel panel = new JPanel(new BorderLayout(8, 8));

        panel.setOpaque(false);

        JPanel superior = new JPanel(new BorderLayout(8, 8));

        superior.setOpaque(false);

        superior.setMinimumSize(
                new Dimension(220, 208));

        superior.add(
                crearPanelCPU(),
                BorderLayout.CENTER);

        JPanel inferior = new JPanel(new BorderLayout(8, 8));

        inferior.setOpaque(false);

        inferior.setMinimumSize(
                new Dimension(220, 180));

        inferior.add(
                crearPanelBCP(),
                BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                superior,
                inferior);

        configurarSplit(
                division,
                0.40,
                240);

        panel.add(
                division,
                BorderLayout.CENTER);

        return panel;
    }

    /** El BCP usa tres pestañas y ocupa el espacio bajo la CPU. */
    private JPanel crearPanelBCP() {
        JPanel panel = panelSeccion("BCP · Información del proceso seleccionado");
        pestañasBCP.setFont(fuenteInterfaz(Font.BOLD, 11f));
        panel.add(pestañasBCP, BorderLayout.CENTER);
        return panel;
    }

    /** Sitúa la consola debajo de las pestañas RAM, Disco y Archivos. */
    private JPanel crearPanelRecursosConMonitor() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                crearPanelRecursos(), crearPanelConsola());
        configurarSplit(division, 0.60, 335);
        panel.add(division, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelCPU() {

        JPanel panel = panelSeccion(
                "Procesadores · Estado en tiempo real");

        procesoCPU.setFont(
                procesoCPU.getFont().deriveFont(
                        Font.BOLD,
                        15f));

        procesoCPU.setForeground(AZUL);

        procesoCPU.setHorizontalAlignment(
                SwingConstants.LEFT);

        procesoCPU.setBorder(
                new EmptyBorder(8, 2, 7, 2));

        /*
         * Una sola vista reúne el PID, los contadores de ocupación
         * y los registros de la CPU seleccionada.
         */
        JPanel superior = new JPanel(new BorderLayout(8, 0));
        superior.setOpaque(false);
        superior.add(procesoCPU, BorderLayout.CENTER);

        JPanel navegacion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        navegacion.setOpaque(false);

        cpuAnterior.setToolTipText("Mostrar la CPU anterior");
        cpuSiguiente.setToolTipText("Mostrar la CPU siguiente");
        cpuAnterior.setFocusable(false);
        cpuSiguiente.setFocusable(false);
        aplicarBotonSecundario(cpuAnterior);
        aplicarBotonSecundario(cpuSiguiente);

        navegacion.add(cpuAnterior);
        navegacion.add(cpuSiguiente);
        superior.add(navegacion, BorderLayout.EAST);

        JPanel registros = new JPanel(new GridLayout(3, 4, 5, 5));

        registros.setOpaque(false);

        registros.setBorder(
                new EmptyBorder(2, 4, 2, 4));

        registros.add(
                crearCeldaRegistro(
                        "PC", valorPC,
                        AZUL_SUAVE, AZUL));

        registros.add(
                crearCeldaRegistro(
                        "IR", valorIR,
                        MORADO_SUAVE, MORADO));

        registros.add(
                crearCeldaRegistro(
                        "AC", valorAC,
                        VERDE_SUAVE, VERDE));

        registros.add(
                crearCeldaRegistro(
                        "ZF", valorZF,
                        GRIS_SUAVE, TEXTO_SECUNDARIO));

        registros.add(
                crearCeldaRegistro(
                        "AX", valorAX,
                        PANEL, BORDE));

        registros.add(
                crearCeldaRegistro(
                        "BX", valorBX,
                        PANEL, BORDE));

        registros.add(
                crearCeldaRegistro(
                        "CX", valorCX,
                        PANEL, BORDE));

        registros.add(
                crearCeldaRegistro(
                        "DX", valorDX,
                        PANEL, BORDE));

        registros.add(
                crearCeldaRegistro(
                        "AH", valorAH,
                        PANEL, BORDE));

        registros.add(
                crearCeldaRegistro(
                        "AL", valorAL,
                        PANEL, BORDE));

        registros.add(
                crearCeldaRegistro(
                        "OCUPADA", tiempoOcupadoCPU,
                        VERDE_SUAVE, VERDE));

        registros.add(
                crearCeldaRegistro(
                        "OCIOSA", tiempoOciosoCPU,
                        GRIS_SUAVE, TEXTO_SECUNDARIO));

        panel.add(superior, BorderLayout.NORTH);
        panel.add(registros, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearCeldaRegistro(
            String nombre,
            JLabel valor,
            Color fondo,
            Color borde) {

        JPanel celda = new JPanel(new BorderLayout(2, 2));

        celda.setBackground(fondo);

        celda.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borde),
                        new EmptyBorder(3, 6, 3, 6)));

        JLabel etiqueta = new JLabel(nombre);

        etiqueta.setForeground(TEXTO_SECUNDARIO);

        etiqueta.setFont(
                etiqueta.getFont().deriveFont(
                        Font.BOLD,
                        9f));

        valor.setForeground(TEXTO);

        valor.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.BOLD,
                        12));

        celda.add(
                etiqueta,
                BorderLayout.NORTH);

        celda.add(
                valor,
                BorderLayout.CENTER);

        return celda;
    }

    // =========================================================
    // CONTROLES DE EJECUCION
    // =========================================================

    private JPanel crearPanelEjecucion() {

        JPanel panel = new JPanel(new BorderLayout(14, 0));

        panel.setBackground(PANEL);

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        new EmptyBorder(9, 14, 9, 14)));

        JPanel botones = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT,
                        8,
                        0));

        botones.setOpaque(false);

        aplicarBotonPrincipal(siguiente);
        aplicarBotonExito(automatico);
        aplicarBotonSecundario(pausa);

        siguiente.setText("Siguiente (+1 s)");
        automatico.setText("Ejecutar");

        botones.add(siguiente);
        botones.add(automatico);
        botones.add(pausa);

        estadoSimulacion.setForeground(
                TEXTO_SECUNDARIO);

        estadoSimulacion.setFont(
                fuenteInterfaz(Font.PLAIN, 11f));

        panel.add(
                estadoSimulacion,
                BorderLayout.WEST);

        panel.add(
                botones,
                BorderLayout.EAST);

        return panel;
    }

    // =========================================================
    // CONSOLA
    // =========================================================

    private JPanel crearPanelConsola() {

        JPanel panel = panelSeccion("Monitor / Consola");

        pantalla.setLineWrap(true);
        pantalla.setWrapStyleWord(true);

        pantalla.setBackground(
                new Color(24, 30, 36));

        pantalla.setForeground(
                new Color(208, 232, 214));

        pantalla.setCaretColor(Color.WHITE);

        pantalla.setBorder(
                new EmptyBorder(10, 10, 10, 10));

        pantalla.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        14));

        JScrollPane scroll = new JScrollPane(pantalla);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        new Color(45, 55, 65)));

        scroll.setMinimumSize(
                new Dimension(160, 120));

        scroll.setPreferredSize(
                new Dimension(400, 200));

        panel.add(
                scroll,
                BorderLayout.CENTER);

        /*
         * Entrada de teclado deshabilitada temporalmente para el Proyecto 2.
         * Se conserva este código comentado para restaurarlo si se necesita.
         *
        JPanel teclado = new JPanel(new BorderLayout(8, 6));

        teclado.setOpaque(false);

        teclado.setBorder(
                new EmptyBorder(8, 0, 0, 0));

        JLabel etiqueta = new JLabel(
                "Teclado · valor de 0 a 255");

        etiqueta.setFont(
                etiqueta.getFont().deriveFont(
                        Font.BOLD));

        entrada.setToolTipText(
                "Valor entero entre 0 y 255");

        aplicarBotonPrincipal(enviar);

        teclado.add(
                etiqueta,
                BorderLayout.NORTH);

        teclado.add(
                entrada,
                BorderLayout.CENTER);

        teclado.add(
                enviar,
                BorderLayout.EAST);
         */

        estadoMonitor.setForeground(
                TEXTO_SECUNDARIO);

        estadoMonitor.setBorder(
                new EmptyBorder(0, 0, 8, 0));

        panel.add(
                estadoMonitor,
                BorderLayout.NORTH);

        /*
        panel.add(
                teclado,
                BorderLayout.SOUTH);
        */

        return panel;
    }

    // =========================================================
    // RECURSOS
    // =========================================================

    private JTabbedPane crearPanelRecursos() {

        JTabbedPane pestanas = new JTabbedPane();

        pestanas.addTab(
                "RAM",
                crearPanelTabla(
                        tablaRam,
                        "Memoria principal"));

        pestanas.addTab(
                "Disco",
                crearPanelTabla(
                        tablaAlmacenamiento,
                        "Almacenamiento secundario"));

        pestanas.addTab(
                "Archivos",
                crearPanelTabla(
                        tablaArchivos,
                        "Archivos simulados"));

        JPanel panelEstadisticas = panelSeccion(
                "Estadísticas de procesos");

        estadisticas.setLineWrap(true);
        estadisticas.setWrapStyleWord(true);

        estadisticas.setBorder(
                new EmptyBorder(12, 12, 12, 12));

        panelEstadisticas.add(
                new JScrollPane(estadisticas),
                BorderLayout.CENTER);

        pestanas.addTab(
                "Estadísticas",
                panelEstadisticas);

        return pestanas;
    }

    private JPanel crearPanelTabla(
            JTable tabla,
            String titulo) {

        JPanel panel = new JPanel(new BorderLayout());

        panel.setBackground(PANEL);

        panel.setBorder(
                tituloInterno(titulo));

        panel.add(
                new JScrollPane(tabla),
                BorderLayout.CENTER);

        return panel;
    }

    // =========================================================
    // ESTILOS
    // =========================================================

    private JPanel panelSeccion(String titulo) {

        JPanel panel = new JPanel(new BorderLayout(6, 6));

        panel.setBackground(PANEL);

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createTitledBorder(
                                        BorderFactory.createEmptyBorder(),
                                        titulo,
                                        TitledBorder.LEFT,
                                        TitledBorder.TOP,
                                        fuenteInterfaz(Font.BOLD, 13f),
                                        TEXTO),
                                new EmptyBorder(8, 8, 8, 8))));

        return panel;
    }

    private TitledBorder tituloInterno(
            String titulo) {

        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDE),
                titulo,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                fuenteInterfaz(Font.BOLD, 12f),
                TEXTO_SECUNDARIO);
    }

    private Font fuenteInterfaz(
            int estilo,
            float tamaño) {

        Font fuente = UIManager.getFont("Label.font");

        if (fuente == null) {

            fuente = new Font(
                    Font.SANS_SERIF,
                    Font.PLAIN,
                    12);
        }

        return fuente.deriveFont(
                estilo,
                tamaño);
    }

    private void configurarSplit(
            JSplitPane split,
            double peso,
            int posicion) {

        split.setBorder(null);

        split.setContinuousLayout(true);

        split.setResizeWeight(peso);

        split.setDividerLocation(posicion);

        split.setDividerSize(7);
    }

    private void configurarBotones() {

        conservar.setFocusPainted(false);

        entrada.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        13));

        pantalla.setEditable(false);
    }

    private void aplicarBotonPrincipal(
            JButton boton) {

        aplicarEstiloBoton(
                boton,
                AZUL,
                Color.WHITE);
    }

    private void aplicarBotonExito(
            JButton boton) {

        aplicarEstiloBoton(
                boton,
                VERDE,
                Color.WHITE);
    }

    private void aplicarBotonAdvertencia(
            JButton boton) {

        aplicarEstiloBoton(
                boton,
                NARANJA,
                Color.WHITE);
    }

    private void aplicarBotonPeligro(
            JButton boton) {

        aplicarEstiloBoton(
                boton,
                ROJO,
                Color.WHITE);
    }

    private void aplicarBotonSecundario(
            JButton boton) {

        aplicarEstiloBoton(
                boton,
                new Color(89, 101, 114),
                Color.WHITE);
    }

    private void aplicarEstiloBoton(
            JButton boton,
            Color fondo,
            Color texto) {

        boton.setFocusPainted(false);
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setOpaque(true);

        boton.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                fondo.darker()),
                        new EmptyBorder(6, 10, 6, 10)));
    }

    private static JLabel valorRegistro(
            String texto) {

        JLabel label = new JLabel(texto);

        label.setHorizontalAlignment(
                SwingConstants.LEFT);

        return label;
    }

    private static JTextArea texto() {

        JTextArea area = new JTextArea();

        area.setEditable(false);

        area.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        13));

        return area;
    }

    private static DefaultTableModel modelo(
            String... columnas) {

        return new DefaultTableModel(
                columnas, 0) {

            @Override
            public boolean isCellEditable(
                    int fila,
                    int columna) {

                return false;
            }
        };
    }

    /**
     * Prepara las 80 primeras columnas de tiempo y permite extender
     * el cronograma si la simulación supera los 80 segundos.
     */
    private void configurarCronograma() {

        tablaCronograma.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tablaCronograma.setFillsViewportHeight(true);
        tablaCronograma.setRowHeight(27);
        tablaCronograma.setGridColor(BORDE);
        tablaCronograma.setIntercellSpacing(new Dimension(1, 1));
        tablaCronograma.setCellSelectionEnabled(true);
        tablaCronograma.getTableHeader().setReorderingAllowed(false);
        tablaCronograma.getTableHeader().setFont(fuenteInterfaz(Font.BOLD, 11f));
        tablaCronograma.getTableHeader().setBackground(new Color(226, 233, 243));
        tablaCronograma.setDefaultRenderer(Object.class, new RenderizadorCronograma());

        for (int segundo = 1; segundo <= SEGUNDOS_CRONOGRAMA_INICIALES; segundo++) {
            modeloCronograma.addColumn(Integer.toString(segundo));
        }

        tablaCronograma.getColumnModel().getColumn(0).setPreferredWidth(90);
        tablaCronograma.getColumnModel().getColumn(1).setPreferredWidth(190);

        for (int columna = 2; columna < modeloCronograma.getColumnCount(); columna++) {
            tablaCronograma.getColumnModel().getColumn(columna).setPreferredWidth(40);
        }
    }

    /** Obtiene el color estable de una CPU configurada. */
    private static Color colorCPU(int numero) {
        return numero >= 1 && numero <= COLORES_CPU.length
                ? COLORES_CPU[numero - 1]
                : TEXTO_SECUNDARIO;
    }

    /** Obtiene el fondo tenue de la misma CPU. */
    private static Color fondoCPU(int numero) {
        return numero >= 1 && numero <= FONDOS_CPU.length
                ? FONDOS_CPU[numero - 1]
                : GRIS_SUAVE;
    }

    private void configurarTablas() {

        JTable[] tablas = {
                tablaTrabajos,
                tablaRam,
                tablaAlmacenamiento,
                tablaArchivos
        };

        for (JTable tabla : tablas) {

            tabla.setFillsViewportHeight(true);
            tabla.setRowHeight(28);

            tabla.setGridColor(
                    new Color(225, 229, 234));

            tabla.setSelectionBackground(
                    AZUL_SUAVE);

            tabla.setSelectionForeground(TEXTO);

            tabla.getTableHeader()
                    .setFont(
                            tabla.getTableHeader()
                                    .getFont()
                                    .deriveFont(Font.BOLD));

            tabla.getTableHeader()
                    .setBackground(
                            new Color(228, 233, 239));

            tabla.getTableHeader()
                    .setForeground(TEXTO);

            tabla.getTableHeader()
                    .setReorderingAllowed(false);
        }

        tablaTrabajos.setDefaultRenderer(
                Object.class,
                new RenderizadorProcesos());

        tablaRam.setDefaultRenderer(
                Object.class,
                new RenderizadorMemoria());

        tablaAlmacenamiento.setDefaultRenderer(
                Object.class,
                new RenderizadorAlmacenamiento());

        tablaRam.getColumnModel()
                .getColumn(0).setPreferredWidth(70);

        tablaRam.getColumnModel()
                .getColumn(1).setPreferredWidth(90);

        tablaRam.getColumnModel()
                .getColumn(2).setPreferredWidth(280);

        tablaAlmacenamiento.getColumnModel()
                .getColumn(0).setPreferredWidth(70);

        tablaAlmacenamiento.getColumnModel()
                .getColumn(1).setPreferredWidth(90);

        tablaAlmacenamiento.getColumnModel()
                .getColumn(2).setPreferredWidth(280);
    }

    // =========================================================
    // EVENTOS
    // =========================================================

    private void registrarEventos() {

        // Cambiar el procesador visible mediante flechas, sin una tabla adicional.
        cpuAnterior.addActionListener(e -> cambiarCPUVisible(-1));
        cpuSiguiente.addActionListener(e -> cambiarCPUVisible(1));

        // Cada columna de la matriz transpuesta corresponde a un PID.
        tablaTrabajos.getColumnModel().getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!actualizando
                            && !e.getValueIsAdjusting()) {

                        int columna = tablaTrabajos.getSelectedColumn();

                        seleccionado = columna <= 0
                                ? null
                                : Integer.valueOf(
                                        trabajos.getValueAt(FILA_PID, columna)
                                                .toString());

                        actualizarDetalle();
                    }
                });

        // Al hacer clic en una fila del cronograma se muestra su BCP.
        tablaCronograma.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evento) {
                int fila = tablaCronograma.rowAtPoint(evento.getPoint());
                if (fila < 0 || fila >= pidsCronograma.size()) {
                    return;
                }
                seleccionarProcesoCronograma(pidsCronograma.get(fila));
            }
        });

        cargar.addActionListener(
                e -> elegirProgramas());

        disco.addActionListener(
                e -> elegirProgramaDisco());

        siguiente.addActionListener(
                e -> ejecutarTick());

        automatico.addActionListener(e -> {

            continuarAutomatico = true;

            reloj.start();

            actualizar();
        });

        pausa.addActionListener(e -> {

            detener();

            actualizar();
        });

        enviar.addActionListener(
                e -> entregarEntrada());

        entrada.addActionListener(
                e -> entregarEntrada());
    }

    // =========================================================
    // CONFIGURACION Y CARGA
    // =========================================================

    public void cargarConfiguracion(
            Path archivo) throws IOException {

        Configuracion nueva = new ConfigLoader().cargar(archivo);

        Controlador nuevo = new Controlador(nueva);

        detener();

        configuracion = nueva;
        controlador = nuevo;

        conservar.setSelected(
                nueva.isConservarArchivosAlReiniciar());

        nombres.clear();
        seleccionado = null;
        historialEjecucion.clear();
        cpuSeleccionada = 1;

        actualizar();
    }

    public void cargarPrograma(
            Path archivo) throws IOException {

        BCP proceso = controlador.cargarPrograma(archivo);

        nombres.put(
                proceso.getPid(),
                archivo.getFileName().toString());

        seleccionado = proceso.getPid();

        actualizar();
    }

    private JFileChooser selector(
            String extension,
            String descripcion,
            Path directorio) {

        JFileChooser chooser = new JFileChooser(directorio.toFile());

        chooser.setFileFilter(
                new FileNameExtensionFilter(
                        descripcion,
                        extension));

        return chooser;
    }

    private void elegirProgramas() {

        JFileChooser chooser = selector(
                "asm",
                "Programas ASM",
                raiz.resolve("codigo"));

        chooser.setMultiSelectionEnabled(true);

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        for (java.io.File archivo : chooser.getSelectedFiles()) {

            try {

                cargarPrograma(
                        archivo.toPath());

            } catch (IOException | RuntimeException ex) {

                mostrarError(ex);
            }
        }
    }

    private void elegirProgramaDisco() {

        java.util.List<String> programas = new java.util.ArrayList<>();
        for (ArchivoSimulado archivo : controlador.getAlmacenamiento().listarArchivos()) {
            if (archivo.esPrograma()) programas.add(archivo.getNombre());
        }
        if (programas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay programas ASM en el disco simulado.",
                    "Ejecutar desde disco", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String nombre = (String) JOptionPane.showInputDialog(this,
                "Seleccione un programa del disco simulado:", "Ejecutar desde disco",
                JOptionPane.QUESTION_MESSAGE, null, programas.toArray(new String[0]), programas.get(0));

        if (nombre == null) {
            return;
        }

        try {

            BCP proceso = controlador.cargarProgramaDesdeDisco(
                    nombre.trim());

            nombres.put(
                    proceso.getPid(),
                    nombre.trim());

            seleccionado = proceso.getPid();

            actualizar();

        } catch (RuntimeException ex) {

            mostrarError(ex);
        }
    }

    // =========================================================
    // EJECUCION
    // =========================================================

    private boolean hayEjecutable() {

        return controlador != null
                && controlador.puedeAvanzar();
    }

    private void ejecutarTick() {

        try {

            /*
             * Guardar los tiempos anteriores. Comparar antes y después
             * permite identificar qué PID utilizó cada CPU incluso si
             * una INT 20H finaliza el proceso dentro del mismo tick.
             */
            Map<Integer, Integer> tiemposAntes = new HashMap<>();

            for (BCP proceso : controlador.getGestorProcesos().getListaTrabajos()) {
                tiemposAntes.put(proceso.getPid(), proceso.getTiempoCPU());
            }

            int instanteAnterior = controlador.getTiempoSimulado();

            controlador.ejecutarSiguiente();

            if (controlador.getTiempoSimulado() > instanteAnterior) {
                registrarSegundoEjecutado(
                        controlador.getTiempoSimulado(), tiemposAntes);
            }

            if (controlador.simulacionFinalizada()) {

                detener();

            } else if (!hayEjecutable()) {

                reloj.stop();
            }

            actualizar();

        } catch (RuntimeException ex) {

            detener();
            actualizar();
            mostrarError(ex);
        }
    }

    private void entregarEntrada() {

        try {

            controlador.recibirEntradaTeclado(
                    entrada.getText());

            entrada.setText("");

            if (continuarAutomatico) {
                reloj.start();
            }

            actualizar();

        } catch (RuntimeException ex) {

            mostrarError(ex);
        }
    }

    private void detener() {

        reloj.stop();

        continuarAutomatico = false;
    }

    private void mostrarError(Exception ex) {

        JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Mini PC",
                JOptionPane.ERROR_MESSAGE);
    }

    private String hora(LocalDateTime valor) {

        return valor == null
                ? "-"
                : valor.format(
                        DateTimeFormatter.ofPattern("HH:mm"));
    }

    // =========================================================
    // ACTUALIZACION GENERAL
    // =========================================================

    private void actualizar() {

        boolean listo = controlador != null;

        // Sincronizar controles.
        ajustandoControles = true;

        try {

            if (listo) {

                cantidadCPU.setValue(
                        controlador.getCantidadCPU());

                TipoAlgoritmo tipo = controlador.getAlgoritmoSeleccionado();

                algoritmos.setSelectedItem(tipo);
            }

        } finally {

            ajustandoControles = false;
        }

        boolean configurables = listo
                && controlador.getTiempoSimulado() == 0
                && !continuarAutomatico;

        cantidadCPU.setEnabled(configurables);
        algoritmos.setEnabled(configurables);
        quantumRR.setEnabled(configurables);

        valorReloj.setText(
                listo
                        ? "RELOJ  " + controlador.getTiempoSimulado() + " s"
                        : "RELOJ  0 s");

        algoritmoSuperior.setText(
                "Algoritmo: " + (!listo || controlador.getAlgoritmoSeleccionado() == null
                        ? "sin seleccionar"
                        : controlador.getAlgoritmoSeleccionado().name()
                                + (controlador.getAlgoritmoSeleccionado() == TipoAlgoritmo.RR
                                        || controlador.getAlgoritmoSeleccionado() == TipoAlgoritmo.SELFISH_ROUND_ROBIN
                                        ? " · quantum " + controlador.getQuantumRR() + " s"
                                        : "")
                                + (controlador.getAlgoritmoSeleccionado() == TipoAlgoritmo.SELFISH_ROUND_ROBIN
                                        ? " · a=" + controlador.getTasaEsperaSRR() + " b=" + controlador.getTasaServicioSRR()
                                        : "")));

        valorNucleos.setText(
                listo
                        ? controlador.getCantidadCPU() + " / 5"
                        : "1 / 5");

        if (!listo) {

            estadoSimulacion.setText(
                    "Carga una configuración");

        } else if (continuarAutomatico) {

            estadoSimulacion.setText(
                    "En ejecución");

        } else if (controlador.getTiempoSimulado() == 0) {

            estadoSimulacion.setText(
                    controlador.getAlgoritmoSeleccionado() == null
                            ? "Selecciona un algoritmo"
                            : "Listo para iniciar");

        } else if (controlador.simulacionFinalizada()) {

            estadoSimulacion.setText(
                    "Simulación finalizada");

        } else {

            estadoSimulacion.setText(
                    "Pausado en t="
                            + controlador.getTiempoSimulado()
                            + " s");
        }

        // Botones.
        cargar.setEnabled(listo);
        disco.setEnabled(listo);
        pausa.setEnabled(continuarAutomatico);

        boolean ejecutable = listo && hayEjecutable();

        siguiente.setEnabled(
                ejecutable && !continuarAutomatico);

        automatico.setEnabled(
                ejecutable && !continuarAutomatico);

        // Detectar espera de teclado en cualquier CPU.
        boolean espera = false;

        if (listo) {

            for (ContextoNucleo nucleo : controlador.getNucleos()) {

                if (nucleo.getInterrupciones()
                        .hayProcesoEsperandoTeclado()) {

                    espera = true;
                    break;
                }
            }
        }

        enviar.setEnabled(espera);
        entrada.setEnabled(espera);

        if (!listo) {

            valorRAM.setText("-");
            valorKernel.setText("-");
            valorProgramasEnRAM.setText("-");
            valorDisco.setText("-");

            procesoCPU.setText("Sin CPU configurada");

            actualizarCronograma();
            actualizarTarjetasProcesos();
            actualizarDetalle();

            return;
        }

        // =====================================================
        // RESUMEN DE MEMORIA
        // =====================================================

        int procesosEnRAM = controlador.getGestorProcesos()
                .getProcesosEnRAM().size();

        int memoriaTotal = configuracion.getTamañoMemoria();

        int kernel = configuracion.getInicioUsuario();

        int usuarioTotal = memoriaTotal - kernel;

        int usuarioOcupado = 0;

        for (int i = controlador.getInicioUsuario(); i < controlador.getSizeMemoria(); i++) {

            if (controlador.getMemoria().leer(i) != null) {

                usuarioOcupado++;
            }
        }

        int discoOcupado = controlador.getAlmacenamiento()
                .getEspacioArchivosOcupado();

        int discoTotal = controlador.getAlmacenamiento()
                .getTamañoArchivos();

        valorRAM.setText(
                usuarioOcupado + " / " + usuarioTotal);

        valorKernel.setText(
                kernel + " posiciones");

        valorProgramasEnRAM.setText(
                procesosEnRAM
                        + " / "
                        + configuracion.getMaxProcesosEnRam());

        valorDisco.setText(
                discoOcupado + " / " + discoTotal);

        barraRAM.setMaximum(
                Math.max(usuarioTotal, 1));

        barraRAM.setValue(usuarioOcupado);

        barraRAM.setString(
                usuarioTotal == 0
                        ? "0%"
                        : (usuarioOcupado * 100 / usuarioTotal)
                                + "%");

        barraDisco.setMaximum(
                Math.max(discoTotal, 1));

        barraDisco.setValue(
                Math.min(discoOcupado, discoTotal));

        barraDisco.setString(
                discoTotal == 0
                        ? "0%"
                        : (discoOcupado * 100 / discoTotal)
                                + "%");

        // =====================================================
        // ACTUALIZAR CPU SELECCIONADA
        // =====================================================

        actualizarCPUSeleccionada();

        // =====================================================
        // TABLA DE PROCESOS
        // =====================================================

        actualizando = true;

        trabajos.setRowCount(0);
        trabajos.setColumnCount(1);

        // Filas = atributos, columnas = procesos (matriz transpuesta).
        for (String campo : CAMPOS_PROCESO) {
            trabajos.addRow(new Object[] { campo });
        }

        StringBuilder resumenEstadisticas = new StringBuilder();

        for (BCP p : controlador.getGestorProcesos()
                .getListaTrabajos()) {

            String ubicacion = p.getEstado() == EstadoProceso.FINALIZADO
                    ? "Liberada"
                    : controlador.getGestorMemoriaVirtual()
                            .estaEnMemoriaVirtual(p)
                                    ? "Virtual @"
                                            + controlador
                                                    .getGestorMemoriaVirtual()
                                                    .getDireccionVirtual(p)
                                    : "RAM";

            trabajos.addColumn("PID " + p.getPid(), new String[] {
                    Integer.toString(p.getPid()),
                    p.getCpuAsignada() < 1
                            ? "Sin CPU"
                            : "CPU " + p.getCpuAsignada(),
                    nombres.getOrDefault(p.getPid(), ""),
                    p.getEstado().toString(),
                    ubicacion,
                    Integer.toString(p.getInicioPrograma()),
                    Integer.toString(p.getTamañoPrograma()),
                    Integer.toString(p.getTiempoCPU()),
                    Integer.toString(p.getRafaga()),
                    Integer.toString(p.getTiempoLlegada()),
                    Integer.toString(p.getPrioridad()),
                    Integer.toString(p.getTiempoRestante())
            });

            resumenEstadisticas
                    .append("PROCESO PID ")
                    .append(p.getPid())
                    .append(" · ")
                    .append(nombres.getOrDefault(
                            p.getPid(), ""))
                    .append(" · CPU ")
                    .append(p.getCpuAsignada() < 1
                            ? "sin asignar"
                            : Integer.toString(p.getCpuAsignada()))
                    .append("\n────────────────────────────\n")
                    .append("Estado: ")
                    .append(p.getEstado())
                    .append("\nResultado: ")
                    .append(
                            !p.getMotivoError().isEmpty()
                                    ? "Error: "
                                            + p.getMotivoError()
                                    : p.getHoraFinal() == null
                                            ? "Pendiente"
                                            : "Finalización normal")
                    .append("\nInicio: ")
                    .append(hora(p.getHoraInicio()))
                    .append("\nFin: ")
                    .append(hora(p.getHoraFinal()))
                    .append("\nDuración real: ")
                    .append(
                            p.getHoraFinal() == null
                                    ? "Pendiente"
                                    : String.format(
                                            java.util.Locale.ROOT,
                                            "%.3f s",
                                            p.getTiempoTotalSegundos()))
                    .append("\nTiempo CPU: ")
                    .append(p.getTiempoCPU())
                    .append(" s")
                    .append("\n\nContexto ")
                    .append(
                            p.getHoraFinal() == null
                                    ? "actual"
                                    : "final")
                    .append("\nPC: ")
                    .append(p.getPC())
                    .append("\nIR: ")
                    .append(p.getIR())
                    .append("\nAC: ")
                    .append(p.getAC())
                    .append("\nAX: ")
                    .append(p.getAX())
                    .append("\nBX: ")
                    .append(p.getBX())
                    .append("\nCX: ")
                    .append(p.getCX())
                    .append("\nDX: ")
                    .append(p.getDX())
                    .append("\nAH: ")
                    .append(p.getAH())
                    .append("\nAL: ")
                    .append(p.getAL())
                    .append("\nZF: ")
                    .append(p.isZeroFlag())
                    .append("\nPila: ")
                    .append(p.getPila())
                    .append("\n\n\n");

        }

        javax.swing.table.TableRowSorter<DefaultTableModel> filtroFilas =
                new javax.swing.table.TableRowSorter<>(trabajos);
        for (int columna = 0; columna < trabajos.getColumnCount(); columna++) {
            filtroFilas.setSortable(columna, false);
        }
        filtroFilas.setRowFilter(new javax.swing.RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entrada) {
                return entrada.getIdentifier() != FILA_PRIORIDAD || usaPrioridad();
            }
        });
        tablaTrabajos.setRowSorter(filtroFilas);

        // Restaurar tamaños y selección después de recrear las columnas.
        tablaTrabajos.getColumnModel().getColumn(0).setPreferredWidth(150);

        for (int columna = 1; columna < trabajos.getColumnCount(); columna++) {
            tablaTrabajos.getColumnModel().getColumn(columna).setPreferredWidth(150);

            if (seleccionado != null
                    && Integer.toString(seleccionado)
                            .equals(trabajos.getValueAt(FILA_PID, columna).toString())) {

                tablaTrabajos.setColumnSelectionInterval(columna, columna);
            }
        }

        actualizando = false;

        actualizarCronograma();
        actualizarTarjetasProcesos();

        String contenidoEstadisticas = resumenEstadisticas.length() == 0
                ? "Todavía no hay procesos cargados."
                : resumenEstadisticas.toString();

        if (!estadisticas.getText()
                .equals(contenidoEstadisticas)) {

            int posicion = estadisticas.getCaretPosition();

            estadisticas.setText(
                    contenidoEstadisticas);

            estadisticas.setCaretPosition(
                    Math.min(
                            posicion,
                            estadisticas.getDocument()
                                    .getLength()));
        }

        // =====================================================
        // RAM
        // =====================================================

        ram.setRowCount(0);

        for (int i = 0; i < controlador.getSizeMemoria(); i++) {

            String contenido = i < controlador.getInicioUsuario()
                    ? controlador.getMemoria()
                            .obtenerContenido(i)
                    : controlador.getMemoria()
                            .leer(i) == null
                                    ? ""
                                    : controlador.getMemoria()
                                            .leer(i)
                                            .getTextoOriginal();

            ram.addRow(new String[] {
                    Integer.toString(i),
                    i < controlador.getInicioUsuario()
                            ? "Kernel"
                            : "Usuario",
                    contenido
            });
        }

        // =====================================================
        // DISCO
        // =====================================================

        Almacenamiento a = controlador.getAlmacenamiento();

        almacenamiento.setRowCount(0);

        for (int i = 0; i < a.getSize(); i++) {

            almacenamiento.addRow(new String[] {
                    Integer.toString(i),
                    i < a.getTamañoIndice()
                            ? "Índice"
                            : i < a.getInicioMemoriaVirtual()
                                    ? "Archivos"
                                    : "Virtual",
                    a.obtenerContenido(i)
            });
        }

        // =====================================================
        // ARCHIVOS
        // =====================================================

        archivos.setRowCount(0);

        for (ArchivoSimulado f : a.listarArchivos()) {

            archivos.addRow(new String[] {
                    f.getNombre(),
                    Integer.toString(
                            f.getDireccionInicio()),
                    Integer.toString(
                            f.getTamaño()),
                    Integer.toString(
                            f.getEspacioOcupado()),
                    f.esPrograma()
                            ? "Programa ASM"
                            : "Texto"
            });
        }

        // =====================================================
        // CONSOLA DE TODAS LAS CPU
        // =====================================================

        java.util.List<String> salidas = new java.util.ArrayList<>();

        for (ContextoNucleo nucleo : controlador.getNucleos()) {

            for (String mensaje : nucleo.getInterrupciones()
                    .getSalidas()) {

                salidas.add(
                        "[CPU "
                                + nucleo.getCpu().getId()
                                + "] "
                                + mensaje);
            }
        }

        java.util.List<String> errores = controlador.getErroresEjecucion();

        String contenidoMonitor = String.join("\n", salidas);

        if (!errores.isEmpty()) {

            contenidoMonitor += (contenidoMonitor.isEmpty()
                    ? ""
                    : "\n\n")
                    + "Errores de ejecución:\n"
                    + String.join("\n", errores);
        }

        if (!pantalla.getText()
                .equals(contenidoMonitor)) {

            pantalla.setText(
                    contenidoMonitor);

            pantalla.setCaretPosition(
                    pantalla.getDocument()
                            .getLength());
        }

        estadoMonitor.setText(
                espera
                        ? "Hay una solicitud de entrada pendiente."
                        : "Salida de programas · "
                                + salidas.size()
                                + " mensajes");

        actualizarDetalle();
    }

    // =========================================================
    // HISTORIAL DE EJECUCIÓN Y CRONOGRAMA
    // =========================================================

    /**
     * Registra qué proceso consumió cada segundo de cada CPU.
     *
     * Se compara el tiempo CPU de los BCP para incluir las instrucciones
     * de finalización, aunque el proceso ya no esté en EJECUCION.
     *
     * @param segundo segundo que acaba de ejecutarse (comienza en 1)
     * @param tiemposAntes tiempo CPU por PID antes del tick
     */
    private void registrarSegundoEjecutado(
            int segundo,
            Map<Integer, Integer> tiemposAntes) {

        Map<Integer, Integer> ejecutados = new HashMap<>();

        for (BCP proceso : controlador.getGestorProcesos().getListaTrabajos()) {

            int tiempoAnterior = tiemposAntes.getOrDefault(proceso.getPid(), 0);

            if (proceso.getTiempoCPU() > tiempoAnterior
                    && proceso.getCpuAsignada() >= 1) {

                ejecutados.put(proceso.getCpuAsignada(), proceso.getPid());
            }
        }

        // Puede quedar vacío si todas las CPU estuvieron ociosas.
        historialEjecucion.put(segundo, ejecutados);
    }

    /**
     * Actualiza la tabla temporal sin modificar el estado del simulador.
     * Presenta una fila por proceso agrupada según su CPU asignada.
     */
    private void actualizarCronograma() {

        int tiempo = controlador == null ? 0 : controlador.getTiempoSimulado();

        // Extender las columnas al superar los primeros 80 segundos.
        if (tiempo + 2 > modeloCronograma.getColumnCount()) {

            while (modeloCronograma.getColumnCount() < tiempo + 2) {
                int segundoNuevo = modeloCronograma.getColumnCount() - 1;
                modeloCronograma.addColumn(Integer.toString(segundoNuevo));
            }

            tablaCronograma.getColumnModel().getColumn(0).setPreferredWidth(90);
            tablaCronograma.getColumnModel().getColumn(1).setPreferredWidth(190);

            for (int columna = 2; columna < modeloCronograma.getColumnCount(); columna++) {
                tablaCronograma.getColumnModel().getColumn(columna).setPreferredWidth(40);
            }
        }

        modeloCronograma.setRowCount(0);
        pidsCronograma.clear();

        if (controlador == null) {
            resumenCronograma.setText("0 s registrados");
            return;
        }

        List<BCP> procesos = new ArrayList<>(
                controlador.getGestorProcesos().getListaTrabajos());

        // Mantener juntas las filas que pertenecen a una misma CPU.
        procesos.sort((primero, segundo) -> {
            int comparacion = Integer.compare(
                    primero.getCpuAsignada(), segundo.getCpuAsignada());
            return comparacion != 0
                    ? comparacion
                    : Integer.compare(primero.getPid(), segundo.getPid());
        });

        for (BCP proceso : procesos) {

            int cpuId = proceso.getCpuAsignada();
            if (cpuId < 1) {
                continue;
            }

            Object[] fila = new Object[modeloCronograma.getColumnCount()];
            fila[0] = "CPU " + cpuId;
            fila[1] = "PID " + proceso.getPid() + " · "
                    + nombres.getOrDefault(proceso.getPid(), "Proceso");

            for (int segundo = 1; segundo <= tiempo; segundo++) {

                Map<Integer, Integer> porCPU = historialEjecucion.get(segundo);

                if (porCPU != null
                        && Integer.valueOf(proceso.getPid()).equals(porCPU.get(cpuId))) {
                    fila[segundo + 1] = "■";
                }
            }

            modeloCronograma.addRow(fila);
            pidsCronograma.add(proceso.getPid());
        }

        int segundosOcupados = 0;
        for (Map<Integer, Integer> segundo : historialEjecucion.values()) {
            segundosOcupados += segundo.size();
        }

        resumenCronograma.setText(
                tiempo + " s · " + segundosOcupados + " s de CPU acumulados");

        // Mantener visible el segundo reciente durante la ejecución.
        if (tiempo > 0 && seguirCronograma.isSelected()) {
            desplazarCronograma(tiempo);
        }
    }

    /** Desplaza horizontalmente la línea de tiempo a un segundo. */
    private void desplazarCronograma(int segundo) {

        if (tablaCronograma.getRowCount() == 0) {
            return;
        }

        int columna = Math.min(
                Math.max(2, segundo + 1),
                tablaCronograma.getColumnCount() - 1);

        tablaCronograma.scrollRectToVisible(
                tablaCronograma.getCellRect(0, columna, true));
    }

    /** Abre el BCP correspondiente al proceso elegido en el cronograma. */
    private void seleccionarProcesoCronograma(int pid) {

        seleccionado = pid;

        for (int columna = 1; columna < trabajos.getColumnCount(); columna++) {
            if (Integer.toString(pid).equals(
                    trabajos.getValueAt(FILA_PID, columna).toString())) {
                tablaTrabajos.setColumnSelectionInterval(columna, columna);
                tablaTrabajos.scrollRectToVisible(
                        tablaTrabajos.getCellRect(FILA_PID, columna, true));
                break;
            }
        }

        BCP proceso = controlador.getGestorProcesos().buscarProceso(pid);
        if (proceso != null && proceso.getCpuAsignada() >= 1
                && proceso.getCpuAsignada() <= controlador.getCantidadCPU()) {
            cpuSeleccionada = proceso.getCpuAsignada();
            actualizarCPUSeleccionada();
        }

        actualizarDetalle();
        actualizarTarjetasProcesos();
    }

    // =========================================================
    // NAVEGACIÓN ENTRE LAS CPU
    // =========================================================

    /**
     * Avanza o retrocede de forma circular entre las CPU configuradas.
     * Cada flecha cambia únicamente la CPU mostrada en la interfaz.
     */
    private void cambiarCPUVisible(int desplazamiento) {

        if (controlador == null || controlador.getCantidadCPU() == 0) {
            return;
        }

        int cantidad = controlador.getCantidadCPU();
        cpuSeleccionada = Math.floorMod(
                cpuSeleccionada - 1 + desplazamiento, cantidad) + 1;

        actualizarCPUSeleccionada();
        tablaRam.repaint();
    }

    // =========================================================
    // REGISTROS DE CPU SELECCIONADA
    // =========================================================

    private void actualizarCPUSeleccionada() {

        if (controlador == null
                || controlador.getCantidadCPU() < 1) {
            return;
        }

        cpuSeleccionada = Math.max(1,
                Math.min(cpuSeleccionada, controlador.getCantidadCPU()));

        int numero = cpuSeleccionada;

        cpuAnterior.setEnabled(controlador.getCantidadCPU() > 1);
        cpuSiguiente.setEnabled(controlador.getCantidadCPU() > 1);

        CPU cpuSeleccionada = controlador.getCPU(numero);

        BCP proceso = controlador.getProcesoActual(numero);

        procesoCPU.setText(
                proceso == null
                        ? "CPU " + numero + " de " + controlador.getCantidadCPU() + " · Libre"
                        : "CPU " + numero + " de " + controlador.getCantidadCPU()
                                + " · Ejecutando PID "
                                + proceso.getPid());

        procesoCPU.setForeground(
                proceso == null
                        ? TEXTO_SECUNDARIO
                        : colorCPU(numero));

        valorPC.setText(
                String.valueOf(
                        cpuSeleccionada.getPC()));

        String ir = proceso == null || cpuSeleccionada.getIR() == null
                ? "—"
                : cpuSeleccionada.getIR();

        valorIR.setText(
                ir.length() > 18
                        ? ir.substring(0, 18) + "..."
                        : ir);

        valorIR.setToolTipText(ir);

        valorAC.setText(
                String.valueOf(
                        cpuSeleccionada.getAC()));

        valorAX.setText(
                String.valueOf(
                        cpuSeleccionada.getRegistros().getAX()));

        valorBX.setText(
                String.valueOf(
                        cpuSeleccionada.getRegistros().getBX()));

        valorCX.setText(
                String.valueOf(
                        cpuSeleccionada.getRegistros().getCX()));

        valorDX.setText(
                String.valueOf(
                        cpuSeleccionada.getRegistros().getDX()));

        valorAH.setText(
                String.valueOf(
                        cpuSeleccionada.getRegistros().getAH()));

        valorAL.setText(
                String.valueOf(
                        cpuSeleccionada.getRegistros().getAL()));

        valorZF.setText(
                String.valueOf(
                        cpuSeleccionada.isZeroFlag()));

        tiempoOcupadoCPU.setText(cpuSeleccionada.getTiempoOcupado() + " s");
        tiempoOciosoCPU.setText(cpuSeleccionada.getTiempoOcioso() + " s");
        tablaRam.repaint();
    }

    // =========================================================
    // DETALLE DEL BCP
    // =========================================================

    private void actualizarDetalle() {

        BCP p = seleccionado == null
                || controlador == null
                        ? null
                        : controlador.getGestorProcesos()
                                .buscarProceso(seleccionado);

        // Mantener la pestaña elegida mientras el reloj actualiza los datos.
        int pestañaActiva = pestañasBCP.getSelectedIndex();
        pestañasBCP.removeAll();

        if (p == null) {
            JLabel ayuda = new JLabel(
                    "<html>Selecciona <b>Ver BCP</b> en una tarjeta "
                            + "para consultar el proceso.</html>");
            ayuda.setForeground(TEXTO_SECUNDARIO);
            ayuda.setBorder(new EmptyBorder(10, 8, 10, 8));
            pestañasBCP.addTab("General", ayuda);

        } else {

            JPanel informacion = new JPanel();
            informacion.setLayout(new BoxLayout(informacion, BoxLayout.Y_AXIS));
            informacion.setOpaque(false);

            // Encabezado con PID, nombre y color de CPU.
            JPanel cabecera = new JPanel(new BorderLayout(7, 4));
            cabecera.setOpaque(false);
            cabecera.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel titulo = new JLabel("PID " + p.getPid() + " · "
                    + nombres.getOrDefault(p.getPid(), "Proceso"));
            titulo.setFont(fuenteInterfaz(Font.BOLD, 14f));
            titulo.setForeground(TEXTO);
            titulo.setToolTipText(titulo.getText());

            JLabel insignia = new JLabel(p.getCpuAsignada() < 1
                    ? "SIN CPU" : "CPU " + p.getCpuAsignada());
            insignia.setOpaque(true);
            insignia.setBackground(colorCPU(p.getCpuAsignada()));
            insignia.setForeground(Color.WHITE);
            insignia.setFont(fuenteInterfaz(Font.BOLD, 11f));
            insignia.setBorder(new EmptyBorder(5, 8, 5, 8));

            cabecera.add(titulo, BorderLayout.CENTER);
            cabecera.add(insignia, BorderLayout.EAST);
            informacion.add(cabecera);
            informacion.add(Box.createVerticalStrut(7));

            informacion.add(crearGrupoDetalleBCP("Identificación y estado",
                    new String[][] {
                            { "Estado", p.getEstado().toString() },
                            { "Llegada", p.getTiempoLlegada() + " s" },
                            { "Prioridad", String.valueOf(p.getPrioridad()) },
                            { "Tiempo CPU", p.getTiempoCPU() + " s" },
                            { "Ráfaga", p.getRafaga() + " s" },
                            { "Restante", p.getTiempoRestante() + " s" }
                    }));
            informacion.add(Box.createVerticalStrut(8));

            JPanel memoriaYTiempo = crearGrupoDetalleBCP("Memoria y tiempo",
                    new String[][] {
                            { "Inicio del programa", String.valueOf(p.getInicioPrograma()) },
                            { "Fin del programa", String.valueOf(p.getFinPrograma()) },
                            { "Tamaño", p.getTamañoPrograma() + " instrucciones" },
                            { "Dirección BCP", String.valueOf(p.getDireccionBCP()) },
                            { "Inicio real", hora(p.getHoraInicio()) },
                            { "Fin real", hora(p.getHoraFinal()) }
                    });

            JPanel registrosYContexto = crearGrupoDetalleBCP("Registros y contexto",
                    new String[][] {
                            { "PC", String.valueOf(p.getPC()) },
                            { "IR · instrucción", String.valueOf(p.getIR()) },
                            { "AC", String.valueOf(p.getAC()) },
                            { "Zero Flag", String.valueOf(p.isZeroFlag()) },
                            { "AX", String.valueOf(p.getAX()) },
                            { "BX", String.valueOf(p.getBX()) },
                            { "CX", String.valueOf(p.getCX()) },
                            { "DX", String.valueOf(p.getDX()) },
                            { "AH", String.valueOf(p.getAH()) },
                            { "AL", String.valueOf(p.getAL()) },
                            { "Pila", String.valueOf(p.getPila()) },
                            { "CPU", String.valueOf(p.getCpuAsignada()) }
                    });

            if (p.getMotivoError() != null && !p.getMotivoError().isBlank()) {
                informacion.add(Box.createVerticalStrut(8));
                JLabel error = new JLabel("<html><b>Error:</b> "
                        + escaparHTML(p.getMotivoError()) + "</html>");
                error.setOpaque(true);
                error.setBackground(ROJO_SUAVE);
                error.setForeground(ROJO);
                error.setBorder(new EmptyBorder(8, 8, 8, 8));
                error.setAlignmentX(Component.LEFT_ALIGNMENT);
                informacion.add(error);
            }

            // Tres vistas cortas evitan un BCP demasiado alto o difícil de leer.
            pestañasBCP.addTab("General", crearVistaDetalle(informacion));
            pestañasBCP.addTab("Memoria y tiempo", crearVistaDetalle(memoriaYTiempo));
            pestañasBCP.addTab("Registros", crearVistaDetalle(registrosYContexto));
        }

        pestañasBCP.setSelectedIndex(Math.max(0,
                Math.min(pestañaActiva, pestañasBCP.getTabCount() - 1)));
        pestañasBCP.revalidate();
        pestañasBCP.repaint();
    }

    /** Añade desplazamiento vertical individual a cada vista del BCP. */
    private JScrollPane crearVistaDetalle(JPanel contenido) {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(PANEL);
        contenedor.setBorder(new EmptyBorder(5, 5, 5, 5));
        contenedor.add(contenido, BorderLayout.NORTH);
        JScrollPane desplazamiento = new JScrollPane(contenedor);
        desplazamiento.setBorder(null);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(18);
        return desplazamiento;
    }

    /** Crea un grupo de datos del BCP con dos columnas de tarjetas. */
    private JPanel crearGrupoDetalleBCP(String titulo, String[][] datos) {

        JPanel grupo = new JPanel(new GridLayout(0, 2, 7, 6));
        grupo.setBackground(PANEL);
        grupo.setBorder(tituloInterno(titulo));
        grupo.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (String[] dato : datos) {
            grupo.add(crearDatoDetalleBCP(dato[0], dato[1]));
        }

        return grupo;
    }

    /** Muestra un atributo individual del BCP con etiqueta y valor. */
    private JPanel crearDatoDetalleBCP(String nombre, String valor) {

        JPanel celda = new JPanel(new BorderLayout(2, 3));
        celda.setBackground(new Color(245, 248, 252));
        celda.setBorder(new EmptyBorder(6, 8, 6, 8));

        JLabel etiqueta = new JLabel(nombre);
        etiqueta.setForeground(TEXTO_SECUNDARIO);
        etiqueta.setFont(fuenteInterfaz(Font.BOLD, 10f));

        JLabel contenido = new JLabel(valor == null ? "—" : valor);
        contenido.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        contenido.setForeground(TEXTO);
        contenido.setToolTipText(valor);

        celda.add(etiqueta, BorderLayout.NORTH);
        celda.add(contenido, BorderLayout.CENTER);

        return celda;
    }

    /** Evita que textos de error sean interpretados como HTML. */
    private static String escaparHTML(String texto) {
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // =========================================================
    // RENDERIZADOR DE PROCESOS
    // =========================================================

    private class RenderizadorProcesos
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Component componente = super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            row = table.convertRowIndexToModel(row);

            if (column == 0) {
                componente.setBackground(new Color(230, 236, 245));
                componente.setForeground(TEXTO);
                setFont(table.getFont().deriveFont(Font.BOLD));
                setToolTipText("Atributo del bloque de control de proceso");
                return componente;
            }

            String estadoProceso = table.getValueAt(FILA_ESTADO, column).toString();

            String cpuTexto = table.getValueAt(FILA_CPU, column).toString();
            int numero = -1;

            if (cpuTexto.startsWith("CPU ")) {
                try {
                    numero = Integer.parseInt(cpuTexto.substring(4));
                } catch (NumberFormatException ignorado) {
                    // El proceso todavía puede estar sin CPU asignada.
                }
            }

            if (row == FILA_CPU && numero > 0) {
                // La fila de CPU identifica por color cada columna-proceso.
                componente.setBackground(colorCPU(numero));
                componente.setForeground(Color.WHITE);

            } else if (isSelected) {
                componente.setBackground(numero > 0
                        ? fondoCPU(numero).darker()
                        : AZUL_SUAVE);
                componente.setForeground(TEXTO);

            } else if ((row == FILA_LLEGADA || row == FILA_PRIORIDAD)
                    && trabajos.isCellEditable(row, column)) {
                // El amarillo identifica las celdas editables por doble clic.
                componente.setBackground(new Color(255, 248, 212));
                componente.setForeground(new Color(114, 77, 14));

            } else {
                componente.setBackground(numero > 0
                        ? fondoCPU(numero)
                        : GRIS_SUAVE);
                componente.setForeground(TEXTO);
            }

            setFont(table.getFont().deriveFont(
                    row == FILA_PID || row == FILA_CPU
                            || row == FILA_LLEGADA || row == FILA_PRIORIDAD
                            || estadoProceso.equals("EJECUCION")
                            ? Font.BOLD : Font.PLAIN));

            setToolTipText(row == FILA_LLEGADA
                    ? "Doble clic para editar la llegada antes de iniciar"
                    : row == FILA_PRIORIDAD
                            ? "Doble clic para editar la prioridad antes de iniciar"
                            : "Proceso " + table.getValueAt(FILA_PID, column)
                                    + " asignado a " + cpuTexto);

            return componente;
        }
    }

    /**
     * Representa cada segundo ejecutado mediante una celda coloreada.
     * Los colores se mantienen iguales a los de la lista de trabajos.
     */
    private class RenderizadorCronograma extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            Component componente = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            setToolTipText(null);

            String etiqueta = String.valueOf(table.getValueAt(row, 0));
            int cpuId = 1;

            try {
                cpuId = Integer.parseInt(etiqueta.substring(4));
            } catch (RuntimeException ignorado) {
                // En ausencia de una CPU válida, usar el color inicial.
            }

            if (column == 0) {
                componente.setBackground(colorCPU(cpuId));
                componente.setForeground(Color.WHITE);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(table.getFont().deriveFont(Font.BOLD));

            } else if (column == 1) {
                componente.setBackground(fondoCPU(cpuId));
                componente.setForeground(TEXTO);
                setHorizontalAlignment(SwingConstants.LEFT);
                setFont(table.getFont().deriveFont(Font.BOLD));

            } else {
                boolean ejecutado = "■".equals(value);

                componente.setBackground(ejecutado
                        ? colorCPU(cpuId)
                        : (row % 2 == 0 ? Color.WHITE : new Color(247, 249, 252)));
                componente.setForeground(ejecutado ? Color.WHITE : TEXTO);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(table.getFont().deriveFont(Font.BOLD, 15f));

                setToolTipText("CPU " + cpuId + " · "
                        + table.getValueAt(row, 1) + " · segundo " + (column - 1)
                        + (ejecutado ? " · ejecutó 1 s" : " · no ejecutó"));
            }

            return componente;
        }
    }

    // =========================================================
    // RENDERIZADOR DE MEMORIA
    // =========================================================

    private class RenderizadorMemoria
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Component componente = super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            int direccion = Integer.parseInt(table.getValueAt(row, 0).toString());
            if (controlador != null && cpuSeleccionada >= 1
                    && cpuSeleccionada <= controlador.getCantidadCPU()) {
                ContextoNucleo nucleo = controlador.getNucleos().get(cpuSeleccionada - 1);
                BCP actual = controlador.getProcesoActual(cpuSeleccionada);
                if (actual != null && actual.getPid() == nucleo.getPidUltimoTick()
                        && nucleo.getDireccionUltimoTick() == direccion) {
                    componente.setBackground(colorCPU(cpuSeleccionada));
                    componente.setForeground(Color.WHITE);
                    setToolTipText("Instrucción del último tick en CPU " + cpuSeleccionada);
                    return componente;
                }
            }
            setToolTipText(null);
            if (isSelected) {
                return componente;
            }

            String area = table.getValueAt(row, 1).toString();

            componente.setBackground(
                    area.equals("Kernel")
                            ? MORADO_SUAVE
                            : Color.WHITE);

            componente.setForeground(TEXTO);

            return componente;
        }
    }

    // =========================================================
    // RENDERIZADOR DE ALMACENAMIENTO
    // =========================================================

    private class RenderizadorAlmacenamiento
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Component componente = super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            if (isSelected) {
                return componente;
            }

            String area = table.getValueAt(row, 1).toString();

            if (area.equals("Índice")) {

                componente.setBackground(AZUL_SUAVE);

            } else if (area.equals("Virtual")) {

                componente.setBackground(MORADO_SUAVE);

            } else {

                componente.setBackground(Color.WHITE);
            }

            componente.setForeground(TEXTO);

            return componente;
        }
    }
}
