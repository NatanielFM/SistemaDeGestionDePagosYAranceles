package com.ipesmi.pagos.controller;

import com.ipesmi.pagos.exception.MontoInvalidoException;
import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.Pago;
import com.ipesmi.pagos.model.TipoConcepto;
import com.ipesmi.pagos.service.AppContext;
import com.ipesmi.pagos.service.ConceptoService;
import com.ipesmi.pagos.util.Formatos;
import com.ipesmi.pagos.view.Animaciones;
import com.ipesmi.pagos.view.Celdas;
import com.ipesmi.pagos.view.Dialogos;
import com.ipesmi.pagos.view.Notificador;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** CRUD de conceptos de pago. */
public class ConceptosController implements Refrescable {

    @FXML private TableView<Concepto> tabla;
    @FXML private TableColumn<Concepto, String> colCodigo;
    @FXML private TableColumn<Concepto, String> colDescripcion;
    @FXML private TableColumn<Concepto, TipoConcepto> colTipo;
    @FXML private TableColumn<Concepto, BigDecimal> colMonto;
    @FXML private TableColumn<Concepto, Boolean> colBecable;
    @FXML private TableColumn<Concepto, LocalDate> colVencimiento;
    @FXML private TableColumn<Concepto, BigDecimal> colRecaudado;
    @FXML private Label lblResumen;
    @FXML private Button btnEliminar;

    @FXML private VBox formulario;
    @FXML private Label lblFormTitulo;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtDescripcion;
    @FXML private ComboBox<TipoConcepto> cmbTipo;
    @FXML private TextField txtMonto;
    @FXML private DatePicker dpVencimiento;
    @FXML private CheckBox chkBecable;
    @FXML private Button btnGuardar;

    private final ConceptoService servicio = AppContext.get().conceptos();
    private final Map<Concepto, BigDecimal> recaudado = new HashMap<>();

    @FXML
    private void initialize() {
        colCodigo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getCodigo()));
        colCodigo.setCellFactory(Celdas.texto(s -> s, "celda-codigo"));
        colDescripcion.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDescripcion()));
        colTipo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getTipo()));
        colMonto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMonto()));
        colMonto.setCellFactory(Celdas.texto(Formatos::moneda, "celda-monto"));
        colBecable.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().isBecable()));
        colBecable.setCellFactory(Celdas.pill(b -> b ? "Sí" : "No", b -> b ? "pill-beca" : "pill-inactivo"));
        colVencimiento.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getVencimiento()));
        colVencimiento.setCellFactory(Celdas.texto(Formatos::fecha, null));
        colRecaudado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                recaudado.getOrDefault(c.getValue(), BigDecimal.ZERO)));
        colRecaudado.setCellFactory(Celdas.texto(Formatos::moneda, "celda-monto"));

        tabla.setPlaceholder(new Label("Todavía no hay conceptos cargados."));
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, a, c) -> {
            btnEliminar.setDisable(c == null);
            if (c != null) {
                cargarEnFormulario(c);
            }
        });
        cmbTipo.getItems().setAll(TipoConcepto.values());
        dpVencimiento.setConverter(Celdas.convertidorFecha());
        nuevo();
    }

    private void cargarEnFormulario(Concepto c) {
        lblFormTitulo.setText("Editar " + c.getCodigo());
        lblFormTitulo.getStyleClass().removeAll("editando");
        lblFormTitulo.getStyleClass().add("editando");
        btnGuardar.setText("Guardar cambios");
        txtCodigo.setText(c.getCodigo());
        txtCodigo.setDisable(true);
        txtDescripcion.setText(c.getDescripcion());
        cmbTipo.setValue(c.getTipo());
        txtMonto.setText(c.getMonto().toPlainString().replace('.', ','));
        dpVencimiento.setValue(c.getVencimiento());
        chkBecable.setSelected(c.isBecable());
    }

    @FXML
    private void guardar() {
        boolean edicion = txtCodigo.isDisabled();
        try {
            BigDecimal monto = Formatos.leerMonto(txtMonto.getText());
            Concepto c = edicion
                    ? servicio.actualizar(txtCodigo.getText(), txtDescripcion.getText(), monto, cmbTipo.getValue(),
                    chkBecable.isSelected(), dpVencimiento.getValue())
                    : servicio.registrar(txtCodigo.getText(), txtDescripcion.getText(), monto, cmbTipo.getValue(),
                    chkBecable.isSelected(), dpVencimiento.getValue());
            Notificador.exito("Concepto " + c.getCodigo() + (edicion ? " actualizado" : " creado"));
            refrescar();
            tabla.getSelectionModel().select(c);
        } catch (MontoInvalidoException ex) {
            Notificador.error("Monto inválido", ex.getMessage());
            Animaciones.sacudir(txtMonto);
        } catch (IllegalArgumentException ex) {
            Notificador.error("Revise los datos", ex.getMessage());
            Animaciones.sacudir(formulario);
        }
    }

    @FXML
    private void eliminar() {
        Concepto c = tabla.getSelectionModel().getSelectedItem();
        if (c == null || !Dialogos.confirmar("Eliminar " + c.getCodigo(),
                c.getDescripcion() + " dejará de figurar como deuda para todos los estudiantes.", "Eliminar")) {
            return;
        }
        try {
            servicio.eliminar(c.getCodigo());
            Notificador.info("Concepto " + c.getCodigo() + " eliminado");
            nuevo();
            refrescar();
        } catch (IllegalStateException ex) {
            Notificador.error("No se puede eliminar", ex.getMessage());
        }
    }

    @FXML
    private void nuevo() {
        tabla.getSelectionModel().clearSelection();
        lblFormTitulo.setText("Nuevo concepto");
        lblFormTitulo.getStyleClass().removeAll("editando");
        btnGuardar.setText("Guardar concepto");
        txtCodigo.setDisable(false);
        txtCodigo.clear();
        txtDescripcion.clear();
        txtMonto.clear();
        cmbTipo.setValue(TipoConcepto.CUOTA);
        dpVencimiento.setValue(null);
        chkBecable.setSelected(true);
        btnEliminar.setDisable(true);
        txtCodigo.requestFocus();
    }

    @Override
    public void refrescar() {
        recaudado.clear();
        for (Pago p : AppContext.get().pagos().listar()) {
            if (p.isAcreditado()) {
                recaudado.merge(p.getConcepto(), p.getMonto(), BigDecimal::add);
            }
        }
        tabla.getItems().setAll(servicio.listar());
        tabla.refresh();
        BigDecimal total = servicio.listar().stream().map(Concepto::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblResumen.setText(servicio.listar().size() + " conceptos  ·  " + Formatos.moneda(total)
                + " por estudiante sin beca");
    }
}
