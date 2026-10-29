package com.ipesmi.pagos;

import com.ipesmi.pagos.controller.MainController;
import com.ipesmi.pagos.dao.JPAUtil;
import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.Estudiante;
import com.ipesmi.pagos.service.AppContext;
import com.ipesmi.pagos.service.DatosDemo;
import com.ipesmi.pagos.swing.SimuladorBecaFrame;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Herramienta (no es una prueba) que abre la aplicación con datos de ejemplo
 * sobre H2 en memoria, recorre las pantallas y guarda capturas en docs/capturas.
 * <pre>mvn test-compile exec:java -Dexec.mainClass=com.ipesmi.pagos.CapturasPantalla -Dexec.classpathScope=test</pre>
 */
public class CapturasPantalla {

    public static void main(String[] args) throws Exception {
        Path log = Path.of("target", "capturas-transacciones.log");
        Files.deleteIfExists(log);
        System.setProperty("pagos.log", log.toString());
        Application.launch(App.class, args);
    }

    public static class App extends Application {

        private static final File DESTINO = new File("docs/capturas");
        private record Paso(Runnable accion, int esperaMs, String captura) {
        }

        private final List<Paso> pasos = new ArrayList<>();
        private Scene escena;
        private MainController main;

        @Override
        public void start(Stage stage) throws Exception {
            DESTINO.mkdirs();
            Map<String, String> props = new HashMap<>(JPAUtil.h2("jdbc:h2:mem:capturas;DB_CLOSE_DELAY=-1;MODE=MySQL"));
            props.put("hibernate.hbm2ddl.auto", "create-drop");
            JPAUtil.configurar(props);
            DatosDemo.cargarSiVacia(AppContext.get());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ipesmi/pagos/view/Main.fxml"));
            Parent raiz = loader.load();
            main = loader.getController();
            // área útil de una ventana maximizada en una pantalla de 1366x768
            escena = new Scene(raiz, 1366, 700);
            escena.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource(com.ipesmi.pagos.App.CSS)).toExternalForm());
            stage.setTitle("Gestión de Pagos y Aranceles · IPESMI");
            stage.setScene(escena);
            stage.show();
            main.conectar();

            var ctx = AppContext.get();
            Estudiante lucia = ctx.estudiantes().buscar(1001).orElseThrow();
            Estudiante camila = ctx.estudiantes().buscar(1003).orElseThrow();
            Estudiante joaquin = ctx.estudiantes().buscar(1004).orElseThrow();
            Estudiante valentina = ctx.estudiantes().buscar(1005).orElseThrow();
            Concepto mat = ctx.conceptos().buscar("MAT-2026").orElseThrow();
            Concepto cuo4 = ctx.conceptos().buscar("CUO-04").orElseThrow();
            Concepto cuo5 = ctx.conceptos().buscar("CUO-05").orElseThrow();

            paso(() -> { }, 1800, "01_resumen_por_medio");
            paso(() -> this.<ToggleButton>buscar("#tbPorConcepto").fire(), 1300, "02_resumen_por_concepto");
            paso(() -> main.mostrar("Pagos"), 900, "03_pagos_listado");
            paso(() -> {
                this.<ComboBox<Estudiante>>buscar("#cmbEstudiante").setValue(camila);
                this.<ComboBox<Concepto>>buscar("#cmbConcepto").setValue(cuo4);
                this.<ToggleButton>buscar("#tbTarjeta").fire();
                this.<TextField>buscar("#txtReferencia").setText("5544");
                this.<Spinner<Integer>>buscar("#spCuotas").getValueFactory().setValue(3);
            }, 700, "04_pagos_formulario_tarjeta");
            paso(() -> this.<Button>buscar("#btnRegistrar").fire(), 800, "05_pagos_registrado");
            paso(() -> {
                this.<ComboBox<Estudiante>>buscar("#cmbEstudiante").setValue(lucia);
                this.<ComboBox<Concepto>>buscar("#cmbConcepto").setValue(mat);
                this.<Button>buscar("#btnRegistrar").fire();
            }, 800, "06_error_pago_duplicado");
            paso(() -> {
                this.<ComboBox<Estudiante>>buscar("#cmbEstudiante").setValue(joaquin);
                this.<ComboBox<Concepto>>buscar("#cmbConcepto").setValue(cuo5);
                this.<TextField>buscar("#txtMonto").setText("-500");
                this.<Button>buscar("#btnRegistrar").fire();
            }, 800, "07_error_monto_invalido");
            paso(() -> {
                this.<Button>buscar("#btnLimpiar").fire();
                this.<ComboBox<String>>buscar("#cmbFiltroMedio").getSelectionModel().select("Transferencia");
            }, 700, "08_pagos_filtro_transferencias");
            paso(() -> {
                this.<ComboBox<String>>buscar("#cmbFiltroMedio").getSelectionModel().selectFirst();
                main.mostrar("Conceptos");
            }, 500, null);
            paso(() -> this.<TableView<Concepto>>buscar("#tabla").getSelectionModel().select(1), 900, "09_conceptos");
            paso(() -> main.mostrar("Estudiantes"), 500, null);
            paso(() -> seleccionarEstudiante(lucia), 900, "10_estudiantes_cuenta_corriente");
            paso(() -> {
                seleccionarEstudiante(valentina);
                this.<TextField>buscar("#txtBeca").setText("150");
                this.<Button>buscar("#btnAplicarBeca").fire();
            }, 900, "11_error_beca_no_aplicable");
            paso(() -> {
                seleccionarEstudiante(joaquin);
                this.<TextField>buscar("#txtBeca").setText("40");
                this.<Button>buscar("#btnAplicarBeca").fire();
            }, 900, "12_beca_aplicada");
            paso(() -> main.mostrar("Bitacora"), 900, "13_bitacora");
            paso(() -> main.mostrar("Resumen"), 1800, "14_resumen_actualizado");
            paso(this::capturarSwing, 5000, null);
            paso(() -> {
                JPAUtil.cerrar();
                Platform.exit();
                System.exit(0);
            }, 0, null);

