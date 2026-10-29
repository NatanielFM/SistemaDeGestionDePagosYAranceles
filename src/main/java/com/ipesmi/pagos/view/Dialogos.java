package com.ipesmi.pagos.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import java.util.Objects;

/** Diálogo de confirmación con el mismo estilo que el resto de la aplicación. */
public final class Dialogos {

    private static final String CSS = Objects.requireNonNull(
            Dialogos.class.getResource("/com/ipesmi/pagos/css/estilos.css")).toExternalForm();

    private Dialogos() {
    }

    public static boolean confirmar(String titulo, String mensaje, String textoAceptar) {
        ButtonType aceptar = new ButtonType(textoAceptar, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.NONE, mensaje, aceptar, cancelar);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        DialogPane panel = alerta.getDialogPane();
        panel.getStylesheets().add(CSS);
        panel.getStyleClass().add("dialogo");
        panel.lookupButton(aceptar).getStyleClass().add("btn-peligro");
        panel.lookupButton(cancelar).getStyleClass().add("btn-secundario");
        return alerta.showAndWait().filter(aceptar::equals).isPresent();
    }
}
