package com.ipesmi.pagos.controller;

import com.ipesmi.pagos.exception.MontoInvalidoException;
import com.ipesmi.pagos.exception.PagoDuplicadoException;
import com.ipesmi.pagos.factory.PagoFactory;
import com.ipesmi.pagos.factory.SolicitudPago;
import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.EstadoPago;
import com.ipesmi.pagos.model.Estudiante;
import com.ipesmi.pagos.model.MedioPago;
import com.ipesmi.pagos.model.Pago;
import com.ipesmi.pagos.model.PagoTarjeta;
import com.ipesmi.pagos.model.PagoTransferencia;
import com.ipesmi.pagos.service.AppContext;
import com.ipesmi.pagos.service.PagoService;
import com.ipesmi.pagos.util.Formatos;
import com.ipesmi.pagos.view.Animaciones;
import com.ipesmi.pagos.view.Celdas;
import com.ipesmi.pagos.view.ComboBuscable;
import com.ipesmi.pagos.view.Dialogos;
import com.ipesmi.pagos.view.Notificador;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Vista de pagos: TableView filtrable y formulario de alta / edición. */
public class PagosController implements Refrescable {

    private static final String TODOS_ESTADOS = "Todos los estados";
    private static final String TODOS_MEDIOS = "Todos los medios";

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbFiltroEstado;
    @FXML private ComboBox<String> cmbFiltroMedio;
    @FXML private TableView<Pago> tabla;
    @FXML private TableColumn<Pago, Long> colId;
    @FXML private TableColumn<Pago, LocalDate> colFecha;
    @FXML private TableColumn<Pago, String> colEstudiante;
    @FXML private TableColumn<Pago, String> colConcepto;
    @FXML private TableColumn<Pago, Pago> colMedio;
    @FXML private TableColumn<Pago, BigDecimal> colMonto;
    @FXML private TableColumn<Pago, EstadoPago> colEstado;
    @FXML private Label lblResumenTabla;
    @FXML private Button btnAcreditar;
    @FXML private Button btnEditar;
    @FXML private Button btnAnular;
    @FXML private Button btnEliminar;

    @FXML private VBox formulario;
    @FXML private Label lblFormTitulo;
    @FXML private ComboBox<Estudiante> cmbEstudiante;
    @FXML private ComboBox<Concepto> cmbConcepto;
    @FXML private Label lblImporte;
    @FXML private ToggleGroup grupoMedio;
    @FXML private ToggleButton tbEfectivo;
    @FXML private ToggleButton tbTarjeta;
    @FXML private ToggleButton tbTransferencia;
    @FXML private TextField txtMonto;
    @FXML private DatePicker dpFecha;
    @FXML private Label lblReferencia;
    @FXML private TextField txtReferencia;
    @FXML private VBox boxCuotas;
    @FXML private Spinner<Integer> spCuotas;
    @FXML private VBox boxBanco;
    @FXML private TextField txtBanco;
    @FXML private Button btnRegistrar;

    private final PagoService servicio = AppContext.get().pagos();
    private final ObservableList<Pago> datos = FXCollections.observableArrayList();
    private FilteredList<Pago> filtrados;
    private ComboBuscable<Estudiante> buscadorEstudiante;

    /** Pago que se está editando, o null si el formulario es de alta. */
    private Pago enEdicion;