            ejecutar(0);
        }

        @SuppressWarnings("unchecked")
        private <T> T buscar(String selector) {
            return (T) Objects.requireNonNull(escena.lookup(selector), "no se encontró " + selector);
        }

        private void seleccionarEstudiante(Estudiante e) {
            TableView<Estudiante> tabla = buscar("#tabla");
            tabla.getSelectionModel().select(e);
        }

        private void paso(Runnable accion, int esperaMs, String captura) {
            pasos.add(new Paso(accion, esperaMs, captura));
        }

        private void ejecutar(int i) {
            if (i >= pasos.size()) {
                return;
            }
            Paso p = pasos.get(i);
            p.accion().run();
            PauseTransition espera = new PauseTransition(Duration.millis(Math.max(1, p.esperaMs())));
            espera.setOnFinished(e -> {
                if (p.captura() != null) {
                    guardar(p.captura());
                }
                ejecutar(i + 1);
            });
            espera.play();
        }

        private void guardar(String nombre) {
            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(Transform.scale(1.5, 1.5));
            WritableImage img = escena.getRoot().snapshot(params, null);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", new File(DESTINO, nombre + ".png"));
                System.out.println("captura: " + nombre);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        /** Abre el formulario Swing, ajusta valores y lo dibuja en un PNG. */
        private void capturarSwing() {
            List<Concepto> conceptos = AppContext.get().conceptos().listar();
            SwingUtilities.invokeLater(() -> {
                SimuladorBecaFrame frame = new SimuladorBecaFrame(conceptos);
                frame.setVisible(true);
                buscarComponente(frame.getContentPane(), JComboBox.class, 0).setSelectedIndex(1);
                buscarComponente(frame.getContentPane(), JSlider.class, 0).setValue(50);
                buscarComponente(frame.getContentPane(), JComboBox.class, 1).setSelectedIndex(1);
                buscarComponente(frame.getContentPane(), JSpinner.class, 0).setValue(3);
                javax.swing.Timer t = new javax.swing.Timer(700, ev -> {
                    BufferedImage img = new BufferedImage(frame.getContentPane().getWidth() * 3 / 2,
                            frame.getContentPane().getHeight() * 3 / 2, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = img.createGraphics();
                    g.scale(1.5, 1.5);
                    frame.getContentPane().paint(g);
                    g.dispose();
                    try {
                        ImageIO.write(img, "png", new File(DESTINO, "15_simulador_swing.png"));
                        System.out.println("captura: 15_simulador_swing");
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    frame.dispose();
                });
                t.setRepeats(false);
                t.start();
            });
        }

        @SuppressWarnings("unchecked")
        private static <C extends java.awt.Component> C buscarComponente(java.awt.Container raiz, Class<C> tipo, int indice) {
            List<C> encontrados = new ArrayList<>();
            recolectar(raiz, tipo, encontrados);
            return encontrados.get(indice);
        }

        private static <C extends java.awt.Component> void recolectar(java.awt.Container raiz, Class<C> tipo, List<C> salida) {
            for (java.awt.Component c : raiz.getComponents()) {
                if (tipo.isInstance(c)) {
                    salida.add(tipo.cast(c));
                }
                if (c instanceof java.awt.Container cont) {
                    recolectar(cont, tipo, salida);
                }
            }
        }
    }
}
