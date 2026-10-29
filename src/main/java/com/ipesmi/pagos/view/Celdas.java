package com.ipesmi.pagos.view;

import com.ipesmi.pagos.model.Pago;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.function.Function;

/** Fábricas de celdas para las TableView. */
public final class Celdas {

    private Celdas() {
    }

    /** Celda de texto con formato y una clase CSS opcional (por ejemplo "celda-monto"). */
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> texto(Function<T, String> formato, String estilo) {
        return col -> {
            TableCell<S, T> celda = new TableCell<>() {
                @Override
                protected void updateItem(T item, boolean vacio) {
                    super.updateItem(item, vacio);
                    setText(vacio || item == null ? null : formato.apply(item));
                }
            };
            if (estilo != null) {
                celda.getStyleClass().add(estilo);
            }
            return celda;
        };
    }

    /** Etiqueta redondeada de color (estado del pago, beca, activo/inactivo). */
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> pill(Function<T, String> texto,
                                                                          Function<T, String> estilo) {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(null);
                if (vacio || item == null) {
                    setGraphic(null);
                    return;
                }
                String t = texto.apply(item);
                if (t == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(t);
                pill.getStyleClass().addAll("pill", estilo.apply(item));
                pill.setMinWidth(Region.USE_PREF_SIZE);   // nunca se recorta con "..."
                setGraphic(pill);
            }
        };
    }

    /** Punto de color según el medio, el medio y su referencia; el detalle completo va en el tooltip. */
    public static <S> Callback<TableColumn<S, Pago>, TableCell<S, Pago>> medioPago() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(Pago pago, boolean vacio) {
                super.updateItem(pago, vacio);
                setText(null);
                if (vacio || pago == null) {
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                Region punto = new Region();
                punto.getStyleClass().addAll("punto-medio", "punto-" + pago.getMedio().name().toLowerCase(Locale.ROOT));
                Label medio = new Label(pago.getMedio().toString());
                medio.setMinWidth(Region.USE_PREF_SIZE);
                Label referencia = new Label(pago.getReferencia());
                referencia.getStyleClass().add("ref-medio");
                HBox caja = new HBox(8, punto, medio, referencia);
                caja.setAlignment(Pos.CENTER_LEFT);
                setGraphic(caja);
                setTooltip(new Tooltip(pago.getDetalleMedio()));
            }
        };
    }

    /** Convierte fechas del DatePicker en formato dd/MM/yyyy. */
    public static StringConverter<LocalDate> convertidorFecha() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate fecha) {
                return fecha == null ? "" : f.format(fecha);
            }

            @Override
            public LocalDate fromString(String texto) {
                if (texto == null || texto.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(texto.strip(), f);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        };
    }
}
