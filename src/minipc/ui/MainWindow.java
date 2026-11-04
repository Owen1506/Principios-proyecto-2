package minipc.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import minipc.config.ConfigLoader;
import minipc.config.Configuracion;
import minipc.controller.Controlador;
import minipc.model.*;

/**
 * Ventana principal de la Mini PC.
 *
 * La interfaz únicamente muestra información y delega toda la lógica
 * de ejecución al Controlador.
 */
public class MainWindow extends JFrame {

    private final Path raiz;

    private Controlador controlador;
    private Configuracion configuracion;

    private final Map<Integer, String> nombres = new HashMap<>();

    /*
     * Colores de la interfaz.
     *
     * Se utilizan únicamente para diferenciar visualmente los componentes
     * del simulador. No intervienen en la lógica de la Mini PC.
     */
    private static final Color FONDO = new Color(243, 246, 249);
    private static final Color PANEL = Color.WHITE;
    private static final Color BORDE = new Color(205, 213, 221);
    private static final Color TEXTO = new Color(35, 43, 52);
    private static final Color TEXTO_SECUNDARIO = new Color(92, 104, 116);

    private static final Color AZUL = new Color(48, 105, 180);
    private static final Color AZUL_SUAVE = new Color(230, 240, 252);

    private static final Color VERDE = new Color(53, 145, 91);
    private static final Color VERDE_SUAVE = new Color(231, 246, 236);

    private static final Color NARANJA = new Color(205, 127, 42);
    private static final Color NARANJA_SUAVE = new Color(252, 239, 220);

    private static final Color ROJO = new Color(184, 69, 69);
    private static final Color ROJO_SUAVE = new Color(249, 229, 229);

    private static final Color MORADO = new Color(118, 83, 169);
    private static final Color MORADO_SUAVE = new Color(239, 233, 248);

    private static final Color GRIS_SUAVE = new Color(236, 239, 242);

    private static final int MAX_CPUS = 5;
    private final JLabel[] procesoCPU = crearValoresCPU("");

    private final JLabel[] valorPC = crearValoresCPU("0");
    private final JLabel[] valorIR = crearValoresCPU("");
    private final JLabel[] valorAC = crearValoresCPU("0");
    private final JLabel[] valorAX = crearValoresCPU("0");
    private final JLabel[] valorBX = crearValoresCPU("0");
    private final JLabel[] valorCX = crearValoresCPU("0");
    private final JLabel[] valorDX = crearValoresCPU("0");
    private final JLabel[] valorAH = crearValoresCPU("");
    private final JLabel[] valorAL = crearValoresCPU("");
    private final JLabel[] valorZF = crearValoresCPU("false");

    private final JLabel[] valorAlgoritmo = crearValoresCPU("—");

    private final JLabel estadoMonitor = new JLabel("Salida de programas");

    private final JLabel valorRAM = new JLabel("-");
    private final JLabel valorKernel = new JLabel("-");
    private final JLabel valorProgramasEnRAM = new JLabel("-");
    private final JLabel valorDisco = new JLabel("-");

    private final JProgressBar barraRAM = new JProgressBar();
    private final JProgressBar barraDisco = new JProgressBar();

    private final JTextArea detalle = texto();
    private final JTextArea pantalla = texto();

    private final JCheckBox conservar = new JCheckBox("Conservar archivos al reiniciar", true);

    private final JButton cargar = new JButton("Cargar ASM");
    private final JButton disco = new JButton("Ejecutar desde disco");
    private final JButton siguiente = new JButton("Siguiente");
    private final JButton automatico = new JButton("Ejecutar");
    private final JButton pausa = new JButton("Pausar");

    private final DefaultTableModel trabajos = modelo(
            "PID", "Programa", "Estado", "Ubicación", "Base", "Instrucciones", "Tiempo CPU (s)");

    private final JTextArea estadisticas = texto();

    private final DefaultTableModel ram = modelo("Dirección", "Área", "Contenido");
    private final DefaultTableModel almacenamiento = modelo("Dirección", "Área", "Contenido");
    private final DefaultTableModel archivos = modelo("Nombre", "Dirección", "Peso", "Espacio", "Tipo");

    private final JTable tablaTrabajos = new JTable(trabajos);
    private final JTable tablaRam = new JTable(ram);
    private final JTable tablaAlmacenamiento = new JTable(almacenamiento);
    private final JTable tablaArchivos = new JTable(archivos);

    private final Timer reloj;

    private boolean continuarAutomatico;
    private boolean actualizando;
    private Integer seleccionado;

    public MainWindow() {
        this(Paths.get("."));
    }

