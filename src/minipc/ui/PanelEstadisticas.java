package minipc.ui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import minipc.services.InformeEjecucion;

/* Presenta informes y comparaciones sin cambiar datos. La crea MainWindow.crearPanelEstadisticas. */
public final class PanelEstadisticas extends JPanel {
    private static final Color FONDO = new Color(242, 246, 251);
    private static final Color AZUL = new Color(37, 99, 235);
    private static final Color TEXTO = new Color(28, 42, 60);
    private final JTable procesos;
    private final JTable historial;
    private final List<InformeEjecucion> guardados;
    private final JLabel[] tarjetas = new JLabel[7];
    private final JPanel cpus = new JPanel();
    private final JTextArea rendimiento = area();
    private final JTextArea contextoProceso = area();
    private final JLabel aviso = new JLabel("Selecciona dos ejecuciones.");
    private final JTextArea comparacion = area();
    private final JTextArea detalleHistorico = area();
    private final JTextArea revision = area();
    private final JTextArea revisionComparada = area();
    private JButton guardar;
    private final JComboBox<Integer> referencia = new JComboBox<>();
    private final JComboBox<Integer> comparada = new JComboBox<>();
    private final JComboBox<String> metrica = new JComboBox<>(new String[] {
            "Retorno promedio", "Utilización de CPU", "Tiempo ocioso", "Retorno/servicio promedio"
    });
    private final JProgressBar barraA = new JProgressBar(0, 1000);
    private final JProgressBar barraB = new JProgressBar(0, 1000);
    private final JLabel textoA = new JLabel("Referencia: —");
    private final JLabel textoB = new JLabel("Comparada: —");
    private InformeEjecucion actual;
    private boolean actualizando;

    public PanelEstadisticas(JTable procesos, JTable historial, JTextArea contextoAnterior,
            List<InformeEjecucion> guardados, Runnable accionGuardar) {
        super(new BorderLayout(8, 8));
        this.procesos = procesos;
        this.historial = historial;
        this.guardados = guardados;
        setBackground(FONDO);
        setBorder(new EmptyBorder(10, 12, 10, 12));
        JTabbedPane pestañas = new JTabbedPane();
        pestañas.addTab("Actual", crearActual(accionGuardar));
        pestañas.addTab("Comparar", crearComparacion());
        pestañas.addTab("Contexto", crearContexto(contextoAnterior));
        add(pestañas, BorderLayout.CENTER);
    }

