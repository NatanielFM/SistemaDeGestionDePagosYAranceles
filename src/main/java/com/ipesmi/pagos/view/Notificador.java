package com.ipesmi.pagos.view;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Avisos breves ("toasts") en la esquina inferior derecha. Reemplazan a los
 * diálogos modales para confirmar operaciones o informar errores de negocio.
 */
public final class Notificador {

    public enum Tipo { EXITO, ERROR, INFO }

    private static StackPane capa;
    private static VBox pila;

    private Notificador() {
    }

    public static void instalar(StackPane capaRaiz) {
        capa = capaRaiz;
        pila = new VBox(10);
        pila.setAlignment(Pos.BOTTOM_RIGHT);
        pila.setPickOnBounds(false);
        pila.setMouseTransparent(true);
        pila.setPadding(new Insets(24));
        pila.setMaxSize(StackPane.USE_PREF_SIZE, StackPane.USE_PREF_SIZE);
        StackPane.setAlignment(pila, Pos.BOTTOM_RIGHT);
        capa.getChildren().add(pila);
    }

    public static void exito(String mensaje) { mostrar(Tipo.EXITO, "Listo", mensaje); }
    public static void error(String titulo, String mensaje) { mostrar(Tipo.ERROR, titulo, mensaje); }
    public static void info(String mensaje) { mostrar(Tipo.INFO, "Aviso", mensaje); }

    public static void mostrar(Tipo tipo, String titulo, String mensaje) {
        if (capa == null) {
            return;
        }
        Label lTitulo = new Label(titulo);
        lTitulo.getStyleClass().add("toast-titulo");
        Label lMensaje = new Label(mensaje);
        lMensaje.getStyleClass().add("toast-mensaje");
        lMensaje.setWrapText(true);

        VBox toast = new VBox(2, lTitulo, lMensaje);
        toast.getStyleClass().addAll("toast", "toast-" + tipo.name().toLowerCase());
        toast.setMaxWidth(360);
        toast.setPrefWidth(360);

        pila.getChildren().add(toast);
        if (pila.getChildren().size() > 3) {
            pila.getChildren().remove(0);
        }

        toast.setOpacity(0);
        toast.setTranslateX(40);
        FadeTransition aparecer = new FadeTransition(Duration.millis(220), toast);
        aparecer.setToValue(1);
        TranslateTransition entrar = new TranslateTransition(Duration.millis(260), toast);
        entrar.setToX(0);
        PauseTransition espera = new PauseTransition(Duration.seconds(tipo == Tipo.ERROR ? 4.5 : 3));
        FadeTransition desaparecer = new FadeTransition(Duration.millis(300), toast);
        desaparecer.setToValue(0);

        SequentialTransition seq = new SequentialTransition(new ParallelTransition(aparecer, entrar), espera, desaparecer);
        seq.setOnFinished(e -> pila.getChildren().remove(toast));
        seq.play();
    }
}