    public MainWindow(Path raiz) {

        super("Mini PC - Gestor de Procesos");

        this.raiz = raiz.toAbsolutePath().normalize();
        // Cada tick automático consume un segundo real y un segundo simulado de CPU.
        this.reloj = new Timer(1000, e -> ejecutarTick());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1250, 720));
        setSize(1500, 900);
        setLocationRelativeTo(null);

        getContentPane().setBackground(FONDO);

        configurarBotones();
        configurarTablas();
        construirInterfaz();
        registrarEventos();

        actualizar();

        try {
            cargarConfiguracion(this.raiz.resolve("config/minipc.txt"));
        } catch (IOException | RuntimeException ex) {
            // Permite seleccionar la configuración desde el botón.
        }
    }

    /**
     * Construye una interfaz tipo panel de control:
     *
     * izquierda -> procesos y BCP
     * centro    -> CPU, ejecución y consola
     * derecha   -> memoria, disco, archivos y estadísticas
     */
    private void construirInterfaz() {

        setLayout(new BorderLayout(10, 10));

        add(crearEncabezado(), BorderLayout.NORTH);

        JPanel principal = new JPanel(new BorderLayout(10, 10));
        principal.setOpaque(false);
        principal.setBorder(new EmptyBorder(0, 10, 10, 10));

        JSplitPane izquierdaCentro = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                crearPanelProcesos(),
                crearPanelCPUConsola());

        configurarSplit(izquierdaCentro, 0.40, 500);

        JSplitPane contenido = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                izquierdaCentro,
                crearPanelRecursos());

        configurarSplit(contenido, 0.73, 1050);

        principal.add(contenido, BorderLayout.CENTER);
        add(principal, BorderLayout.CENTER);
    }

    private JPanel crearEncabezado() {

        JPanel exterior = new JPanel(new BorderLayout(0, 8));
        exterior.setOpaque(false);
        exterior.setBorder(new EmptyBorder(10, 10, 0, 10));

        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setBackground(new Color(36, 48, 61));
        barra.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel titulo = new JLabel("MINI PC");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));

        JLabel subtitulo = new JLabel("Simulador de procesos y sistema operativo");
        subtitulo.setForeground(new Color(205, 215, 225));
        subtitulo.setFont(subtitulo.getFont().deriveFont(Font.PLAIN, 12f));

        JPanel identidad = new JPanel(new GridLayout(2, 1, 0, 0));
        identidad.setOpaque(false);
        identidad.add(titulo);
        identidad.add(subtitulo);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        acciones.setOpaque(false);

        JButton configurar = new JButton("Configuración");
        JButton reiniciar = new JButton("Reiniciar");

        aplicarBotonSecundario(configurar);
        aplicarBotonPrincipal(cargar);
        aplicarBotonSecundario(disco);
        aplicarBotonPrincipal(siguiente);
        aplicarBotonExito(automatico);
        aplicarBotonSecundario(pausa);
        aplicarBotonPeligro(reiniciar);

        conservar.setOpaque(false);
        conservar.setForeground(Color.WHITE);

        acciones.add(configurar);
        acciones.add(cargar);
        acciones.add(disco);
        acciones.add(reiniciar);
        acciones.add(conservar);

        barra.add(identidad, BorderLayout.WEST);
        barra.add(acciones, BorderLayout.EAST);

        JPanel resumen = new JPanel(new GridLayout(1, 4, 8, 0));
        resumen.setOpaque(false);

        resumen.add(crearTarjetaResumen("RAM Usuario", valorRAM, AZUL, barraRAM));
        resumen.add(crearTarjetaResumen("Kernel / SO", valorKernel, MORADO, null));
        resumen.add(crearTarjetaResumen("Programas en RAM", valorProgramasEnRAM, VERDE, null));
        resumen.add(crearTarjetaResumen("Disco", valorDisco, NARANJA, barraDisco));

        exterior.add(barra, BorderLayout.NORTH);
        exterior.add(resumen, BorderLayout.CENTER);
        exterior.add(crearPanelEjecucion(), BorderLayout.SOUTH);

        configurar.addActionListener(e -> elegirConfiguracion());

        reiniciar.addActionListener(e -> {
            detener();
            if (controlador != null) controlador.reiniciar(conservar.isSelected());
            nombres.clear();
            seleccionado = null;
            actualizar();
        });

        return exterior;
    }

    private JPanel crearTarjetaResumen(String titulo, JLabel valor, Color acento, JProgressBar barra) {

        JPanel panel = new JPanel(new BorderLayout(4, 6));
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, acento),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        new EmptyBorder(9, 12, 9, 12))));

        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setForeground(TEXTO_SECUNDARIO);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 11f));

        valor.setForeground(TEXTO);
        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 17f));

        panel.add(etiqueta, BorderLayout.NORTH);
        panel.add(valor, BorderLayout.CENTER);

        if (barra != null) {
            barra.setStringPainted(true);
            barra.setPreferredSize(new Dimension(100, 15));
            panel.add(barra, BorderLayout.SOUTH);
        }

        return panel;
    }

    private JPanel crearPanelProcesos() {

        JPanel panel = panelSeccion("Procesos / BCP");

        tablaTrabajos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel detalleBCP = new JPanel(new BorderLayout(4, 4));
        detalleBCP.setBackground(PANEL);
        detalleBCP.setBorder(tituloInterno("BCP seleccionado"));

        detalle.setBackground(new Color(249, 250, 252));
        detalle.setBorder(new EmptyBorder(8, 8, 8, 8));

        detalleBCP.add(new JScrollPane(detalle), BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                crearPanelTabla(tablaTrabajos, "Lista de trabajos"),
                detalleBCP);

        configurarSplit(division, 0.30, 150);

        panel.add(division, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelCPUConsola() {

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);

        JPanel superior = new JPanel(new BorderLayout(8, 8));
        superior.setOpaque(false);
        JPanel panelCPU = crearPanelCPU();
        superior.add(panelCPU, BorderLayout.CENTER);
        // Reserva la altura natural de pestañas, estado y registros según la fuente.
        int alturaCPU = panelCPU.getPreferredSize().height;
        superior.setMinimumSize(new Dimension(160, alturaCPU));
        superior.setPreferredSize(new Dimension(400, alturaCPU));

        JPanel inferior = new JPanel(new BorderLayout(8, 8));
        inferior.setOpaque(false);
        inferior.setMinimumSize(new Dimension(160, 160));
        inferior.add(crearPanelConsola(), BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                superior,
                inferior);

        configurarSplit(division, 0.40, alturaCPU);

        panel.add(division, BorderLayout.CENTER);

        return panel;
    }

    private JLabel[] crearValoresCPU(String inicial) {
        JLabel[] valores = new JLabel[MAX_CPUS];
        for (int i = 0; i < MAX_CPUS; i++) {
            valores[i] = valorRegistro(i == 0 ? inicial : "—");
        }
        return valores;
    }

    private JPanel crearPanelCPU() {
        JPanel panel = panelSeccion("Procesador (CPU)");
        JTabbedPane cpus = new JTabbedPane();
        cpus.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        for (int i = 0; i < MAX_CPUS; i++) {
            cpus.addTab("CPU " + (i + 1), crearEstadoCPU(i));
            if (i > 0) {
                cpus.setToolTipTextAt(i, "Esta CPU todavía no está configurada.");
            }
        }
        panel.add(cpus, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearEstadoCPU(int indice) {

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBackground(PANEL);
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));

        procesoCPU[indice].setText("CPU " + (indice + 1)
                + (indice == 0 ? " libre" : " no configurada"));
        procesoCPU[indice].setFont(procesoCPU[indice].getFont().deriveFont(Font.BOLD, 16f));
        procesoCPU[indice].setForeground(AZUL);
        procesoCPU[indice].setHorizontalAlignment(SwingConstants.CENTER);
        procesoCPU[indice].setBorder(new EmptyBorder(4, 4, 10, 4));

        panel.add(procesoCPU[indice], BorderLayout.NORTH);

        JPanel registros = new JPanel(new GridLayout(3, 4, 8, 8));
        registros.setOpaque(false);
        registros.setBorder(new EmptyBorder(2, 4, 2, 4));

        registros.add(crearCeldaRegistro("PC", valorPC[indice], AZUL_SUAVE, AZUL));
        registros.add(crearCeldaRegistro("IR", valorIR[indice], MORADO_SUAVE, MORADO));
        registros.add(crearCeldaRegistro("AC", valorAC[indice], VERDE_SUAVE, VERDE));
        registros.add(crearCeldaRegistro("ZF", valorZF[indice], GRIS_SUAVE, TEXTO_SECUNDARIO));

        registros.add(crearCeldaRegistro("AX", valorAX[indice], PANEL, BORDE));
        registros.add(crearCeldaRegistro("BX", valorBX[indice], PANEL, BORDE));
        registros.add(crearCeldaRegistro("CX", valorCX[indice], PANEL, BORDE));
        registros.add(crearCeldaRegistro("DX", valorDX[indice], PANEL, BORDE));

        registros.add(crearCeldaRegistro("AH", valorAH[indice], PANEL, BORDE));
        registros.add(crearCeldaRegistro("AL", valorAL[indice], PANEL, BORDE));
        registros.add(crearCeldaRegistro("ALGORITMO", valorAlgoritmo[indice], GRIS_SUAVE, TEXTO_SECUNDARIO));

        panel.add(registros, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearCeldaRegistro(String nombre, JLabel valor, Color fondo, Color borde) {

        JPanel celda = new JPanel(new BorderLayout(2, 2));
        celda.setBackground(fondo);
        celda.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borde),
                new EmptyBorder(4, 10, 4, 10)));

        JLabel etiqueta = new JLabel(nombre);
        etiqueta.setForeground(TEXTO_SECUNDARIO);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 10f));

        valor.setForeground(TEXTO);
        valor.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));

        celda.add(etiqueta, BorderLayout.NORTH);
        celda.add(valor, BorderLayout.CENTER);

        return celda;
    }

    private JPanel crearPanelEjecucion() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE), new EmptyBorder(4, 10, 4, 10)));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        botones.setOpaque(false);

        aplicarBotonPrincipal(siguiente);
        aplicarBotonExito(automatico);
        aplicarBotonSecundario(pausa);

        botones.add(siguiente);
        botones.add(automatico);
        botones.add(pausa);

        panel.add(botones, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelConsola() {

        JPanel panel = panelSeccion("Monitor / Consola");

        pantalla.setLineWrap(true);
        pantalla.setWrapStyleWord(true);
        pantalla.setBackground(new Color(24, 30, 36));
        pantalla.setForeground(new Color(208, 232, 214));
        pantalla.setCaretColor(Color.WHITE);
        pantalla.setBorder(new EmptyBorder(10, 10, 10, 10));
        pantalla.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        JScrollPane scroll = new JScrollPane(pantalla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(45, 55, 65)));
        scroll.setMinimumSize(new Dimension(160, 120));
        scroll.setPreferredSize(new Dimension(400, 200));

        panel.add(scroll, BorderLayout.CENTER);

        estadoMonitor.setForeground(TEXTO_SECUNDARIO);
        estadoMonitor.setBorder(new EmptyBorder(0, 0, 8, 0));
        panel.add(estadoMonitor, BorderLayout.NORTH);

        return panel;
    }

    private JTabbedPane crearPanelRecursos() {

        JTabbedPane pestanas = new JTabbedPane();

        pestanas.addTab("RAM", crearPanelTabla(tablaRam, "Memoria principal"));
        pestanas.addTab("Disco", crearPanelTabla(tablaAlmacenamiento, "Almacenamiento secundario"));
        pestanas.addTab("Archivos", crearPanelTabla(tablaArchivos, "Archivos simulados"));
        JPanel panelEstadisticas = panelSeccion("Estadísticas de procesos");
        estadisticas.setLineWrap(true);
        estadisticas.setWrapStyleWord(true);
        estadisticas.setBorder(new EmptyBorder(12, 12, 12, 12));
        panelEstadisticas.add(new JScrollPane(estadisticas), BorderLayout.CENTER);
        pestanas.addTab("Estadísticas", panelEstadisticas);

        return pestanas;
    }

    private JPanel crearPanelTabla(JTable tabla, String titulo) {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL);
        panel.setBorder(tituloInterno(titulo));
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        return panel;
    }

    private JPanel panelSeccion(String titulo) {

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
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

    private TitledBorder tituloInterno(String titulo) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDE),
                titulo,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                fuenteInterfaz(Font.BOLD, 12f),
                TEXTO_SECUNDARIO);
    }

    /**
     * Obtiene una fuente segura para componentes creados antes de que
     * el JFrame haya recibido una fuente propia del Look & Feel.
     */
    private Font fuenteInterfaz(int estilo, float tamaño) {

        Font fuente = UIManager.getFont("Label.font");

        if (fuente == null) {
            fuente = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }

        return fuente.deriveFont(estilo, tamaño);
    }

    private void configurarSplit(JSplitPane split, double peso, int posicion) {
        split.setBorder(null);
        split.setContinuousLayout(true);
        split.setResizeWeight(peso);
        split.setDividerLocation(posicion);
        split.setDividerSize(7);
    }

    private void configurarBotones() {
        conservar.setFocusPainted(false);
    }

    private void aplicarBotonPrincipal(JButton boton) {
        aplicarEstiloBoton(boton, AZUL, Color.WHITE);
    }

    private void aplicarBotonExito(JButton boton) {
        aplicarEstiloBoton(boton, VERDE, Color.WHITE);
    }

    private void aplicarBotonAdvertencia(JButton boton) {
        aplicarEstiloBoton(boton, NARANJA, Color.WHITE);
    }

    private void aplicarBotonPeligro(JButton boton) {
        aplicarEstiloBoton(boton, ROJO, Color.WHITE);
    }

    private void aplicarBotonSecundario(JButton boton) {
        aplicarEstiloBoton(boton, new Color(89, 101, 114), Color.WHITE);
    }

    private void aplicarEstiloBoton(JButton boton, Color fondo, Color texto) {
        boton.setFocusPainted(false);
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setOpaque(true);
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fondo.darker()),
                new EmptyBorder(6, 10, 6, 10)));
    }

    private static JLabel valorRegistro(String texto) {
        JLabel label = new JLabel(texto);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
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
            tabla.setRowHeight(24);
            tabla.setGridColor(new Color(225, 229, 234));
            tabla.setSelectionBackground(AZUL_SUAVE);
            tabla.setSelectionForeground(TEXTO);
            tabla.getTableHeader().setFont(tabla.getTableHeader().getFont().deriveFont(Font.BOLD));
            tabla.getTableHeader().setBackground(new Color(228, 233, 239));
            tabla.getTableHeader().setForeground(TEXTO);
        }

        tablaTrabajos.setDefaultRenderer(Object.class, new RenderizadorProcesos());
        tablaRam.setDefaultRenderer(Object.class, new RenderizadorMemoria());
        tablaAlmacenamiento.setDefaultRenderer(Object.class, new RenderizadorAlmacenamiento());


        tablaRam.getColumnModel().getColumn(0).setPreferredWidth(70);
        tablaRam.getColumnModel().getColumn(1).setPreferredWidth(90);
        tablaRam.getColumnModel().getColumn(2).setPreferredWidth(280);

        tablaAlmacenamiento.getColumnModel().getColumn(0).setPreferredWidth(70);
        tablaAlmacenamiento.getColumnModel().getColumn(1).setPreferredWidth(90);
        tablaAlmacenamiento.getColumnModel().getColumn(2).setPreferredWidth(280);
    }

    private void registrarEventos() {

        tablaTrabajos.getSelectionModel().addListSelectionListener(e -> {

            if (!actualizando && !e.getValueIsAdjusting()) {
                int fila = tablaTrabajos.getSelectedRow();
                seleccionado = fila < 0 ? null : Integer.valueOf(trabajos.getValueAt(fila, 0).toString());
                actualizarDetalle();
            }
        });

        cargar.addActionListener(e -> elegirProgramas());
        disco.addActionListener(e -> elegirProgramaDisco());
        siguiente.addActionListener(e -> ejecutarTick());

        automatico.addActionListener(e -> {
            continuarAutomatico = true;
            reloj.start();
            actualizar();
        });

        pausa.addActionListener(e -> {
            detener();
            actualizar();
        });


    }

    private static JTextArea texto() {

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        return area;
    }

    private static DefaultTableModel modelo(String... columnas) {

        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    /**
     * Cargar otra configuración crea una simulación nueva.
     */
    public void cargarConfiguracion(Path archivo) throws IOException {

        Configuracion nueva = new ConfigLoader().cargar(archivo);
        Controlador nuevo = new Controlador(nueva);

        detener();

        configuracion = nueva;
        controlador = nuevo;

        conservar.setSelected(nueva.isConservarArchivosAlReiniciar());

        nombres.clear();
        seleccionado = null;

        actualizar();
    }

    public void cargarPrograma(Path archivo) throws IOException {

        BCP proceso = controlador.cargarPrograma(archivo);

        nombres.put(proceso.getPid(), archivo.getFileName().toString());
        seleccionado = proceso.getPid();

        actualizar();
    }

    private JFileChooser selector(String extension, String descripcion, Path directorio) {

        JFileChooser chooser = new JFileChooser(directorio.toFile());
        chooser.setFileFilter(new FileNameExtensionFilter(descripcion, extension));

        return chooser;
    }

    private void elegirConfiguracion() {

        JFileChooser chooser = new JFileChooser(raiz.resolve("config").toFile());
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Configuración Mini PC (*.properties, *.txt)", "properties", "txt"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        if (controlador != null
                && (!controlador.getGestorProcesos().getListaTrabajos().isEmpty()
                || controlador.getAlmacenamiento().getEspacioArchivosOcupado() > 0)
                && JOptionPane.showConfirmDialog(
                        this,
                        "La nueva configuración inicia otra simulación y borra los archivos simulados. ¿Continuar?",
                        "Cambiar configuración",
                        JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            cargarConfiguracion(chooser.getSelectedFile().toPath());
        } catch (IOException | RuntimeException ex) {
            mostrarError(ex);
        }
    }

    private void elegirProgramas() {

        JFileChooser chooser = selector("asm", "Programas ASM", raiz.resolve("codigo"));
        chooser.setMultiSelectionEnabled(true);

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        for (java.io.File archivo : chooser.getSelectedFiles()) {
            try {
                cargarPrograma(archivo.toPath());
            } catch (IOException | RuntimeException ex) {
                mostrarError(ex);
            }
        }
    }

    private void elegirProgramaDisco() {

        String nombre = JOptionPane.showInputDialog(
                this,
                "Nombre del programa guardado en el disco simulado:");

        if (nombre == null) return;

        try {
            BCP proceso = controlador.cargarProgramaDesdeDisco(nombre.trim());

            nombres.put(proceso.getPid(), nombre.trim());
            seleccionado = proceso.getPid();

            actualizar();

        } catch (RuntimeException ex) {
            mostrarError(ex);
        }
    }

    private boolean hayEjecutable() {

        if (controlador == null) return false;

        if (controlador.getProcesoActual() != null
                || !controlador.getGestorProcesos().getColaPreparados().isEmpty()) {
            return true;
        }

        for (BCP bcp : controlador.getGestorProcesos().getListaTrabajos()) {

            if (bcp.getEstado() == EstadoProceso.PREPARADO_SUSPENDIDO
                    && controlador.getGestorProcesos().hayEspacioParaPrograma(bcp.getTamañoPrograma())) {
                return true;
            }
        }

        return false;
    }

    private void ejecutarTick() {

        try {
            controlador.ejecutarSiguiente();

            if (controlador.simulacionFinalizada()) detener();
            else if (!hayEjecutable()) reloj.stop();

            actualizar();

        } catch (RuntimeException ex) {
            detener();
            actualizar();
            mostrarError(ex);
        }
    }

    private void detener() {
        reloj.stop();
        continuarAutomatico = false;
    }

    private void mostrarError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Mini PC", JOptionPane.ERROR_MESSAGE);
    }

    private String hora(LocalDateTime valor) {
        return valor == null ? "-" : valor.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String registros(BCP p) {

        return "PC=" + p.getPC()
                + " IR=" + p.getIR()
                + " AC=" + p.getAC()
                + " AX=" + p.getAX()
                + " BX=" + p.getBX()
                + " CX=" + p.getCX()
                + " DX=" + p.getDX()
                + " AH=" + p.getAH()
                + " AL=" + p.getAL()
                + " ZF=" + p.isZeroFlag();
    }

    private void actualizar() {

        boolean listo = controlador != null;

        cargar.setEnabled(listo);
        disco.setEnabled(listo);
        pausa.setEnabled(continuarAutomatico);

        boolean ejecutable = listo && hayEjecutable();

        siguiente.setEnabled(ejecutable && !continuarAutomatico);
        automatico.setEnabled(ejecutable && !continuarAutomatico);


        if (!listo) {
            valorRAM.setText("-");
            valorKernel.setText("-");
            valorProgramasEnRAM.setText("-");
            valorDisco.setText("-");
            return;
        }

        int procesosEnRAM = controlador.getGestorProcesos().getProcesosEnRAM().size();

        int memoriaTotal = configuracion.getTamañoMemoria();
        int kernel = configuracion.getInicioUsuario();
        int usuarioTotal = memoriaTotal - kernel;

        int usuarioOcupado = 0;

        for (int i = controlador.getInicioUsuario(); i < controlador.getSizeMemoria(); i++) {
            if (controlador.getMemoria().leer(i) != null) usuarioOcupado++;
        }

        int discoOcupado = controlador.getAlmacenamiento().getEspacioArchivosOcupado();
        int discoTotal = controlador.getAlmacenamiento().getTamañoArchivos();

        valorRAM.setText(usuarioOcupado + " / " + usuarioTotal);
        valorKernel.setText(kernel + " posiciones");
        valorProgramasEnRAM.setText(procesosEnRAM + " / " + configuracion.getMaxProcesosEnRam());
        valorDisco.setText(discoOcupado + " / " + discoTotal);

        barraRAM.setMaximum(Math.max(usuarioTotal, 1));
        barraRAM.setValue(usuarioOcupado);
        barraRAM.setString(usuarioTotal == 0 ? "0%" : (usuarioOcupado * 100 / usuarioTotal) + "%");

        barraDisco.setMaximum(Math.max(discoTotal, 1));
        barraDisco.setValue(Math.min(discoOcupado, discoTotal));
        barraDisco.setString(discoTotal == 0 ? "0%" : (discoOcupado * 100 / discoTotal) + "%");


        CPU c = controlador.getCPU();
        BCP actual = controlador.getProcesoActual();

        procesoCPU[0].setText(actual == null ? "CPU 1 libre" : "CPU 1 ejecutando PID " + actual.getPid());
        procesoCPU[0].setForeground(actual == null ? TEXTO_SECUNDARIO : VERDE);

        valorPC[0].setText(Integer.toString(c.getPC()));
        valorIR[0].setText(c.getIR());
        valorAC[0].setText(Integer.toString(c.getAC()));
        valorAX[0].setText(Integer.toString(c.getRegistros().getAX()));
        valorBX[0].setText(Integer.toString(c.getRegistros().getBX()));
        valorCX[0].setText(Integer.toString(c.getRegistros().getCX()));
        valorDX[0].setText(c.getRegistros().getDX());
        valorAH[0].setText(c.getRegistros().getAH());
        valorAL[0].setText(c.getRegistros().getAL());
        valorZF[0].setText(Boolean.toString(c.isZeroFlag()));

        valorAlgoritmo[0].setText(configuracion.getAlgoritmo());

        actualizando = true;

        trabajos.setRowCount(0);
        StringBuilder resumenEstadisticas = new StringBuilder();

        for (BCP p : controlador.getGestorProcesos().getListaTrabajos()) {

            String ubicacion = p.getEstado() == EstadoProceso.FINALIZADO
                    ? "Liberada"
                    : controlador.getGestorMemoriaVirtual().estaEnMemoriaVirtual(p)
                    ? "Virtual @" + controlador.getGestorMemoriaVirtual().getDireccionVirtual(p)
                    : "RAM";

            trabajos.addRow(new String[]{
                Integer.toString(p.getPid()),
                nombres.getOrDefault(p.getPid(), ""),
                p.getEstado().toString(),
                ubicacion,
                Integer.toString(p.getInicioPrograma()),
                Integer.toString(p.getTamañoPrograma()),
                Integer.toString(p.getTiempoCPU())
            });

            resumenEstadisticas.append("PROCESO PID ").append(p.getPid())
                    .append(" · ").append(nombres.getOrDefault(p.getPid(), ""))
                    .append("\n────────────────────────────\n")
                    .append("Estado: ").append(p.getEstado())
                    .append("\nResultado: ").append(!p.getMotivoError().isEmpty()
                            ? "Error: " + p.getMotivoError()
                            : p.getHoraFinal() == null ? "Pendiente" : "Finalización normal")
                    .append("\nInicio: ").append(hora(p.getHoraInicio()))
                    .append("\nFin: ").append(hora(p.getHoraFinal()))
                    .append("\nDuración real: ").append(p.getHoraFinal() == null ? "Pendiente"
                            : String.format(java.util.Locale.ROOT, "%.3f s", p.getTiempoTotalSegundos()))
                    .append("\nTiempo CPU: ").append(p.getTiempoCPU()).append(" s")
                    .append("\n\nContexto ").append(p.getHoraFinal() == null ? "actual" : "final")
                    .append("\nPC: ").append(p.getPC()).append("\nIR: ").append(p.getIR())
                    .append("\nAC: ").append(p.getAC()).append("\nAX: ").append(p.getAX())
                    .append("\nBX: ").append(p.getBX()).append("\nCX: ").append(p.getCX())
                    .append("\nDX: ").append(p.getDX()).append("\nAH: ").append(p.getAH())
                    .append("\nAL: ").append(p.getAL()).append("\nZF: ").append(p.isZeroFlag())
                    .append("\nPila: ").append(p.getPila()).append("\n\n\n");

            if (seleccionado != null && seleccionado == p.getPid()) {
                tablaTrabajos.setRowSelectionInterval(
                        trabajos.getRowCount() - 1,
                        trabajos.getRowCount() - 1);
            }
        }

        actualizando = false;
        String contenidoEstadisticas = resumenEstadisticas.length() == 0
                ? "Todavía no hay procesos cargados." : resumenEstadisticas.toString();
        if (!estadisticas.getText().equals(contenidoEstadisticas)) {
            int posicion = estadisticas.getCaretPosition();
            estadisticas.setText(contenidoEstadisticas);
            estadisticas.setCaretPosition(Math.min(posicion, estadisticas.getDocument().getLength()));
        }

        ram.setRowCount(0);

        for (int i = 0; i < controlador.getSizeMemoria(); i++) {

            String contenido = i < controlador.getInicioUsuario()
                    ? controlador.getMemoria().obtenerContenido(i)
                    : controlador.getMemoria().leer(i) == null
                    ? ""
                    : controlador.getMemoria().leer(i).getTextoOriginal();

            ram.addRow(new String[]{
                Integer.toString(i),
                i < controlador.getInicioUsuario() ? "Kernel" : "Usuario",
                contenido
            });
        }

        Almacenamiento a = controlador.getAlmacenamiento();

        almacenamiento.setRowCount(0);

        for (int i = 0; i < a.getSize(); i++) {

            almacenamiento.addRow(new String[]{
                Integer.toString(i),
                i < a.getTamañoIndice()
                        ? "Índice"
                        : i < a.getInicioMemoriaVirtual()
                        ? "Archivos"
                        : "Virtual",
                a.obtenerContenido(i)
            });
        }

        archivos.setRowCount(0);

        for (ArchivoSimulado f : a.listarArchivos()) {

            archivos.addRow(new String[]{
                f.getNombre(),
                Integer.toString(f.getDireccionInicio()),
                Integer.toString(f.getTamaño()),
                Integer.toString(f.getEspacioOcupado()),
                f.esPrograma() ? "Programa ASM" : "Texto"
            });
        }

        java.util.List<String> salidas = controlador.getGestorInterrupciones().getSalidas();
        java.util.List<String> errores = controlador.getErroresEjecucion();
        String contenidoMonitor = String.join("\n", salidas);
        if (!errores.isEmpty()) contenidoMonitor += (contenidoMonitor.isEmpty() ? "" : "\n\n")
                + "Errores de ejecución:\n" + String.join("\n", errores);
        if (!pantalla.getText().equals(contenidoMonitor)) {
            pantalla.setText(contenidoMonitor);
            pantalla.setCaretPosition(pantalla.getDocument().getLength());
        }
        estadoMonitor.setText("Salida de programas · " + salidas.size() + " mensajes");

        actualizarDetalle();
    }

    private void actualizarDetalle() {

        BCP p = seleccionado == null || controlador == null
                ? null
                : controlador.getGestorProcesos().buscarProceso(seleccionado);

        detalle.setText(
                p == null
                        ? "Selecciona un proceso para consultar su BCP, pila y contexto."
                        : p.toString());

        if (p != null) {

            ArchivoSimulado programa = controlador.getAlmacenamiento().buscarArchivo(nombres.get(p.getPid()));

            if (programa != null && programa.esPrograma()) {

                detalle.append("\n\nPrograma guardado en disco:\n");

                int posicion = 0;

                for (Instruccion instruccion : programa.getInstrucciones()) {
                    detalle.append(posicion++ + ": " + instruccion.getTextoOriginal() + "\n");
                }
            }
        }

        detalle.setCaretPosition(0);

    }

    /**
     * Renderiza los estados de proceso con una representación visual clara.
     */
    private class RenderizadorProcesos extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Component componente = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (isSelected) return componente;

            String estadoProceso = table.getValueAt(row, 2).toString();

            if (estadoProceso.equals("EJECUCION")) componente.setBackground(VERDE_SUAVE);
            else if (estadoProceso.equals("PREPARADO")) componente.setBackground(AZUL_SUAVE);
            else if (estadoProceso.equals("BLOQUEADO")) componente.setBackground(ROJO_SUAVE);
            else if (estadoProceso.contains("SUSPENDIDO")) componente.setBackground(MORADO_SUAVE);
            else if (estadoProceso.equals("FINALIZADO")) componente.setBackground(GRIS_SUAVE);
            else componente.setBackground(Color.WHITE);

            componente.setForeground(TEXTO);

            return componente;
        }
    }

    /**
     * Diferencia visualmente el área Kernel y el área de Usuario.
     */
    private class RenderizadorMemoria extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Component componente = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (isSelected) return componente;

            String area = table.getValueAt(row, 1).toString();

            componente.setBackground(area.equals("Kernel") ? MORADO_SUAVE : Color.WHITE);
            componente.setForeground(TEXTO);

            return componente;
        }
    }

    /**
     * Diferencia visualmente índice, archivos y memoria virtual.
     */
    private class RenderizadorAlmacenamiento extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Component componente = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (isSelected) return componente;

            String area = table.getValueAt(row, 1).toString();

            if (area.equals("Índice")) componente.setBackground(AZUL_SUAVE);
            else if (area.equals("Virtual")) componente.setBackground(MORADO_SUAVE);
            else componente.setBackground(Color.WHITE);

            componente.setForeground(TEXTO);

            return componente;
        }
    }
}
