package com.ipesmi.pagos.controller;

import com.ipesmi.pagos.dao.JPAUtil;
import com.ipesmi.pagos.service.AppContext;
import com.ipesmi.pagos.swing.SimuladorBecaFrame;
import com.ipesmi.pagos.view.Animaciones;
import com.ipesmi.pagos.view.Notificador;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador de la ventana principal: barra lateral, navegación entre vistas
 * y conexión inicial con la base de datos (en segundo plano).
 */
public class MainController {

    @FXML private StackPane capa;
    @FXML private StackPane contenido;
    @FXML private Region indicador;
    @FXML private VBox nav;
    @FXML private Button btnResumen;
    @FXML private Button btnPagos;
    @FXML private Button btnConceptos;
    @FXML private Button btnEstudiantes;
    @FXML private Button btnBitacora;
    @FXML private Button btnSimulador;
    @FXML private Label lblConexion;

    private record Vista(Parent nodo, Object controlador) {
    }

    private final Map<String, Vista> vistas = new HashMap<>();
    private final Map<String, Button> botones = new HashMap<>();
    private Button activo;

    @FXML
    private void initialize() {
        Notificador.instalar(capa);
        botones.put("Resumen", btnResumen);
        botones.put("Pagos", btnPagos);
        botones.put("Conceptos", btnConceptos);
        botones.put("Estudiantes", btnEstudiantes);
        botones.put("Bitacora", btnBitacora);
        indicador.setVisible(false);
        habilitarNavegacion(false);
    }

    // ------------------------------------------------------------ conexión

    public void conectar() {
        ProgressIndicator progreso = new ProgressIndicator();
        progreso.setPrefSize(34, 34);
        Label titulo = new Label("Conectando con la base de datos");
        titulo.getStyleClass().add("titulo");
        Label detalle = new Label(JPAUtil.getUrl());
        detalle.getStyleClass().add("subtitulo");
        VBox espera = new VBox(14, progreso, titulo, detalle);
        espera.getStyleClass().add("pantalla-estado");
        mostrarNodo(espera);
        lblConexion.setText("Conectando…");

        Task<AppContext> tarea = new Task<>() {
            @Override
            protected AppContext call() {
                return AppContext.get();
            }
        };
        tarea.setOnSucceeded(e -> {
            lblConexion.setText("● Conectado\n" + descripcionBase());
            habilitarNavegacion(true);
            mostrar("Resumen");
        });
        tarea.setOnFailed(e -> mostrarErrorConexion(tarea.getException()));
        Thread hilo = new Thread(tarea, "conexion-bd");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarErrorConexion(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        lblConexion.setText("Sin conexión");

        Label titulo = new Label("No se pudo conectar");
        titulo.getStyleClass().add("titulo");
        Label mensaje = new Label(causa.getMessage());
        mensaje.setWrapText(true);
        mensaje.getStyleClass().add("subtitulo");
        Label ayuda = new Label("Ejecute sql/pagos_aranceles.sql en MySQL y revise el usuario y la contraseña en config/db.properties:");
        ayuda.setWrapText(true);
        Label config = new Label("""
                jakarta.persistence.jdbc.url=jdbc:mysql://localhost:3306/pagos_aranceles
                jakarta.persistence.jdbc.user=root
                jakarta.persistence.jdbc.password=********""");
        config.getStyleClass().add("codigo-config");
        Label demo = new Label("También puede probar sin MySQL ejecutando con -Dpagos.demo=true (base H2 local).");
        demo.setWrapText(true);
        demo.getStyleClass().add("texto-apagado");

        Button reintentar = new Button("Reintentar");
        reintentar.getStyleClass().add("btn-primario");
        reintentar.setOnAction(e -> {
            JPAUtil.cerrar();
            AppContext.reiniciar();
            conectar();
        });

        VBox panel = new VBox(14, titulo, mensaje, ayuda, config, demo, reintentar);
        panel.getStyleClass().add("pantalla-estado");
        mostrarNodo(panel);
    }

    private static String descripcionBase() {
        String url = JPAUtil.getUrl();
        if (url.startsWith("jdbc:h2")) {
            return "H2 · modo demo";
        }
        String sinPrefijo = url.replaceFirst("^jdbc:mysql://", "");
        int q = sinPrefijo.indexOf('?');
        return "MySQL · " + (q >= 0 ? sinPrefijo.substring(0, q) : sinPrefijo);
    }

    // ---------------------------------------------------------- navegación

    @FXML private void irResumen() { mostrar("Resumen"); }
    @FXML private void irPagos() { mostrar("Pagos"); }
    @FXML private void irConceptos() { mostrar("Conceptos"); }
    @FXML private void irEstudiantes() { mostrar("Estudiantes"); }
    @FXML private void irBitacora() { mostrar("Bitacora"); }

    /** Muestra una vista por nombre (Resumen, Pagos, Conceptos, Estudiantes, Bitacora). */
    public void mostrar(String nombre) {
        Vista vista = vistas.computeIfAbsent(nombre, this::cargarVista);
        if (vista.controlador() instanceof Refrescable r) {
            r.refrescar();
        }
        mostrarNodo(vista.nodo());
        marcarActivo(botones.get(nombre));
    }

    public Object getControlador(String nombre) {
        return vistas.computeIfAbsent(nombre, this::cargarVista).controlador();
    }

    private Vista cargarVista(String nombre) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ipesmi/pagos/view/" + nombre + ".fxml"));
        try {
            Parent nodo = loader.load();
            return new Vista(nodo, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + nombre, e);
        }
    }

    private void mostrarNodo(Node nodo) {
        contenido.getChildren().setAll(nodo);
        Animaciones.entrada(nodo);
    }

    private void marcarActivo(Button boton) {
        if (activo != null) {
            activo.getStyleClass().remove("activo");
        }
        activo = boton;
        if (boton == null) {
            return;
        }
        boton.getStyleClass().add("activo");
        double y = boton.getBoundsInParent().getMinY();
        if (!indicador.isVisible()) {
            indicador.setTranslateY(y);
            indicador.setVisible(true);
        } else {
            Animaciones.deslizarY(indicador, y);
        }
    }

    private void habilitarNavegacion(boolean habilitar) {
        for (Button b : List.of(btnResumen, btnPagos, btnConceptos, btnEstudiantes, btnBitacora, btnSimulador)) {
            b.setDisable(!habilitar);
        }
    }

    /** Ctrl+1 … Ctrl+5 para cambiar de vista. */
    public void registrarAtajos(Scene escena) {
        String[] orden = {"Resumen", "Pagos", "Conceptos", "Estudiantes", "Bitacora"};
        KeyCode[] teclas = {KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4, KeyCode.DIGIT5};
        for (int i = 0; i < orden.length; i++) {
            String destino = orden[i];
            escena.getAccelerators().put(new KeyCodeCombination(teclas[i], KeyCombination.SHORTCUT_DOWN), () -> {
                if (!btnResumen.isDisabled()) {
                    mostrar(destino);
                }
            });
        }
    }

    // ------------------------------------------------------------- Swing

    /** Abre el simulador hecho en Swing (Unidad 5) en su propio hilo de eventos (EDT). */
    @FXML
    private void abrirSimulador() {
        var conceptos = AppContext.get().conceptos().listar();
        SwingUtilities.invokeLater(() -> new SimuladorBecaFrame(conceptos).setVisible(true));
    }
}
