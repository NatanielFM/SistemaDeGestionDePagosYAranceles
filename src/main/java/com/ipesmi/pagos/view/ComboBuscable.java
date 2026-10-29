package com.ipesmi.pagos.view;

import com.ipesmi.pagos.util.Busqueda;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.function.Function;

/**
 * Convierte un ComboBox en un buscador con autocompletado: al escribir se
 * filtra la lista y se despliega sola. Pensado para listas largas (estudiantes).
 * <p>
 * Usar {@link #getSeleccionado()} en lugar de {@code combo.getValue()}: si el
 * usuario modificó el texto después de elegir, la selección ya no es válida y
 * devuelve null (así no se carga un pago al estudiante equivocado).
 */
public final class ComboBuscable<T> {

    private final ComboBox<T> combo;
    private final Function<T, String> texto;
    private final Function<T, String> clave;
    private final ObservableList<T> todos = FXCollections.observableArrayList();
    private final FilteredList<T> filtrados = new FilteredList<>(todos, t -> true);
    private final ObjectProperty<T> seleccionado = new SimpleObjectProperty<>();
    private boolean restaurando;

    /**
     * @param texto texto que se muestra al elegir un elemento
     * @param clave texto sobre el que se busca (ej.: legajo + apellido + nombre)
     */
    public ComboBuscable(ComboBox<T> combo, Function<T, String> texto, Function<T, String> clave) {
        this.combo = combo;
        this.texto = texto;
        this.clave = clave;

        combo.setEditable(true);
        combo.setItems(filtrados);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? "" : texto.apply(t);
            }

            /** Al confirmar (Enter / perder foco): coincidencia exacta o única; si no, sin selección. */
            @Override
            public T fromString(String s) {
                if (s == null || s.isBlank()) {
                    return null;
                }
                for (T t : todos) {
                    if (texto.apply(t).equals(s)) {
                        return t;
                    }
                }
                List<T> coincidencias = todos.stream().filter(t -> Busqueda.coincide(s, clave.apply(t))).toList();
                return coincidencias.size() == 1 ? coincidencias.get(0) : null;
            }
        });

        combo.valueProperty().addListener((o, a, nuevo) -> seleccionado.set(nuevo));
        combo.getEditor().textProperty().addListener((o, anterior, txt) -> alEscribir(txt));
    }

    private void alEscribir(String txt) {
        if (restaurando) {
            return;
        }
        T valor = combo.getValue();
        if (valor != null && texto.apply(valor).equals(txt)) {
            seleccionado.set(valor);   // el texto es el de la selección (la acaba de elegir)
            return;
        }
        if (valor != null) {
            // El usuario cambió el texto: se descarta la selección vieja. JavaFX borra el editor
            // al hacerlo, así que se repone lo escrito en el siguiente ciclo.
            restaurando = true;
            combo.setValue(null);
            restaurando = false;
            Platform.runLater(() -> {
                restaurando = true;
                combo.getEditor().setText(txt);
                combo.getEditor().positionCaret(txt.length());
                restaurando = false;
                filtrar(txt);
            });
            seleccionado.set(null);
            return;
        }
        seleccionado.set(null);
        filtrar(txt);
    }

    private void filtrar(String consulta) {
        filtrados.setPredicate(t -> Busqueda.coincide(consulta, clave.apply(t)));
        if (combo.getEditor().isFocused() && !consulta.isBlank() && !filtrados.isEmpty()) {
            if (!combo.isShowing()) {
                combo.show();
            }
        } else if (filtrados.isEmpty()) {
            combo.hide();
        }
    }

    /** Reemplaza los elementos disponibles (la selección se pierde: volver a elegirla si hace falta). */
    public void setDatos(List<T> datos) {
        restaurando = true;
        try {
            filtrados.setPredicate(t -> true);
            todos.setAll(datos);
        } finally {
            restaurando = false;
        }
    }

    /** Elemento elegido, o null si no se eligió ninguno o el texto fue modificado después. */
    public T getSeleccionado() {
        return seleccionado.get();
    }

    public ReadOnlyObjectProperty<T> seleccionadoProperty() {
        return seleccionado;
    }

    public void limpiar() {
        combo.setValue(null);
        combo.getEditor().clear();
        seleccionado.set(null);
        filtrados.setPredicate(t -> true);
    }
}
