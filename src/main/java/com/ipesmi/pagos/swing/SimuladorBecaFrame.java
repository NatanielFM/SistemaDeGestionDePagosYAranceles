package com.ipesmi.pagos.swing;

import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.MedioPago;
import com.ipesmi.pagos.util.Formatos;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Formulario Swing (Unidad 5): simula cuánto pagaría un estudiante según el
 * concepto, el porcentaje de beca y el medio de pago, sin registrar nada.
 * <p>
 * Layout managers: BorderLayout (ventana), GridBagLayout (formulario),
 * GridLayout (resultados) y FlowLayout (botones). Eventos: ActionListener,
 * ChangeListener y DocumentListener; todo se recalcula en vivo.
 */
public class SimuladorBecaFrame extends JFrame {

    private static final Color TINTA = new Color(0x15141A);
    private static final Color PAPEL = new Color(0xF6F4EF);
    private static final Color APAGADO = new Color(0x7B7984);
    private static final Color CORAL = new Color(0xFF5533);
    private static final Color INDIGO = new Color(0x4B3BFF);
    private static final Color LINEA = new Color(0xE5E2DA);
    private static final Font BASE = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font ETIQUETA = new Font("Segoe UI", Font.BOLD, 11);

    private final JComboBox<Concepto> cmbConcepto = new JComboBox<>();
    private final JTextField txtMonto = new JTextField(12);
    private final JSlider sldBeca = new JSlider(0, 100, 0);
    private final JLabel lblBeca = new JLabel("0 %");
    private final JLabel lblAvisoBeca = new JLabel(" ");
    private final JComboBox<MedioPago> cmbMedio = new JComboBox<>(MedioPago.values());
    private final JSpinner spCuotas = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));

    private final JLabel resBase = valor();
    private final JLabel resDescuento = valor();
    private final JLabel resTotal = valor();
    private final JLabel resCuota = valor();

    public SimuladorBecaFrame(List<Concepto> conceptos) {
        super("Simulador de becas");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(PAPEL);
        setLayout(new BorderLayout());

        conceptos.forEach(cmbConcepto::addItem);
        cmbConcepto.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Concepto c) {
                    setText(c.getDescripcion() + "  ·  " + Formatos.moneda(c.getMonto()));
                }
                return this;
            }
        });

        add(encabezado(), BorderLayout.NORTH);
        add(formulario(), BorderLayout.CENTER);
        add(pie(), BorderLayout.SOUTH);

        registrarEventos();
        if (cmbConcepto.getItemCount() > 0) {
            cmbConcepto.setSelectedIndex(0);
        }
        seleccionarConcepto();

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- vista

    private JComponent encabezado() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBackground(TINTA);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));
        JLabel titulo = new JLabel("Simulador de becas");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Calcula el importe final sin registrar el pago");
        sub.setFont(BASE);
        sub.setForeground(new Color(0xB9B7C2));
        panel.add(titulo);
        panel.add(sub);
        return panel;
    }

    private JComponent formulario() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(18, 22, 8, 22));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 0, 6, 12);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        estilizar(cmbConcepto, txtMonto, cmbMedio, spCuotas);
        sldBeca.setOpaque(false);
        sldBeca.setMajorTickSpacing(25);
        sldBeca.setMinorTickSpacing(5);
        sldBeca.setSnapToTicks(true);
        sldBeca.setPaintTicks(true);
        sldBeca.setForeground(APAGADO);
        lblBeca.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblBeca.setForeground(INDIGO);
        lblAvisoBeca.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblAvisoBeca.setForeground(APAGADO);

        int fila = 0;
        agregar(form, g, fila++, "CONCEPTO", cmbConcepto);
        agregar(form, g, fila++, "MONTO BASE", txtMonto);

        JPanel beca = new JPanel(new BorderLayout(10, 0));
        beca.setOpaque(false);
        beca.add(sldBeca, BorderLayout.CENTER);
        beca.add(lblBeca, BorderLayout.EAST);
        agregar(form, g, fila++, "BECA", beca);

        g.gridx = 1;
        g.gridy = fila++;
        g.insets = new Insets(0, 0, 6, 0);
        form.add(lblAvisoBeca, g);
        g.insets = new Insets(6, 0, 6, 12);

        agregar(form, g, fila++, "MEDIO DE PAGO", cmbMedio);
        agregar(form, g, fila, "CUOTAS", spCuotas);
        return form;
    }

    private void agregar(JPanel form, GridBagConstraints g, int fila, String etiqueta, JComponent campo) {
        JLabel l = new JLabel(etiqueta);
        l.setFont(ETIQUETA);
        l.setForeground(APAGADO);
        g.gridx = 0;
        g.gridy = fila;
        g.weightx = 0;
        form.add(l, g);
        g.gridx = 1;
        g.weightx = 1;
        form.add(campo, g);
    }

    private JComponent pie() {
        JPanel resultados = new JPanel(new GridLayout(4, 2, 0, 8));
        resultados.setBackground(Color.WHITE);
        resultados.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, LINEA),
                BorderFactory.createEmptyBorder(16, 22, 16, 22)));
        resultados.add(etiquetaResultado("Importe base"));
        resultados.add(resBase);
        resultados.add(etiquetaResultado("Descuento por beca"));
        resultados.add(resDescuento);
        resultados.add(etiquetaResultado("Total a pagar"));
        resultados.add(resTotal);
        resultados.add(etiquetaResultado("Valor de cada cuota"));
        resultados.add(resCuota);
        resTotal.setFont(new Font("Segoe UI", Font.BOLD, 22));
        resTotal.setForeground(CORAL);

        JButton restablecer = boton("Restablecer", Color.WHITE, TINTA);
        restablecer.addActionListener(e -> restablecer());
        JButton cerrar = boton("Cerrar", TINTA, Color.WHITE);
        cerrar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        botones.setOpaque(false);
        botones.add(restablecer);
        botones.add(cerrar);

        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(resultados, BorderLayout.CENTER);
        sur.add(botones, BorderLayout.SOUTH);
        return sur;
    }

    private static JLabel valor() {
        JLabel l = new JLabel("—", JLabel.RIGHT);
        l.setFont(new Font("Consolas", Font.PLAIN, 14));
        l.setForeground(TINTA);
        return l;
    }

    private static JLabel etiquetaResultado(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(BASE);
        l.setForeground(APAGADO);
        return l;
    }

    private static JButton boton(String texto, Color fondo, Color frente) {
        JButton b = new JButton(texto);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(fondo);
        b.setForeground(frente);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TINTA), BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static void estilizar(JComponent... campos) {
        for (JComponent c : campos) {
            c.setFont(BASE);
            c.setBackground(Color.WHITE);
        }
        for (JComponent c : campos) {
            if (c instanceof JTextField t) {
                t.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(LINEA), BorderFactory.createEmptyBorder(5, 8, 5, 8)));
            }
        }
    }

    // -------------------------------------------------------------- eventos

    private void registrarEventos() {
        cmbConcepto.addActionListener(e -> seleccionarConcepto());
        cmbMedio.addActionListener(e -> {
            spCuotas.setEnabled(cmbMedio.getSelectedItem() == MedioPago.TARJETA);
            if (!spCuotas.isEnabled()) {
                spCuotas.setValue(1);
            }
            recalcular();
        });
        sldBeca.addChangeListener(e -> {
            lblBeca.setText(sldBeca.getValue() + " %");
            recalcular();
        });
        spCuotas.addChangeListener(e -> recalcular());
        txtMonto.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcular(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcular(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcular(); }
        });
        spCuotas.setEnabled(false);
    }

    private void seleccionarConcepto() {
        Concepto c = (Concepto) cmbConcepto.getSelectedItem();
        if (c != null) {
            txtMonto.setText(c.getMonto().toPlainString().replace('.', ','));
        }
        recalcular();
    }

    private void restablecer() {
        sldBeca.setValue(0);
        cmbMedio.setSelectedItem(MedioPago.EFECTIVO);
        seleccionarConcepto();
    }

    private void recalcular() {
        Concepto c = (Concepto) cmbConcepto.getSelectedItem();
        boolean becable = c == null || c.isBecable();
        sldBeca.setEnabled(becable);
        lblAvisoBeca.setText(becable ? " " : "Este concepto no admite beca: se cobra completo.");

        BigDecimal base;
        try {
            base = Formatos.leerMonto(txtMonto.getText());
            txtMonto.setForeground(TINTA);
        } catch (Exception ex) {
            txtMonto.setForeground(CORAL);
            resBase.setText("—");
            resDescuento.setText("—");
            resTotal.setText("—");
            resCuota.setText("—");
            return;
        }
        int porcentaje = becable ? sldBeca.getValue() : 0;
        BigDecimal descuento = base.multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = base.subtract(descuento).setScale(2, RoundingMode.HALF_UP);
        int cuotas = (Integer) spCuotas.getValue();
        BigDecimal cuota = total.divide(BigDecimal.valueOf(cuotas), 2, RoundingMode.HALF_UP);

        resBase.setText(Formatos.moneda(base));
        resDescuento.setText(descuento.signum() == 0 ? "—" : "− " + Formatos.moneda(descuento));
        resTotal.setText(Formatos.moneda(total));
        resCuota.setText(cuotas + " × " + Formatos.moneda(cuota));
    }
}
