package minipc.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import minipc.services.InformeEjecucion;

/* Muestra únicamente las siete estadísticas de cada ejecución finalizada. */
public final class PanelEstadisticas extends JPanel {
    private static final Color FONDO = new Color(242, 246, 251);
    private static final Color TEXTO = new Color(28, 42, 60);
    private static final Color BORDE = new Color(221, 229, 238);
    private static final String[] METRICAS = {
        "Algoritmo", "Procesos ejecutados", "Tiempo inicial", "Tiempo final",
        "Tiempo ocioso", "Turnaround promedio", "Tr/Ts promedio"
    };

    private final List<InformeEjecucion> guardados;
    private final JTable tabla = new JTable() {
        @Override public boolean getScrollableTracksViewportWidth() {
            int minimo = 0;
            for (int i = 0; i < getColumnCount(); i++) {
                minimo += getColumnModel().getColumn(i).getMinWidth();
            }
            return getParent() != null && getParent().getWidth() >= minimo;
        }
    };
    private int ejecucionesMostradas = -1;

    public PanelEstadisticas(List<InformeEjecucion> guardados) {
        super(new BorderLayout());
        this.guardados = guardados;
        setBackground(FONDO);
        setBorder(new EmptyBorder(12, 12, 12, 12));
        tabla.setRowHeight(34);
        tabla.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        tabla.setBackground(Color.WHITE);
        tabla.setForeground(TEXTO);
        tabla.setGridColor(BORDE);
        tabla.setShowVerticalLines(false);
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setRowSelectionAllowed(false);
        tabla.setColumnSelectionAllowed(false);
        tabla.setDefaultRenderer(Object.class, new RenderizadorEstadisticas());
        tabla.getTableHeader().setFont(tabla.getFont().deriveFont(Font.BOLD));
        tabla.getTableHeader().setBackground(new Color(228, 233, 239));
        tabla.getTableHeader().setForeground(TEXTO);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 36));

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setColumnHeaderView(tabla.getTableHeader());
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);
        actualizarHistorial();
    }

    /* Añade una columna al terminar una simulación, conservando las anteriores. */
    public void actualizarHistorial() {
        if (ejecucionesMostradas == guardados.size()) return;
        ejecucionesMostradas = guardados.size();
        Object[] columnas = new Object[guardados.size() + 1];
        columnas[0] = "Estadística";
        Object[][] datos = new Object[METRICAS.length][columnas.length];
        for (int fila = 0; fila < METRICAS.length; fila++) datos[fila][0] = METRICAS[fila];
        for (int i = 0; i < guardados.size(); i++) {
            InformeEjecucion informe = guardados.get(i);
            columnas[i + 1] = "Ejecución " + (i + 1);
            Object[] valores = {
                informe.algoritmos,
                informe.procesos.stream().filter(p -> p.inicio() >= 0).count(),
                0, // El reloj simulado comienza en cero.
                informe.tiempo,
                informe.cpus.stream().mapToInt(InformeEjecucion.UsoCPU::ocioso).sum(),
                informe.estanciaMedia,
                informe.proporcionMedia
            };
            for (int fila = 0; fila < valores.length; fila++) datos[fila][i + 1] = valores[fila];
        }
        tabla.setModel(new DefaultTableModel(datos, columnas) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        });
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            int ancho = i == 0 ? 210 : 160;
            for (int fila = 0; fila < tabla.getRowCount(); fila++) {
                Component celda = tabla.prepareRenderer(tabla.getCellRenderer(fila, i), fila, i);
                ancho = Math.max(ancho, celda.getPreferredSize().width + 4);
            }
            // Impide que el ajuste al ancho de la ventana recorte las políticas utilizadas.
            tabla.getColumnModel().getColumn(i).setMinWidth(ancho);
            tabla.getColumnModel().getColumn(i).setPreferredWidth(Math.max(ancho, i == 0 ? 250 : 220));
        }
    }

    private static final class RenderizadorEstadisticas extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable tabla, Object valor,
                boolean seleccionado, boolean foco, int fila, int columna) {
            Object mostrado = valor == null ? "—"
                    : valor instanceof Double d ? InformeEjecucion.numero(d) : valor;
            super.getTableCellRendererComponent(tabla, mostrado, seleccionado, foco, fila, columna);
            setHorizontalAlignment(LEFT);
            setBorder(new EmptyBorder(6, 12, 6, 12));
            setForeground(TEXTO);
            setBackground(fila % 2 == 0 ? Color.WHITE : new Color(247, 249, 252));
            setToolTipText(valor == null ? "Sin datos" : mostrado.toString());
            return this;
        }
    }
}
