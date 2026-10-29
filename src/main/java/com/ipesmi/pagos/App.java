package com.ipesmi.pagos;

import com.ipesmi.pagos.controller.MainController;
import com.ipesmi.pagos.dao.JPAUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/** Punto de entrada JavaFX. */
public class App extends Application {

    public static final String CSS = "/com/ipesmi/pagos/css/estilos.css";

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/ipesmi/pagos/view/Main.fxml"));
        Parent raiz = loader.load();
        MainController controlador = loader.getController();

        // 1320x820 si entra; en pantallas chicas (ej. 1366x768) se abre maximizada
        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        boolean chica = area.getWidth() < 1400 || area.getHeight() < 860;
        Scene escena = new Scene(raiz, Math.min(1320, area.getWidth()), Math.min(820, area.getHeight() - 40));
        escena.getStylesheets().add(Objects.requireNonNull(App.class.getResource(CSS)).toExternalForm());
        controlador.registrarAtajos(escena);

        stage.setTitle("Gestión de Pagos y Aranceles · IPESMI");
        stage.setMinWidth(1100);
        stage.setMinHeight(640);
        stage.setScene(escena);
        stage.setMaximized(chica);
        stage.show();

        controlador.conectar();
    }

    @Override
    public void stop() {
        JPAUtil.cerrar();
        // El simulador Swing deja vivo el hilo de AWT: se fuerza la salida.
        System.exit(0);
    }
}