    /* Organiza tarjetas, ocupación y tabla. Lo llama el constructor. */
    private JPanel crearActual(Runnable accionGuardar) {
        JPanel panel = nuevoPanel(new BorderLayout(8, 8));
        JPanel encabezado = nuevoPanel();
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));
        JPanel tarjetasPanel = nuevoPanel(new GridLayout(0, 4, 8, 8));
        String[] titulos = { "Política utilizada", "Tiempo simulado", "Finalizados normalmente",
                "Pendientes", "Errores", "Retorno promedio", "Retorno/servicio promedio" };
        String[] ayudas = {
                "Políticas registradas durante la simulación; varias indican una ejecución mixta.",
                "Tiempo transcurrido en ticks simulados, no tiempo real.",
                "Procesos terminados sin error.", "Procesos que aún no han finalizado.",
                "Procesos finalizados con error.",
                "Promedio de fin menos llegada, solo para finalizaciones normales.",
                "Promedio de retorno dividido por tiempo de CPU; solo procesos normales con servicio positivo."
        };
        for (int i = 0; i < titulos.length; i++) {
            JPanel tarjeta = new JPanel(new BorderLayout(4, 4));
            tarjeta.setBackground(Color.WHITE);
            tarjeta.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 229, 240)), new EmptyBorder(8, 10, 8, 10)));
            JLabel titulo = new JLabel(titulos[i]);
            titulo.setForeground(new Color(85, 101, 120));
            titulo.setToolTipText(ayudas[i]);
            tarjetas[i] = new JLabel("—");
            tarjetas[i].setFont(tarjetas[i].getFont().deriveFont(Font.BOLD, 16f));
            tarjetas[i].setForeground(AZUL);
            tarjetas[i].setToolTipText(ayudas[i]);
            tarjeta.add(titulo, BorderLayout.NORTH);
            tarjeta.add(tarjetas[i], BorderLayout.CENTER);
            tarjetasPanel.add(tarjeta);
        }
        encabezado.add(tarjetasPanel);
        JLabel cpuTitulo = titulo("Rendimiento de las CPU");
        cpuTitulo.setBorder(new EmptyBorder(12, 0, 4, 0));
        encabezado.add(cpuTitulo);
        cpus.setLayout(new BoxLayout(cpus, BoxLayout.Y_AXIS));
        cpus.setOpaque(false);
        encabezado.add(cpus);
        JButton botonGuardar = new JButton("Guardar resultado");
        botonGuardar.setToolTipText("Conserva la ejecución finalizada en el historial de esta sesión.");
        botonGuardar.addActionListener(e -> accionGuardar.run());
        guardar = botonGuardar;
        JPanel acciones = nuevoPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acciones.add(botonGuardar);
        encabezado.add(acciones);
        JScrollPane superior = new JScrollPane(encabezado);
        superior.setBorder(null);
        superior.setPreferredSize(new Dimension(700, 250));
        panel.add(superior, BorderLayout.NORTH);

        configurarProcesos();
        JTabbedPane detalles = new JTabbedPane();
        detalles.addTab("Rendimiento", new JScrollPane(rendimiento));
        detalles.addTab("Contexto de ejecución", new JScrollPane(contextoProceso));
        detalles.addTab("Revisión de datos", new JScrollPane(revision));
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(procesos), detalles);
        division.setResizeWeight(.65);
        division.setDividerLocation(200);
        division.setBorder(null);
        panel.add(division, BorderLayout.CENTER);
        return panel;
    }

    /* Prepara nombres, anchos, orden y ayuda de la tabla. Lo llama crearActual. */
    private void configurarProcesos() {
        procesos.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        procesos.setRowHeight(30);
        procesos.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        procesos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<javax.swing.table.TableModel> sorter = new TableRowSorter<>(procesos.getModel());
        for (int i : new int[] {0, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12}) {
            sorter.setComparator(i, comparadorNumero());
        }
        procesos.setRowSorter(sorter);
        procesos.setDefaultRenderer(Object.class, new RenderizadorDatos());
        int[] anchos = {55, 160, 50, 70, 70, 95, 100, 80, 110, 170, 150, 140, 130};
        for (int i = 0; i < anchos.length; i++) procesos.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        instalarAyuda(procesos, new String[] {
                "Identificador del proceso.", "Programa ASM ejecutado.", "CPU asignada.",
                "Tick de llegada efectiva.", "Tick de primera ejecución.", "Tick de finalización.",
                "Tiempo de servicio: ticks de CPU realmente consumidos.",
                "Tiempo de retorno: finalización menos llegada.",
                "Retorno dividido entre tiempo de servicio; no se calcula si el servicio es cero.",
                "Resultado o detalle del error.", "Servicio contabilizado por Guaranteed Scheduling.",
                "Cuota justa acumulada mientras el proceso podía competir.",
                "Servicio contabilizado dividido por la cuota justa acumulada."
        });
        procesos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !actualizando) actualizarDetalleProceso();
        });
    }

    /* Presenta el historial y los selectores de comparación. Lo llama el constructor. */
    private JPanel crearComparacion() {
        JPanel panel = nuevoPanel(new BorderLayout(8, 8));
        JPanel selectores = nuevoPanel(new FlowLayout(FlowLayout.LEFT));
        selectores.add(new JLabel("Referencia:"));
        referencia.setToolTipText("Ejecución que sirve de base para la comparación.");
        selectores.add(referencia);
        selectores.add(new JLabel("Comparar con:"));
        comparada.setToolTipText("Ejecución que se compara con la referencia.");
        selectores.add(comparada);
        referencia.addActionListener(e -> actualizarComparacion());
        comparada.addActionListener(e -> actualizarComparacion());
        panel.add(selectores, BorderLayout.NORTH);
        historial.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        historial.setRowHeight(28);
        historial.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        historial.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<javax.swing.table.TableModel> sorter = new TableRowSorter<>(historial.getModel());
        for (int i : new int[] {0, 2, 3, 4, 5, 6, 7, 8, 9}) sorter.setComparator(i, comparadorNumero());
        historial.setRowSorter(sorter);
        historial.setDefaultRenderer(Object.class, new RenderizadorDatos());
        int[] anchos = {45, 300, 60, 75, 75, 85, 80, 95, 120, 150, 200};
        for (int i = 0; i < anchos.length; i++) historial.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        instalarAyuda(historial, new String[] {
                "Número de ejecución.", "Políticas y opciones usadas.", "Cantidad de CPU.",
                "RAM configurada.", "Disco configurado.", "Cantidad de procesos.",
                "Duración simulada en ticks.", "Suma del ocio de todas las CPU, en CPU-ticks.",
                "Retorno promedio de procesos terminados normalmente.",
                "Promedio de retorno/servicio para procesos terminados normalmente.", "Comprobación frente a la referencia inicial."
        });
        historial.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !actualizando && historial.getSelectedRow() >= 0) {
                int modelo = historial.convertRowIndexToModel(historial.getSelectedRow());
                if (modelo < comparada.getItemCount()) comparada.setSelectedIndex(modelo);
            }
        });
        JPanel resultado = nuevoPanel();
        resultado.setLayout(new BoxLayout(resultado, BoxLayout.Y_AXIS));
        aviso.setForeground(new Color(155, 91, 15));
        aviso.setBorder(new EmptyBorder(5, 5, 5, 5));
        resultado.add(aviso);
        comparacion.setRows(8);
        resultado.add(new JScrollPane(comparacion));
        JPanel eleccion = nuevoPanel(new FlowLayout(FlowLayout.LEFT));
        eleccion.add(new JLabel("Gráfico:"));
        metrica.setToolTipText("Métrica que representan las barras comparativas.");
        eleccion.add(metrica);
        resultado.add(eleccion);
        metrica.addActionListener(e -> actualizarComparacion());
        configurarBarra(barraA);
        configurarBarra(barraB);
        resultado.add(textoA);
        resultado.add(barraA);
        resultado.add(textoB);
        resultado.add(barraB);
        JTabbedPane vistasResultado = new JTabbedPane();
        vistasResultado.addTab("Comparación", new JScrollPane(resultado));
        vistasResultado.addTab("Procesos y contexto", new JScrollPane(detalleHistorico));
        vistasResultado.addTab("Revisión de datos", new JScrollPane(revisionComparada));
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(historial), vistasResultado);
        division.setResizeWeight(.45);
        division.setDividerLocation(190);
        division.setBorder(null);
        panel.add(division, BorderLayout.CENTER);
        return panel;
    }

    /* Explica las métricas y conserva el contexto técnico. Lo llama el constructor. */
    private JTabbedPane crearContexto(JTextArea contextoAnterior) {
        JTabbedPane pestañas = new JTabbedPane();
        JEditorPane guia = new JEditorPane("text/html",
                "<html><body style='font-family:sans-serif;padding:14px;color:#1c2a3c'>"
                + "<h2>Cómo leer los resultados</h2>"
                + "<h3>Tiempos de los procesos</h3>"
                + "<p><b>Llegada:</b> tick en que el proceso está disponible. <b>Inicio:</b> primera asignación de CPU. "
                + "<b>Finalización:</b> tick registrado al terminar. <b>Tiempo de CPU:</b> ticks de CPU usados.</p>"
                + "<p><b>Retorno:</b> finalización menos llegada. <b>Retorno/servicio:</b> retorno dividido por el "
                + "tiempo de CPU; valores próximos a 1 indican menos tiempo relativo fuera de CPU. El retorno incluye "
                + "posibles bloqueos.</p>"
                + "<h3>Rendimiento de CPU</h3><p>Ocupación y ocio son ticks registrados para cada CPU. "
                + "La utilización es ocupación dividida por los ticks registrados.</p>"
                + "<h3>Justicia</h3><p>En Guaranteed Scheduling, la cuota justa acumulada es el servicio que "
                + "correspondería al proceso mientras podía competir; el servicio/cuota justa divide el servicio "
                + "contabilizado entre esa cuota.</p>"
                + "<h3>Comparación</h3><p>Un menor retorno u ocio puede ser deseable, pero ninguna métrica aislada "
                + "demuestra que un algoritmo sea mejor. Comprueba recursos, programas, llegadas y configuración. "
                + "Los tiempos reales no son ticks simulados.</p></body></html>");
        guia.setEditable(false);
        pestañas.addTab("Guía de lectura", new JScrollPane(guia));
        contextoAnterior.setLineWrap(true);
        contextoAnterior.setWrapStyleWord(true);
        pestañas.addTab("Contexto de la simulación", new JScrollPane(contextoAnterior));
        return pestañas;
    }

    /* Actualiza indicadores y conserva la selección. Lo llama MainWindow.actualizarInforme. */
    public void actualizar(InformeEjecucion informe) {
        this.actual = informe;
        boolean mixto = informe.algoritmos.contains(" → ");
        tarjetas[0].setText(mixto ? "Políticas mixtas" : informe.algoritmos);
        tarjetas[0].setToolTipText(informe.algoritmos);
        tarjetas[1].setText(informe.tiempo + " ticks");
        long normales = informe.procesos.stream().filter(p -> "Normal".equals(p.resultado())).count();
        long errores = informe.procesos.stream().filter(p -> p.resultado().startsWith("Error:")).count();
        tarjetas[2].setText(Long.toString(normales));
        tarjetas[3].setText(Long.toString(informe.procesos.size() - normales - errores));
        tarjetas[4].setText(Long.toString(errores));
        tarjetas[5].setText(informe.estanciaMedia == null
                ? "—" : InformeEjecucion.numero(informe.estanciaMedia) + " ticks");
        tarjetas[6].setText(informe.proporcionMedia == null
                ? "—" : InformeEjecucion.numero(informe.proporcionMedia) + " veces");
        cpus.removeAll();
        for (InformeEjecucion.UsoCPU cpu : informe.cpus) {
            int total = cpu.ocupado() + cpu.ocioso();
            double uso = total == 0 ? 0 : 100.0 * cpu.ocupado() / total;
            long cantidad = informe.procesos.stream().filter(p -> p.cpu() == cpu.cpu() && p.servicio() > 0).count();
            JPanel fila = nuevoPanel(new BorderLayout(8, 0));
            JLabel nombre = new JLabel("CPU " + cpu.cpu());
            JProgressBar barra = new JProgressBar(0, 1000);
            configurarBarra(barra);
            barra.setValue((int) Math.round(uso * 10));
            barra.setString(total == 0 ? "Sin datos" : String.format(java.util.Locale.ROOT, "%.1f %%", uso));
            barra.setStringPainted(true);
            JLabel datos = new JLabel("Ocupada: " + cpu.ocupado() + " · Ociosa: " + cpu.ocioso()
                    + " ticks · Procesos: " + cantidad);
            String ayudaCpu = "Utilización = ticks ocupados ÷ (ocupados + ociosos).";
            nombre.setToolTipText(ayudaCpu);
            barra.setToolTipText(ayudaCpu);
            datos.setToolTipText(ayudaCpu);
            fila.setToolTipText(ayudaCpu);
            fila.add(nombre, BorderLayout.WEST);
            fila.add(barra, BorderLayout.CENTER);
            fila.add(datos, BorderLayout.EAST);
            cpus.add(fila);
        }
        cpus.revalidate();
        cpus.repaint();
        boolean finalizada = !informe.procesos.isEmpty()
                && informe.procesos.stream().noneMatch(p -> "Pendiente".equals(p.resultado()))
                && informe.tiempo > 0;
        setPuedeGuardar(finalizada);
        revision.setText(formatearRevision(informe, "la ejecución actual"));
        revision.setCaretPosition(0);
        actualizarDetalleProceso();
        actualizarHistorial();
    }

    /* Habilita el guardado solo cuando todos los procesos terminaron. Lo llama actualizar. */
    public void setPuedeGuardar(boolean puede) {
        if (guardar != null) guardar.setEnabled(puede);
    }

    /* Restituye los selectores sin perder la referencia elegida. La llaman actualizar y guardarResultado. */
    public void actualizarHistorial() {
        if (actualizando) return;
        actualizando = true;
        int a = referencia.getSelectedIndex(), b = comparada.getSelectedIndex();
        referencia.removeAllItems();
        comparada.removeAllItems();
        for (int i = 0; i < guardados.size(); i++) {
            referencia.addItem(i + 1);
            comparada.addItem(i + 1);
        }
        if (!guardados.isEmpty()) {
            referencia.setSelectedIndex(a >= 0 && a < guardados.size() ? a : 0);
            comparada.setSelectedIndex(b >= 0 && b < guardados.size() ? b : guardados.size() - 1);
        }
        actualizando = false;
        actualizarComparacion();
    }

    /* Separa métricas y contexto del proceso seleccionado. Lo llama actualizar y la tabla. */
    private void actualizarDetalleProceso() {
        int fila = procesos.getSelectedRow();
        if (actual == null || fila < 0) {
            rendimiento.setText("Selecciona un proceso para consultar sus resultados.");
            contextoProceso.setText("Selecciona un proceso para consultar su contexto.");
            return;
        }
        int modelo = procesos.convertRowIndexToModel(fila);
        int pid = ((Number) procesos.getModel().getValueAt(modelo, 0)).intValue();
        for (InformeEjecucion.Proceso p : actual.procesos) if (p.pid() == pid) {
            String texto = "PID " + p.pid() + " · " + p.nombre() + " · CPU " + p.cpu()
                    + "\nEstado: " + p.estado() + " · Resultado: " + p.resultado()
                    + "\nLlegada: " + p.llegada() + " ticks · Inicio: " + tiempo(p.inicio())
                    + " · Finalización: " + tiempo(p.fin())
                    + "\nTiempo de CPU: " + p.servicio() + " ticks"
                    + "\nTiempo de retorno: " + InformeEjecucion.numero(p.estancia()) + " ticks"
                    + "\nRetorno/servicio: " + InformeEjecucion.numero(p.proporcion());
            if (p.servicioGarantizado() != null) texto += "\n\nReparto garantizado"
                    + "\nCPU contabilizada: " + p.servicioGarantizado() + " ticks"
                    + "\nCuota justa acumulada: " + InformeEjecucion.numero(p.parteJusta()) + " ticks"
                    + "\nServicio/cuota justa: " + InformeEjecucion.numero(p.relacionGarantizada());
            rendimiento.setText(texto);
            contextoProceso.setText(p.contexto() == null ? "Contexto no disponible." : p.contexto());
            return;
        }
    }

    /* Compara recursos y carga comprobada. Lo llama actualizarHistorial y los selectores. */
    private void actualizarComparacion() {
        if (actualizando) return;
        int ia = referencia.getSelectedIndex(), ib = comparada.getSelectedIndex();
        if (ia < 0 || ib < 0 || ia >= guardados.size() || ib >= guardados.size()) {
            mostrarAviso("Selecciona dos ejecuciones guardadas.");
            comparacion.setText("Las ejecuciones terminadas se guardan automáticamente durante esta sesión.");
            actualizarBarras(null, null);
            return;
        }
        InformeEjecucion a = guardados.get(ia), b = guardados.get(ib);
        List<String> diferencias = new ArrayList<>();
        if (a.cpus.size() != b.cpus.size()) diferencias.add("CPU");
        if (a.ram != b.ram) diferencias.add("RAM");
        if (a.disco != b.disco) diferencias.add("disco");
        if (a.procesos.size() != b.procesos.size()) diferencias.add("cantidad de procesos");
        if (!a.cargaVerificable || !b.cargaVerificable) diferencias.add("contenido no verificado");
        else if (!a.firmaCarga.equals(b.firmaCarga)) diferencias.add("programas o llegadas");
        boolean comparables = diferencias.isEmpty();
        mostrarAviso(comparables
                ? "Coinciden recursos, programas, llegadas y valores iniciales. Comprueba entradas y archivos de datos."
                : "Las simulaciones usan condiciones diferentes. Los resultados pueden no ser directamente comparables: "
                        + String.join(", ", diferencias));
        StringBuilder texto = new StringBuilder("Referencia #").append(ia + 1).append(": ").append(a.algoritmos)
                .append("\nComparada #").append(ib + 1).append(": ").append(b.algoritmos)
                .append("\nCPU: ").append(a.cpus.size()).append(" / ").append(b.cpus.size())
                .append(" · RAM: ").append(a.ram).append(" / ").append(b.ram)
                .append(" · disco: ").append(a.disco).append(" / ").append(b.disco)
                .append(" · procesos: ").append(a.procesos.size()).append(" / ").append(b.procesos.size());
        agregarComparacion(texto, "Tiempo simulado", (double) a.tiempo, (double) b.tiempo, "ticks", comparables);
        agregarComparacion(texto, "Ocio total", (double) ocio(a), (double) ocio(b), "CPU-ticks", comparables);
        agregarComparacion(texto, "Retorno promedio", a.estanciaMedia, b.estanciaMedia, "ticks", comparables);
        agregarComparacion(texto, "Retorno/servicio promedio", a.proporcionMedia, b.proporcionMedia, "veces", comparables);
        texto.append("\nFinalización normal: ").append(normales(a)).append(" / ").append(normales(b))
                .append(" · Errores: ").append(errores(a)).append(" / ").append(errores(b))
                .append("\nLa diferencia numérica no determina por sí sola qué política es mejor.");
        comparacion.setText(texto.toString());
        comparacion.setCaretPosition(0);
        StringBuilder detalle = new StringBuilder("Ejecución comparada #").append(ib + 1)
                .append("\nPolíticas: ").append(b.algoritmos)
                .append("\nLos tiempos son simulados salvo las horas y duración real indicadas.\n");
        for (InformeEjecucion.Proceso p : b.procesos) {
            detalle.append("\nPID ").append(p.pid()).append(" · ").append(p.nombre())
                    .append(" · CPU ").append(p.cpu()).append("\nEstado: ").append(p.estado())
                    .append(" · ").append(p.resultado()).append("\nLlegada: ").append(p.llegada())
                    .append(" · Inicio: ").append(tiempo(p.inicio())).append(" · Fin: ").append(tiempo(p.fin()))
                    .append("\nServicio: ").append(p.servicio()).append(" ticks · Retorno: ")
                    .append(InformeEjecucion.numero(p.estancia())).append(" · Retorno/servicio: ")
                    .append(InformeEjecucion.numero(p.proporcion()));
            if (p.servicioGarantizado() != null) detalle.append("\nServicio contabilizado: ")
                    .append(p.servicioGarantizado()).append(" · Cuota justa acumulada: ")
                    .append(InformeEjecucion.numero(p.parteJusta())).append(" · Servicio/cuota justa: ")
                    .append(InformeEjecucion.numero(p.relacionGarantizada()));
            detalle.append("\nContexto:\n").append(p.contexto() == null ? "No disponible." : p.contexto()).append("\n");
        }
        detalleHistorico.setText(detalle.toString());
        detalleHistorico.setCaretPosition(0);
        revisionComparada.setText(formatearRevision(a, "la referencia #" + (ia + 1))
                + "\n\n" + formatearRevision(b, "la ejecución comparada #" + (ib + 1)));
        revisionComparada.setCaretPosition(0);
        actualizarBarras(a, b);
    }

    /* Muestra un aviso con ancho limitado. Lo llama actualizarComparacion. */
    private void mostrarAviso(String texto) {
        aviso.setText("<html><div style='width:640px'>" + texto + "</div></html>");
        aviso.setToolTipText(texto);
    }

    /* Prepara el texto de revisión sin corregir datos. Lo llaman actualizar y actualizarComparacion. */
    private static String formatearRevision(InformeEjecucion informe, String titulo) {
        StringBuilder texto = new StringBuilder("Revisión de ").append(titulo).append("\n\n");
        List<InformeEjecucion.Hallazgo> inconsistencias = new ArrayList<>();
        List<InformeEjecucion.Hallazgo> avisos = new ArrayList<>();
        for (InformeEjecucion.Hallazgo hallazgo : informe.hallazgos) {
            if ("Inconsistencia".equals(hallazgo.nivel())) {
                inconsistencias.add(hallazgo);
            } else {
                avisos.add(hallazgo);
            }
        }
        if (inconsistencias.isEmpty()) {
            texto.append("Inconsistencias de cálculo: ninguna detectada.\n");
        } else {
            texto.append("Inconsistencias de cálculo:\n");
            for (InformeEjecucion.Hallazgo hallazgo : inconsistencias) {
                texto.append("• ").append(hallazgo.mensaje()).append('\n');
            }
        }
        texto.append("\nAvisos y limitaciones:\n");
        if (avisos.isEmpty()) {
            texto.append("• Ninguno.\n");
        } else {
            for (InformeEjecucion.Hallazgo hallazgo : avisos) {
                texto.append("• ").append(hallazgo.mensaje()).append('\n');
            }
        }
        texto.append("\nEsta revisión solo informa; no corrige fórmulas, valores ni promedios.");
        return texto.toString();
    }

    /* Añade una métrica comparada con su diferencia. Lo llama actualizarComparacion. */
    private static void agregarComparacion(StringBuilder t, String nombre, Double a, Double b, String unidad, boolean comparable) {
        t.append("\n").append(nombre).append(": ").append(InformeEjecucion.numero(a)).append(" / ")
                .append(InformeEjecucion.numero(b)).append(" ").append(unidad);
        if (comparable && a != null && b != null) t.append(" · diferencia ")
                .append(String.format(java.util.Locale.ROOT, "%+.2f", b - a)).append(" ").append(unidad);
    }

    /* Dibuja barras proporcionales con valor y unidad visibles. Lo llama actualizarComparacion. */
    private void actualizarBarras(InformeEjecucion a, InformeEjecucion b) {
        Double va = valorMetrica(a), vb = valorMetrica(b);
        double max = Math.max(va == null ? 0 : va, vb == null ? 0 : vb);
        String unidad = metrica.getSelectedIndex() == 1 ? " %"
                : metrica.getSelectedIndex() == 2 ? " CPU-ticks"
                : metrica.getSelectedIndex() == 3 ? " veces" : " ticks";
        barraA.setValue(max <= 0 || va == null ? 0 : (int) Math.round(1000 * va / max));
        barraB.setValue(max <= 0 || vb == null ? 0 : (int) Math.round(1000 * vb / max));
        textoA.setText("Referencia: " + InformeEjecucion.numero(va) + unidad);
        textoB.setText("Comparada: " + InformeEjecucion.numero(vb) + unidad);
    }

    private Double valorMetrica(InformeEjecucion i) {
        if (i == null) return null;
        return switch (metrica.getSelectedIndex()) {
            case 0 -> i.estanciaMedia;
            case 1 -> {
                int ocupado = i.cpus.stream().mapToInt(InformeEjecucion.UsoCPU::ocupado).sum();
                int total = ocupado + ocio(i);
                yield total == 0 ? null : 100.0 * ocupado / total;
            }
            case 2 -> (double) ocio(i);
            default -> i.proporcionMedia;
        };
    }

    private static int ocio(InformeEjecucion i) { return i.cpus.stream().mapToInt(InformeEjecucion.UsoCPU::ocioso).sum(); }
    private static long normales(InformeEjecucion i) { return i.procesos.stream().filter(p -> "Normal".equals(p.resultado())).count(); }
    private static long errores(InformeEjecucion i) { return i.procesos.stream().filter(p -> p.resultado().startsWith("Error:")).count(); }
    private static String tiempo(int t) { return t < 0 ? "—" : t + " ticks"; }
    private static JPanel nuevoPanel() { JPanel p = new JPanel(); p.setBackground(FONDO); return p; }
    private static JPanel nuevoPanel(LayoutManager l) { JPanel p = new JPanel(l); p.setBackground(FONDO); return p; }
    private static JLabel titulo(String s) { JLabel l = new JLabel(s); l.setFont(l.getFont().deriveFont(Font.BOLD, 14f)); l.setForeground(TEXTO); return l; }
    private static JTextArea area() { JTextArea a = new JTextArea(); a.setEditable(false); a.setLineWrap(true); a.setWrapStyleWord(true); a.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13)); a.setBorder(new EmptyBorder(8, 10, 8, 10)); return a; }
    private static void configurarBarra(JProgressBar b) { b.setForeground(AZUL); b.setBackground(new Color(225, 232, 242)); b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20)); }

    private static Comparator<Object> comparadorNumero() {
        return (a, b) -> Double.compare(a instanceof Number ? ((Number) a).doubleValue() : parsear(a),
                b instanceof Number ? ((Number) b).doubleValue() : parsear(b));
    }
    private static double parsear(Object valor) {
        try { return Double.parseDouble(String.valueOf(valor)); } catch (RuntimeException ex) { return Double.NEGATIVE_INFINITY; }
    }

    /* Muestra ayuda por columna y conserva el encabezado. La llaman configurarProcesos y crearComparacion. */
    private static void instalarAyuda(JTable tabla, String[] ayudas) {
        JTableHeader cabecera = new JTableHeader(tabla.getColumnModel()) {
            @Override public String getToolTipText(MouseEvent e) {
                int columna = columnAtPoint(e.getPoint());
                if (columna < 0) return null;
                int modelo = getColumnModel().getColumn(columna).getModelIndex();
                return modelo < ayudas.length ? ayudas[modelo] : null;
            }
        };
        cabecera.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        tabla.setTableHeader(cabecera);
    }

    /* Alinea números y muestra el contenido completo como tooltip. Lo usa la JTable. */
    private static class RenderizadorDatos extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable tabla, Object valor,
                boolean seleccionado, boolean foco, int fila, int columna) {
            Object mostrado = valor == null ? "—" : valor instanceof Double d ? InformeEjecucion.numero(d) : valor;
            super.getTableCellRendererComponent(tabla, mostrado, seleccionado, foco, fila, columna);
            setHorizontalAlignment(valor instanceof Number ? RIGHT : LEFT);
            setToolTipText(valor == null ? "Sin datos" : String.valueOf(valor));
            if (!seleccionado) setBackground(fila % 2 == 0 ? Color.WHITE : new Color(246, 249, 253));
            return this;
        }
    }
}
