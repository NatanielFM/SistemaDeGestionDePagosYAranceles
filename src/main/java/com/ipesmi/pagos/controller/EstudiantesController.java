package com.ipesmi.pagos.controller;

import com.ipesmi.pagos.exception.BecaNoAplicableException;
import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.Estudiante;
import com.ipesmi.pagos.model.Pago;
import com.ipesmi.pagos.service.AppContext;
import com.ipesmi.pagos.service.EstudianteService;
import com.ipesmi.pagos.service.PagoService;
import com.ipesmi.pagos.util.Formatos;
import com.ipesmi.pagos.view.Animaciones;
import com.ipesmi.pagos.view.Celdas;
import com.ipesmi.pagos.view.Dialogos;
import com.ipesmi.pagos.view.Notificador;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** CRUD de estudiantes, aplicación de becas y TreeView con la cuenta corriente. */
public class EstudiantesController implements Refrescable {

    /** Nodo del árbol: texto a la izquierda, detalle a la derecha y un estilo. */
    private record Nodo(String texto, String detalle, String estilo, String estiloDetalle) {
    }

    @FXML private TextField txtBuscar;
    @FXML private TableView<Estudiante> tabla;
    @FXML private TableColumn<Estudiante, Integer> colLegajo;
    @FXML private TableColumn<Estudiante, String> colNombre;
    @FXML private TableColumn<Estudiante, String> colEmail;
    @FXML private TableColumn<Estudiante, Double> colBeca;
    @FXML private TableColumn<Estudiante, BigDecimal> colSaldo;
    @FXML private TableColumn<Estudiante, Boolean> colEstado;
    @FXML private TreeView<Nodo> arbolCuenta;
    @FXML private Label lblCuenta;

    @FXML private VBox formulario;
    @FXML private Label lblFormTitulo;
    @FXML private TextField txtLegajo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtEmail;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar;
    @FXML private Button btnEliminar;

    @FXML private VBox panelBeca;
    @FXML private Label lblBecaActual;
    @FXML private TextField txtBeca;
    @FXML private Button btnAplicarBeca;
    @FXML private Button btnQuitarBeca;

    private final EstudianteService servicio = AppContext.get().estudiantes();
    private final PagoService pagos = AppContext.get().pagos();
    private final ObservableList<Estudiante> datos = FXCollections.observableArrayList();
    private FilteredList<Estudiante> filtrados;

