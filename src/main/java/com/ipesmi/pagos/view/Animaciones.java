package com.ipesmi.pagos.view;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.function.DoubleFunction;

/** Animaciones reutilizables de la interfaz. */
public final class Animaciones {

    private static final Interpolator SUAVE = Interpolator.SPLINE(0.2, 0.8, 0.2, 1.0);

    private Animaciones() {
    }

    /** Entrada de una vista: aparece y sube unos pocos píxeles. */
    public static void entrada(Node nodo) {
        nodo.setOpacity(0);
        nodo.setTranslateY(14);
        FadeTransition fade = new FadeTransition(Duration.millis(320), nodo);
        fade.setToValue(1);
        TranslateTransition subir = new TranslateTransition(Duration.millis(380), nodo);
        subir.setToY(0);
        subir.setInterpolator(SUAVE);
        new ParallelTransition(fade, subir).play();
    }

    /** Entrada escalonada de varios elementos (por ejemplo las cifras del resumen). */
    public static void cascada(Node... nodos) {
        for (int i = 0; i < nodos.length; i++) {
            Node n = nodos[i];
            n.setOpacity(0);
            n.setTranslateY(18);
            FadeTransition fade = new FadeTransition(Duration.millis(350), n);
            fade.setToValue(1);
            TranslateTransition subir = new TranslateTransition(Duration.millis(450), n);
            subir.setToY(0);
            subir.setInterpolator(SUAVE);
            ParallelTransition p = new ParallelTransition(fade, subir);
            p.setDelay(Duration.millis(70L * i));
            p.play();
        }
    }

    /** Sacude un campo para señalar un dato inválido. */
    public static void sacudir(Node nodo) {
        TranslateTransition t = new TranslateTransition(Duration.millis(55), nodo);
        t.setFromX(0);
        t.setByX(7);
        t.setCycleCount(6);
        t.setAutoReverse(true);
        t.setOnFinished(e -> nodo.setTranslateX(0));
        t.play();
    }

    /** Pequeño "pulso" para confirmar una acción. */
    public static void pulso(Node nodo) {
        ScaleTransition s = new ScaleTransition(Duration.millis(140), nodo);
        s.setToX(1.06);
        s.setToY(1.06);
        s.setAutoReverse(true);
        s.setCycleCount(2);
        s.play();
    }

    /** Cuenta desde 0 hasta el valor, mostrando el texto con el formato dado. */
    public static void contador(Labeled etiqueta, double valor, DoubleFunction<String> formato) {
        DoubleProperty actual = new SimpleDoubleProperty(0);
        actual.addListener((obs, a, b) -> etiqueta.setText(formato.apply(b.doubleValue())));
        etiqueta.setText(formato.apply(0));
        new Timeline(new KeyFrame(Duration.millis(900), new KeyValue(actual, valor, SUAVE))).play();
    }

    /** Hace crecer el ancho de una barra hasta el valor indicado. */
    public static void crecerAncho(Region barra, double ancho, int retardoMs) {
        barra.setPrefWidth(0);
        Timeline t = new Timeline(new KeyFrame(Duration.millis(700), new KeyValue(barra.prefWidthProperty(), ancho, SUAVE)));
        t.setDelay(Duration.millis(retardoMs));
        t.play();
    }

    /** Desliza un nodo verticalmente hasta la posición indicada. */
    public static void deslizarY(Node nodo, double y) {
        TranslateTransition t = new TranslateTransition(Duration.millis(260), nodo);
        t.setToY(y);
        t.setInterpolator(SUAVE);
        t.play();
    }
}
