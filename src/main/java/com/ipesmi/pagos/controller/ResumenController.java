package com.ipesmi.pagos.controller;

import com.ipesmi.pagos.model.EstadoPago;
import com.ipesmi.pagos.model.Estudiante;
import com.ipesmi.pagos.model.MedioPago;
import com.ipesmi.pagos.model.Pago;
import com.ipesmi.pagos.model.TipoConcepto;
import com.ipesmi.pagos.service.AppContext;
import com.ipesmi.pagos.service.PagoService;
import com.ipesmi.pagos.util.Formatos;
import com.ipesmi.pagos.view.Animaciones;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tablero con las cifras generales y el PieChart de distribución de pagos. */
public class ResumenController implements Refrescable {

    private static final Map<MedioPago, String> COLOR_MEDIO = new EnumMap<>(Map.of(
            MedioPago.EFECTIVO, "#FFB400",
            MedioPago.TARJETA, "#4B3BFF",
            MedioPago.TRANSFERENCIA, "#00B89C"));
    private static final Map<TipoConcepto, String> COLOR_TIPO = new EnumMap<>(Map.of(
            TipoConcepto.MATRICULA, "#FF5533",
            TipoConcepto.CUOTA, "#4B3BFF",
            TipoConcepto.EXAMEN, "#F0437E",
            TipoConcepto.OTRO, "#15141A"));
    private static final double ANCHO_BARRA = 330;

    @FXML private Label lblFecha;
    @FXML private VBox cajaRecaudado;
    @FXML private VBox cajaPendiente;
    @FXML private VBox cajaSaldo;
    @FXML private VBox cajaBecas;
    @FXML private Label lblRecaudado;
    @FXML private Label lblRecaudadoNota;
    @FXML private Label lblPendiente;
    @FXML private Label lblPendienteNota;
    @FXML private Label lblSaldo;
    @FXML private Label lblSaldoNota;
    @FXML private Label lblBecas;
    @FXML private Label lblBecasNota;
    @FXML private ToggleGroup grupoVista;
    @FXML private ToggleButton tbPorMedio;
    @FXML private PieChart grafico;
    @FXML private VBox leyenda;
    @FXML private VBox listaDeudores;

    private final PagoService pagos = AppContext.get().pagos();