    @FXML
    private void initialize() {
        configurarTabla();
        configurarFiltros();
        configurarFormulario();
        limpiar();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        colId.setCellFactory(Celdas.texto(id -> String.valueOf(id), "celda-codigo"));
        colFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getFecha()));
        colFecha.setCellFactory(Celdas.texto(Formatos::fecha, null));
        colEstudiante.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getEstudiante().getNombreCompleto()));
        colConcepto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getConcepto().getDescripcion()));
        colMedio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colMedio.setCellFactory(Celdas.medioPago());
        colMedio.setComparator((a, b) -> a.getMedio().compareTo(b.getMedio()));
        colMonto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMonto()));
        colMonto.setCellFactory(Celdas.texto(Formatos::moneda, "celda-monto"));
        colEstado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getEstado()));
        colEstado.setCellFactory(Celdas.pill(e -> e.toString(), e -> "pill-" + e.name().toLowerCase(Locale.ROOT)));

        filtrados = new FilteredList<>(datos, p -> true);
        SortedList<Pago> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenados);
        tabla.setPlaceholder(new Label("No hay pagos que coincidan con el filtro."));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> actualizarBotones());
        tabla.setRowFactory(tv -> {
            var fila = new javafx.scene.control.TableRow<Pago>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    editar();
                }
            });
            return fila;
        });
    }

    private void configurarFiltros() {
        cmbFiltroEstado.getItems().add(TODOS_ESTADOS);
        for (EstadoPago e : EstadoPago.values()) {
            cmbFiltroEstado.getItems().add(e.toString());
        }
        cmbFiltroMedio.getItems().add(TODOS_MEDIOS);
        for (MedioPago m : MedioPago.values()) {
            cmbFiltroMedio.getItems().add(m.toString());
        }
        cmbFiltroEstado.getSelectionModel().selectFirst();
        cmbFiltroMedio.getSelectionModel().selectFirst();
        txtBuscar.textProperty().addListener((o, a, b) -> aplicarFiltro());
        cmbFiltroEstado.valueProperty().addListener((o, a, b) -> aplicarFiltro());
        cmbFiltroMedio.valueProperty().addListener((o, a, b) -> aplicarFiltro());
    }

    private void aplicarFiltro() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().strip().toLowerCase(Locale.ROOT);
        String estado = cmbFiltroEstado.getValue();
        String medio = cmbFiltroMedio.getValue();
        filtrados.setPredicate(p -> {
            if (estado != null && !TODOS_ESTADOS.equals(estado) && !p.getEstado().toString().equals(estado)) {
                return false;
            }
            if (medio != null && !TODOS_MEDIOS.equals(medio) && !p.getMedio().toString().equals(medio)) {
                return false;
            }
            if (texto.isEmpty()) {
                return true;
            }
            String bolsa = (p.getEstudiante().getNombreCompleto() + " " + p.getEstudiante().getLegajo() + " "
                    + p.getConcepto().getDescripcion() + " " + p.getConcepto().getCodigo() + " "
                    + p.getReferencia()).toLowerCase(Locale.ROOT);
            return bolsa.contains(texto);
        });
        actualizarResumenTabla();
    }

    private void actualizarResumenTabla() {
        BigDecimal acreditado = filtrados.stream().filter(Pago::isAcreditado)
                .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblResumenTabla.setText(filtrados.size() + (filtrados.size() == 1 ? " pago" : " pagos")
                + "  ·  " + Formatos.moneda(acreditado) + " acreditados");
    }

    private void configurarFormulario() {
        tbEfectivo.setUserData(MedioPago.EFECTIVO);
        tbTarjeta.setUserData(MedioPago.TARJETA);
        tbTransferencia.setUserData(MedioPago.TRANSFERENCIA);
        grupoMedio.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);
            } else {
                ajustarCamposMedio();
            }
        });
        spCuotas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, PagoFactory.MAX_CUOTAS, 1));
        dpFecha.setConverter(Celdas.convertidorFecha());
        // listas largas: se busca escribiendo legajo o nombre ("1001", "benitez", "lucia ben")
        buscadorEstudiante = new ComboBuscable<>(cmbEstudiante,
                e -> e.getLegajo() + " · " + e.getNombreCompleto(),
                e -> e.getLegajo() + " " + e.getApellido() + " " + e.getNombre());
        buscadorEstudiante.seleccionadoProperty().addListener((o, a, b) -> actualizarImporte(true));
        cmbConcepto.valueProperty().addListener((o, a, b) -> actualizarImporte(true));
    }

    private MedioPago medioSeleccionado() {
        return (MedioPago) grupoMedio.getSelectedToggle().getUserData();
    }

    private void ajustarCamposMedio() {
        MedioPago medio = medioSeleccionado();
        boolean tarjeta = medio == MedioPago.TARJETA;
        boolean transferencia = medio == MedioPago.TRANSFERENCIA;
        mostrar(boxCuotas, tarjeta);
        mostrar(boxBanco, transferencia);
        switch (medio) {
            case EFECTIVO -> {
                lblReferencia.setText("NRO. DE RECIBO (OPCIONAL)");
                txtReferencia.setPromptText("Se genera automáticamente");
            }
            case TARJETA -> {
                lblReferencia.setText("ÚLTIMOS 4 DÍGITOS");
                txtReferencia.setPromptText("1234");
            }
            case TRANSFERENCIA -> {
                lblReferencia.setText("NRO. DE OPERACIÓN");
                txtReferencia.setPromptText("OP-000000");
            }
        }
    }

    private static void mostrar(VBox caja, boolean visible) {
        caja.setVisible(visible);
        caja.setManaged(visible);
        if (visible) {
            Animaciones.entrada(caja);
        }
    }

    /** Muestra importe, beca y saldo disponible; opcionalmente completa el monto. */
    private void actualizarImporte(boolean completarMonto) {
        Estudiante e = buscadorEstudiante.getSeleccionado();
        Concepto c = cmbConcepto.getValue();
        if (e == null || c == null || enEdicion != null) {
            if (enEdicion == null) {
                lblImporte.setText("Elija estudiante y concepto para ver el saldo.");
            }
            return;
        }
        BigDecimal importe = servicio.importeConBeca(e, c);
        BigDecimal disponible = servicio.disponibleParaPagar(e, c);
        StringBuilder sb = new StringBuilder("Importe ").append(Formatos.moneda(c.getMonto()));
        if (c.isBecable() && e.tieneBeca()) {
            sb.append("  →  con beca ").append(Formatos.porcentaje(e.getPorcentajeBeca()))
                    .append(": ").append(Formatos.moneda(importe));
        } else if (!c.isBecable() && e.tieneBeca()) {
            sb.append("  (no becable)");
        }
        sb.append("\nDisponible para pagar: ").append(Formatos.moneda(disponible));
        lblImporte.setText(sb.toString());
        if (completarMonto) {
            txtMonto.setText(disponible.signum() > 0 ? disponible.toPlainString().replace('.', ',') : "");
        }
    }

    // ------------------------------------------------------------ acciones

    @FXML
    private void guardar() {
        if (enEdicion != null) {
            guardarEdicion();
            return;
        }
        try {
            // un monto vacío llega como null y lo rechaza el servicio (después de verificar duplicados)
            BigDecimal monto = txtMonto.getText().isBlank() ? null : Formatos.leerMonto(txtMonto.getText());
            SolicitudPago solicitud = new SolicitudPago(buscadorEstudiante.getSeleccionado(), cmbConcepto.getValue(), monto,
                    dpFecha.getValue(), txtReferencia.getText(), spCuotas.getValue(), txtBanco.getText());
            Pago nuevo = servicio.registrarPago(medioSeleccionado(), solicitud);
            Notificador.exito("Pago #" + nuevo.getId() + " registrado · " + nuevo.getDetalleMedio()
                    + (nuevo.getEstado() == EstadoPago.PENDIENTE ? " (pendiente de acreditación)" : ""));
            refrescar();
            tabla.getSelectionModel().select(nuevo);
            tabla.scrollTo(nuevo);
            limpiar();
        } catch (MontoInvalidoException ex) {
            Notificador.error("Monto inválido", ex.getMessage());
            Animaciones.sacudir(txtMonto);
        } catch (PagoDuplicadoException ex) {
            Notificador.error("Pago duplicado", ex.getMessage());
            Animaciones.sacudir(formulario);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Notificador.error("Revise los datos", ex.getMessage());
            Animaciones.sacudir(formulario);
        }
    }

    private void guardarEdicion() {
        try {
            BigDecimal monto = Formatos.leerMonto(txtMonto.getText());
            servicio.actualizarPago(enEdicion, monto, dpFecha.getValue(), txtReferencia.getText());
            if (enEdicion instanceof PagoTarjeta t) {
                t.setCuotas(spCuotas.getValue());
            } else if (enEdicion instanceof PagoTransferencia t) {
                t.setBanco(txtBanco.getText());
            }
            Notificador.exito("Pago #" + enEdicion.getId() + " actualizado");
            Pago editado = enEdicion;
            limpiar();
            refrescar();
            tabla.getSelectionModel().select(editado);
        } catch (MontoInvalidoException ex) {
            Notificador.error("Monto inválido", ex.getMessage());
            Animaciones.sacudir(txtMonto);
        } catch (PagoDuplicadoException ex) {
            Notificador.error("Referencia duplicada", ex.getMessage());
            Animaciones.sacudir(txtReferencia);
        } catch (IllegalStateException ex) {
            Notificador.error("No se puede modificar", ex.getMessage());
        }
    }

    @FXML
    private void editar() {
        Pago p = tabla.getSelectionModel().getSelectedItem();
        if (p == null || p.isAnulado()) {
            return;
        }
        enEdicion = p;
        lblFormTitulo.setText("Editar pago #" + p.getId());
        lblFormTitulo.getStyleClass().removeAll("editando");
        lblFormTitulo.getStyleClass().add("editando");
        btnRegistrar.setText("Guardar cambios");
        cmbEstudiante.setValue(p.getEstudiante());
        cmbConcepto.setValue(p.getConcepto());
        grupoMedio.selectToggle(switch (p.getMedio()) {
            case EFECTIVO -> tbEfectivo;
            case TARJETA -> tbTarjeta;
            case TRANSFERENCIA -> tbTransferencia;
        });
        cmbEstudiante.setDisable(true);
        cmbConcepto.setDisable(true);
        tbEfectivo.setDisable(true);
        tbTarjeta.setDisable(true);
        tbTransferencia.setDisable(true);
        txtMonto.setText(p.getMonto().toPlainString().replace('.', ','));
        dpFecha.setValue(p.getFecha());
        txtReferencia.setText(p.getReferencia());
        if (p instanceof PagoTarjeta t && t.getCuotas() != null) {
            spCuotas.getValueFactory().setValue(t.getCuotas());
        }
        if (p instanceof PagoTransferencia t) {
            txtBanco.setText(t.getBanco());
        }
        lblImporte.setText("Saldo del concepto: " + Formatos.moneda(servicio.saldoConcepto(p.getEstudiante(), p.getConcepto()))
                + "\nEl estudiante, el concepto y el medio no se modifican.");
        Animaciones.pulso(lblFormTitulo);
    }

    @FXML
    private void acreditar() {
        Pago p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            return;
        }
        try {
            servicio.acreditar(p);
            Notificador.exito("Pago #" + p.getId() + " acreditado");
            refrescar();
            tabla.getSelectionModel().select(p);
        } catch (IllegalStateException ex) {
            Notificador.error("No se puede acreditar", ex.getMessage());
        }
    }

    @FXML
    private void anular() {
        Pago p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            return;
        }
        boolean ok = Dialogos.confirmar("Anular pago #" + p.getId(),
                p.getEstudiante().getNombreCompleto() + " · " + p.getConcepto().getDescripcion() + " · "
                        + Formatos.moneda(p.getMonto()) + "\nEl monto vuelve a figurar como saldo pendiente.",
                "Anular");
        if (!ok) {
            return;
        }
        try {
            servicio.anular(p);
            Notificador.info("Pago #" + p.getId() + " anulado");
            refrescar();
        } catch (IllegalStateException ex) {
            Notificador.error("No se puede anular", ex.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        Pago p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            return;
        }
        boolean ok = Dialogos.confirmar("Eliminar pago #" + p.getId(),
                "Se borra definitivamente de la base de datos. Para conservar el historial use Anular.",
                "Eliminar");
        if (ok) {
            servicio.eliminar(p);
            Notificador.info("Pago #" + p.getId() + " eliminado");
            if (p == enEdicion) {
                limpiar();
            }
            refrescar();
        }
    }

    @FXML
    private void limpiar() {
        enEdicion = null;
        lblFormTitulo.setText("Nuevo pago");
        lblFormTitulo.getStyleClass().removeAll("editando");
        btnRegistrar.setText("Registrar pago");
        cmbEstudiante.setDisable(false);
        cmbConcepto.setDisable(false);
        tbEfectivo.setDisable(false);
        tbTarjeta.setDisable(false);
        tbTransferencia.setDisable(false);
        buscadorEstudiante.limpiar();
        cmbConcepto.setValue(null);
        tbEfectivo.setSelected(true);
        ajustarCamposMedio();
        txtMonto.clear();
        txtReferencia.clear();
        txtBanco.clear();
        spCuotas.getValueFactory().setValue(1);
        dpFecha.setValue(LocalDate.now());
        lblImporte.setText("Elija estudiante y concepto para ver el saldo.");
    }

    private void actualizarBotones() {
        Pago p = tabla.getSelectionModel().getSelectedItem();
        btnAcreditar.setDisable(p == null || p.getEstado() != EstadoPago.PENDIENTE);
        btnEditar.setDisable(p == null || p.isAnulado());
        btnAnular.setDisable(p == null || p.isAnulado());
        btnEliminar.setDisable(p == null);
    }

    @Override
    public void refrescar() {
        AppContext ctx = AppContext.get();
        Estudiante e = buscadorEstudiante.getSeleccionado();
        Concepto c = cmbConcepto.getValue();
        List<Estudiante> activos = ctx.estudiantes().listarActivos();
        buscadorEstudiante.setDatos(activos);
        cmbConcepto.getItems().setAll(ctx.conceptos().listar());
        if (e != null && activos.contains(e)) {
            cmbEstudiante.setValue(e);
        }
        if (c != null && cmbConcepto.getItems().contains(c)) {
            cmbConcepto.setValue(c);
        }
        datos.setAll(servicio.listar());
        tabla.refresh();
        aplicarFiltro();
        actualizarBotones();
    }
}
