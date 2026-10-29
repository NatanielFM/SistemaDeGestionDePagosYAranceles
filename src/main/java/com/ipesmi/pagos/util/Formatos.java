package com.ipesmi.pagos.util;

import com.ipesmi.pagos.exception.MontoInvalidoException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formato de moneda y fechas en español (Argentina) y lectura de montos escritos por el usuario. */
public final class Formatos {

    public static final Locale AR = Locale.of("es", "AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", AR);

    private Formatos() {
    }

    public static String moneda(BigDecimal valor) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(AR);
        return nf.format(valor == null ? BigDecimal.ZERO : valor);
    }

    /** Sin decimales, para cifras grandes del resumen. */
    public static String monedaCorta(double valor) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(AR);
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return nf.format(valor);
    }

    public static String porcentaje(double valor) {
        return (valor == Math.rint(valor) ? String.valueOf((long) valor) : String.format(AR, "%.2f", valor)) + " %";
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "—" : fecha.format(FECHA);
    }

    public static String fechaLarga(LocalDate fecha) {
        String texto = fecha.format(FECHA_LARGA);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    /**
     * Interpreta "45000", "45000.50", "45.000,50" o "$ 45.000". Una coma
     * siempre es el separador decimal; con coma presente los puntos son de miles.
     */
    public static BigDecimal leerMonto(String texto) throws MontoInvalidoException {
        if (texto == null || texto.isBlank()) {
            throw new MontoInvalidoException("Debe ingresar un monto");
        }
        String limpio = texto.replace("$", "").replace(" ", "").replace(" ", "").strip();
        if (limpio.contains(",")) {
            limpio = limpio.replace(".", "").replace(',', '.');
        } else if (limpio.indexOf('.') != limpio.lastIndexOf('.')) {
            limpio = limpio.replace(".", "");
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw new MontoInvalidoException("\"" + texto.strip() + "\" no es un monto válido");
        }
    }
}
