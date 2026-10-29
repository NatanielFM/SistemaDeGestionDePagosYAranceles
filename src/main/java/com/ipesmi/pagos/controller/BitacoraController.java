package com.ipesmi.pagos.controller;

import com.ipesmi.pagos.util.LoggerTransacciones;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Muestra el archivo de log del {@link LoggerTransacciones} (últimas líneas de
 * sesiones anteriores + las nuevas en vivo, más recientes arriba).
 */
public class BitacoraController implements Refrescable {

    private static final Pattern LINEA = Pattern.compile("^(\\S+ \\S+) \\[(\\w+)\\s*] (.*)$");
    private static final int MAX_LINEAS = 500;
    private static final String TODOS = "Todos los niveles";

    @FXML private Label lblArchivo;
    @FXML private ComboBox<String> cmbNivel;
    @FXML private ListView<String> lista;
    @FXML private Label lblTotal;

    private final LoggerTransacciones log = LoggerTransacciones.getInstance();
    private final ObservableList<String> lineas = FXCollections.observableArrayList();
    private final FilteredList<String> filtradas = new FilteredList<>(lineas, l -> true);

    @FXML
    private void initialize() {
        lblArchivo.setText("Registro de transacciones · " + log.getArchivo().toAbsolutePath());
        cmbNivel.getItems().add(TODOS);
        for (LoggerTransacciones.Nivel n : LoggerTransacciones.Nivel.values()) {
            cmbNivel.getItems().add(n.name());
        }
        cmbNivel.getSelectionModel().selectFirst();
        cmbNivel.valueProperty().addListener((o, a, nivel) -> {
            filtradas.setPredicate(l -> TODOS.equals(nivel) || l.contains("[" + nivel));
            actualizarTotal();
        });

        lista.setItems(filtradas);
        lista.setPlaceholder(new Label("Sin registros."));
        lista.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String linea, boolean vacio) {
                super.updateItem(linea, vacio);
                setText(null);
                if (vacio || linea == null) {
                    setGraphic(null);
                    return;
                }
                Matcher m = LINEA.matcher(linea);
                if (!m.matches()) {
                    Label l = new Label(linea);
                    l.getStyleClass().add("log-mensaje");
                    setGraphic(l);
                    return;
                }
                Label hora = new Label(m.group(1));
                hora.getStyleClass().add("log-hora");
                Label nivel = new Label(m.group(2));
                nivel.getStyleClass().addAll("log-nivel", "log-" + m.group(2));
                Label mensaje = new Label(m.group(3));
                mensaje.getStyleClass().add("log-mensaje");
                HBox fila = new HBox(14, hora, nivel, mensaje);
                fila.setAlignment(Pos.CENTER_LEFT);
                setGraphic(fila);
            }
        });

        // las líneas nuevas llegan en vivo (desde cualquier hilo)
        log.agregarOyente(linea -> Platform.runLater(() -> {
            lineas.add(0, linea);
            actualizarTotal();
        }));
        cargarArchivo();
    }

    private void cargarArchivo() {
        lineas.clear();
        try {
            if (Files.exists(log.getArchivo())) {
                List<String> todas = Files.readAllLines(log.getArchivo(), StandardCharsets.UTF_8);
                for (int i = todas.size() - 1; i >= Math.max(0, todas.size() - MAX_LINEAS); i--) {
                    lineas.add(todas.get(i));
                }
            }
        } catch (IOException e) {
            lineas.add("No se pudo leer " + log.getArchivo() + ": " + e.getMessage());
        }
        actualizarTotal();
    }

    private void actualizarTotal() {
        lblTotal.setText(filtradas.size() + " de " + lineas.size() + " registros (se muestran las últimas "
                + MAX_LINEAS + " líneas del archivo)");
    }

    @Override
    public void refrescar() {
        lista.refresh();
    }
}