    @FXML
    private void initialize() {
        colLegajo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getLegajo()));
        colLegajo.setCellFactory(Celdas.texto(String::valueOf, "celda-codigo"));
        colNombre.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getNombreCompleto()));
        colEmail.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getEmail()));
        colBeca.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getPorcentajeBeca()));
        colBeca.setCellFactory(Celdas.pill(b -> b > 0 ? Formatos.porcentaje(b) : null, b -> "pill-beca"));
        colSaldo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(pagos.saldoPendiente(c.getValue())));
        colSaldo.setCellFactory(Celdas.texto(Formatos::moneda, "celda-monto"));
        colEstado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().isActivo()));
        colEstado.setCellFactory(Celdas.pill(a -> a ? "Activo" : "Inactivo", a -> a ? "pill-activo" : "pill-inactivo"));

        filtrados = new FilteredList<>(datos, e -> true);
        SortedList<Estudiante> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenados);
        tabla.setPlaceholder(new Label("No hay estudiantes."));
        txtBuscar.textProperty().addListener((o, a, texto) -> {
            String t = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
            filtrados.setPredicate(e -> t.isEmpty()
                    || (e.getLegajo() + " " + e.getNombreCompleto() + " " + e.getEmail()).toLowerCase(Locale.ROOT).contains(t));
        });

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, a, e) -> seleccionar(e));
        configurarArbol();
        nuevo();
    }

    private void configurarArbol() {
        arbolCuenta.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Nodo nodo, boolean vacio) {
                super.updateItem(nodo, vacio);
                setText(null);
                if (vacio || nodo == null) {
                    setGraphic(null);
                    return;
                }
                Label texto = new Label(nodo.texto());
                if (nodo.estilo() != null) {
                    texto.getStyleClass().add(nodo.estilo());
                }
                Region espacio = new Region();
                HBox.setHgrow(espacio, Priority.ALWAYS);
                Label detalle = new Label(nodo.detalle());
                detalle.getStyleClass().add(nodo.estiloDetalle() != null ? nodo.estiloDetalle() : "nodo-detalle");
                HBox fila = new HBox(12, texto, espacio, detalle);
                fila.setAlignment(Pos.CENTER_LEFT);
                fila.setMaxWidth(Double.MAX_VALUE);
                fila.prefWidthProperty().bind(tv.widthProperty().subtract(86 +tv.getTreeItemLevel(getTreeItem()) * 18));
                setGraphic(fila);
            }
        });
        arbolCuenta.setRoot(new TreeItem<>(new Nodo("Seleccione un estudiante para ver su cuenta", "", "texto-apagado", null)));
    }

    /** Arma el árbol: estudiante → conceptos → pagos de cada concepto. */
    private void cargarCuenta(Estudiante e) {
        if (e == null) {
            arbolCuenta.setRoot(new TreeItem<>(new Nodo("Seleccione un estudiante para ver su cuenta", "", "texto-apagado", null)));
            lblCuenta.setText("");
            return;
        }
        BigDecimal saldoTotal = pagos.saldoPendiente(e);
        TreeItem<Nodo> raiz = new TreeItem<>(new Nodo(e.getNombreCompleto() + "  ·  legajo " + e.getLegajo(),
                saldoTotal.signum() > 0 ? "debe " + Formatos.moneda(saldoTotal) : "al día",
                "nodo-raiz", saldoTotal.signum() > 0 ? "nodo-saldo" : "nodo-ok"));
        raiz.setExpanded(true);

        List<Pago> pagosEstudiante = pagos.pagosDe(e);
        for (Concepto c : AppContext.get().conceptos().listar()) {
            BigDecimal saldo = pagos.saldoConcepto(e, c);
            String importe = Formatos.moneda(pagos.importeConBeca(e, c));
            String det = e.isActivo()
                    ? (saldo.signum() > 0 ? "saldo " + Formatos.moneda(saldo) + " de " + importe : "pagado · " + importe)
                    : "inactivo";
            TreeItem<Nodo> nodoConcepto = new TreeItem<>(new Nodo(c.getCodigo() + "   " + c.getDescripcion(), det,
                    "nodo-concepto", saldo.signum() > 0 && e.isActivo() ? "nodo-saldo" : "nodo-ok"));
            for (Pago p : pagosEstudiante) {
                if (p.getConcepto().equals(c)) {
                    nodoConcepto.getChildren().add(new TreeItem<>(new Nodo(
                            Formatos.fecha(p.getFecha()) + "   " + p.getDetalleMedio(),
                            Formatos.moneda(p.getMonto()) + "  " + p.getEstado().toString().toLowerCase(Locale.ROOT),
                            null, null)));
                }
            }
            nodoConcepto.setExpanded(!nodoConcepto.getChildren().isEmpty());
            raiz.getChildren().add(nodoConcepto);
        }
        arbolCuenta.setRoot(raiz);
        lblCuenta.setText(pagosEstudiante.size() + " pagos registrados");
        Animaciones.entrada(arbolCuenta);
    }

    private void seleccionar(Estudiante e) {
        cargarCuenta(e);
        boolean hay = e != null;
        btnEliminar.setDisable(!hay);
        btnAplicarBeca.setDisable(!hay);
        btnQuitarBeca.setDisable(!hay || !e.tieneBeca());
        txtBeca.setDisable(!hay);
        if (!hay) {
            lblBecaActual.setText("Seleccione un estudiante de la tabla.");
            return;
        }
        lblFormTitulo.setText("Editar legajo " + e.getLegajo());
        lblFormTitulo.getStyleClass().removeAll("editando");
        lblFormTitulo.getStyleClass().add("editando");
        txtLegajo.setText(String.valueOf(e.getLegajo()));
        txtLegajo.setDisable(true);
        txtNombre.setText(e.getNombre());
        txtApellido.setText(e.getApellido());
        txtEmail.setText(e.getEmail());
        chkActivo.setSelected(e.isActivo());
        btnGuardar.setText("Guardar cambios");
        lblBecaActual.setText(e.tieneBeca()
                ? e.getNombre() + " tiene una beca del " + Formatos.porcentaje(e.getPorcentajeBeca()) + "."
                : e.getNombre() + " no tiene beca.");
        txtBeca.setText(e.tieneBeca() ? String.valueOf(Math.round(e.getPorcentajeBeca())) : "");
    }

    @FXML
    private void guardar() {
        boolean edicion = txtLegajo.isDisabled();
        try {
            int legajo = leerLegajo();
            Estudiante e = edicion
                    ? servicio.actualizar(legajo, txtNombre.getText(), txtApellido.getText(), txtEmail.getText(),
                    chkActivo.isSelected())
                    : servicio.registrar(nuevoEstudiante(legajo));
            Notificador.exito((edicion ? "Datos de " : "Alta de ") + e.getNombreCompleto() + " guardados");
            refrescar();
            tabla.getSelectionModel().select(e);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Notificador.error("Revise los datos", ex.getMessage());
            Animaciones.sacudir(formulario);
        }
    }

    private Estudiante nuevoEstudiante(int legajo) {
        Estudiante e = new Estudiante(legajo, txtNombre.getText(), txtApellido.getText(), txtEmail.getText());
        e.setActivo(chkActivo.isSelected());
        return e;
    }

    private int leerLegajo() {
        try {
            return Integer.parseInt(txtLegajo.getText().strip());
        } catch (NumberFormatException ex) {
            Animaciones.sacudir(txtLegajo);
            throw new IllegalArgumentException("El legajo debe ser un número entero");
        }
    }

    @FXML
    private void eliminar() {
        Estudiante e = tabla.getSelectionModel().getSelectedItem();
        if (e == null || !Dialogos.confirmar("Eliminar " + e.getNombreCompleto(),
                "Se elimina el legajo " + e.getLegajo() + " de forma permanente.", "Eliminar")) {
            return;
        }
        try {
            servicio.eliminar(e.getLegajo());
            Notificador.info(e.getNombreCompleto() + " eliminado");
            nuevo();
            refrescar();
        } catch (IllegalStateException ex) {
            Notificador.error("No se puede eliminar", ex.getMessage());
        }
    }

    @FXML
    private void aplicarBeca() {
        Estudiante e = tabla.getSelectionModel().getSelectedItem();
        if (e == null) {
            return;
        }
        try {
            double porcentaje;
            try {
                porcentaje = Double.parseDouble(txtBeca.getText().replace("%", "").replace(',', '.').strip());
            } catch (NumberFormatException ex) {
                throw new BecaNoAplicableException("Ingrese un porcentaje numérico");
            }
            BigDecimal antes = pagos.saldoPendiente(e);
            pagos.aplicarBeca(e.getLegajo(), porcentaje);
            BigDecimal despues = pagos.saldoPendiente(e);
            Notificador.exito("Beca del " + Formatos.porcentaje(porcentaje) + " para " + e.getNombreCompleto()
                    + ". Saldo: " + Formatos.moneda(antes) + " → " + Formatos.moneda(despues));
            refrescar();
            tabla.getSelectionModel().select(e);
        } catch (BecaNoAplicableException ex) {
            Notificador.error("Beca no aplicable", ex.getMessage());
            Animaciones.sacudir(panelBeca);
        }
    }

    @FXML
    private void quitarBeca() {
        Estudiante e = tabla.getSelectionModel().getSelectedItem();
        if (e == null) {
            return;
        }
        pagos.quitarBeca(e.getLegajo());
        Notificador.info("Se quitó la beca de " + e.getNombreCompleto());
        refrescar();
        tabla.getSelectionModel().select(e);
    }

    @FXML
    private void nuevo() {
        tabla.getSelectionModel().clearSelection();
        lblFormTitulo.setText("Nuevo estudiante");
        lblFormTitulo.getStyleClass().removeAll("editando");
        btnGuardar.setText("Guardar");
        txtLegajo.setDisable(false);
        txtLegajo.clear();
        txtNombre.clear();
        txtApellido.clear();
        txtEmail.clear();
        chkActivo.setSelected(true);
        txtBeca.clear();
        seleccionar(null);
    }

    @Override
    public void refrescar() {
        Estudiante seleccionado = tabla.getSelectionModel().getSelectedItem();
        datos.setAll(servicio.listar());
        tabla.refresh();
        if (seleccionado != null && datos.contains(seleccionado)) {
            tabla.getSelectionModel().select(seleccionado);
        }
    }
}