    @FXML
    private void initialize() {
        lblFecha.setText(Formatos.fechaLarga(LocalDate.now()));
        grupoVista.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);   // siempre hay una opción elegida
            } else {
                cargarGrafico();
            }
        });
    }

    @Override
    @FXML
    public void refrescar() {
        List<Pago> lista = pagos.listar();
        List<Estudiante> estudiantes = AppContext.get().estudiantes().listar();

        BigDecimal recaudado = pagos.totalPorEstado(EstadoPago.ACREDITADO);
        BigDecimal pendiente = pagos.totalPorEstado(EstadoPago.PENDIENTE);
        BigDecimal saldo = pagos.saldoPendienteTotal();
        long acreditados = lista.stream().filter(Pago::isAcreditado).count();
        long pendientes = lista.stream().filter(p -> p.getEstado() == EstadoPago.PENDIENTE).count();
        long deudores = estudiantes.stream().filter(e -> pagos.saldoPendiente(e).signum() > 0).count();
        List<Estudiante> becados = estudiantes.stream().filter(e -> e.isActivo() && e.tieneBeca()).toList();
        double promedioBeca = becados.stream().mapToDouble(Estudiante::getPorcentajeBeca).average().orElse(0);

        Animaciones.contador(lblRecaudado, recaudado.doubleValue(), Formatos::monedaCorta);
        Animaciones.contador(lblPendiente, pendiente.doubleValue(), Formatos::monedaCorta);
        Animaciones.contador(lblSaldo, saldo.doubleValue(), Formatos::monedaCorta);
        Animaciones.contador(lblBecas, becados.size(), v -> String.valueOf(Math.round(v)));
        lblRecaudadoNota.setText(acreditados + " pagos acreditados");
        lblPendienteNota.setText(pendientes + (pendientes == 1 ? " transferencia en espera" : " transferencias en espera"));
        lblSaldoNota.setText(deudores + " de " + estudiantes.stream().filter(Estudiante::isActivo).count()
                + " estudiantes activos deben");
        lblBecasNota.setText(becados.isEmpty() ? "sin becas vigentes"
                : "promedio " + Formatos.porcentaje(Math.round(promedioBeca)));
        Animaciones.cascada(cajaRecaudado, cajaPendiente, cajaSaldo, cajaBecas);

        cargarGrafico();
        cargarDeudores(estudiantes);
    }

    private void cargarGrafico() {
        Map<String, BigDecimal> datos = new LinkedHashMap<>();
        Map<String, String> colores = new LinkedHashMap<>();
        if (tbPorMedio.isSelected()) {
            pagos.distribucionPorMedio().forEach((medio, total) -> {
                datos.put(medio.toString(), total);
                colores.put(medio.toString(), COLOR_MEDIO.get(medio));
            });
        } else {
            pagos.distribucionPorTipoConcepto().forEach((tipo, total) -> {
                datos.put(tipo.toString(), total);
                colores.put(tipo.toString(), COLOR_TIPO.get(tipo));
            });
        }
        BigDecimal total = datos.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        ObservableList<PieChart.Data> porciones = FXCollections.observableArrayList();
        datos.forEach((nombre, valor) -> porciones.add(new PieChart.Data(nombre, valor.doubleValue())));
        grafico.setData(porciones);
        leyenda.getChildren().clear();

        for (PieChart.Data d : porciones) {
            String color = colores.get(d.getName());
            double pct = total.signum() == 0 ? 0 : d.getPieValue() * 100 / total.doubleValue();
            d.getNode().setStyle("-fx-pie-color: " + color + ";");
            Tooltip.install(d.getNode(), new Tooltip(d.getName() + "  " + Formatos.moneda(BigDecimal.valueOf(d.getPieValue()))
                    + "  (" + Math.round(pct) + " %)"));
            d.getNode().setOnMouseEntered(e -> escalar(d, 1.05));
            d.getNode().setOnMouseExited(e -> escalar(d, 1.0));
            leyenda.getChildren().add(itemLeyenda(d.getName(), color, d.getPieValue(), pct));
        }
        if (porciones.isEmpty()) {
            Label vacio = new Label("Todavía no hay pagos acreditados.");
            vacio.getStyleClass().add("texto-apagado");
            leyenda.getChildren().add(vacio);
        }
        Animaciones.cascada(leyenda.getChildren().toArray(javafx.scene.Node[]::new));
    }

    private static void escalar(PieChart.Data d, double factor) {
        ScaleTransition s = new ScaleTransition(Duration.millis(120), d.getNode());
        s.setToX(factor);
        s.setToY(factor);
        s.play();
    }

    private static VBox itemLeyenda(String nombre, String color, double valor, double pct) {
        Region punto = new Region();
        punto.getStyleClass().add("marcador");
        punto.setStyle("-fx-background-color: " + color + ";");
        Label lNombre = new Label(nombre);
        lNombre.getStyleClass().add("cifra-etiqueta");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label lPct = new Label(Math.round(pct) + " %");
        lPct.getStyleClass().add("deudor-monto");
        HBox fila = new HBox(8, punto, lNombre, espacio, lPct);
        fila.setAlignment(Pos.CENTER_LEFT);
        Label lValor = new Label(Formatos.moneda(BigDecimal.valueOf(valor)));
        lValor.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        return new VBox(2, fila, lValor);
    }

    private void cargarDeudores(List<Estudiante> estudiantes) {
        listaDeudores.getChildren().clear();
        record Deuda(Estudiante e, BigDecimal saldo) {
        }
        List<Deuda> top = estudiantes.stream()
                .map(e -> new Deuda(e, pagos.saldoPendiente(e)))
                .filter(d -> d.saldo().signum() > 0)
                .sorted(Comparator.comparing(Deuda::saldo).reversed())
                .limit(6)
                .toList();
        if (top.isEmpty()) {
            Label nadie = new Label("Nadie adeuda conceptos.");
            nadie.getStyleClass().add("texto-apagado");
            listaDeudores.getChildren().add(nadie);
            return;
        }
        double maximo = top.get(0).saldo().doubleValue();
        int i = 0;
        for (Deuda d : top) {
            Label nombre = new Label(d.e().getNombreCompleto());
            nombre.getStyleClass().add("deudor-nombre");
            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);
            Label monto = new Label(Formatos.moneda(d.saldo()));
            monto.getStyleClass().add("deudor-monto");
            HBox fila = new HBox(nombre, espacio, monto);

            Region fondo = new Region();
            fondo.getStyleClass().add("barra-fondo");
            fondo.setMaxWidth(Double.MAX_VALUE);
            Region valor = new Region();
            valor.getStyleClass().add("barra-valor");
            valor.setMaxWidth(Region.USE_PREF_SIZE);
            StackPane barra = new StackPane(fondo, valor);
            barra.setAlignment(Pos.CENTER_LEFT);

            listaDeudores.getChildren().add(new VBox(6, fila, barra));
            Animaciones.crecerAncho(valor, ANCHO_BARRA * d.saldo().doubleValue() / maximo, 120 + 60 * i++);
        }
    }
}
