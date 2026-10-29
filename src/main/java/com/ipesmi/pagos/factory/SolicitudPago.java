package com.ipesmi.pagos.factory;

import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.Estudiante;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de entrada que recibe {@link PagoFactory}. Los campos {@code cuotas} y
 * {@code banco} solo los usan la tarjeta y la transferencia respectivamente.
 */
public record SolicitudPago(Estudiante estudiante,
                            Concepto concepto,
                            BigDecimal monto,
                            LocalDate fecha,
                            String referencia,
                            int cuotas,
                            String banco) {

    public static SolicitudPago de(Estudiante estudiante, Concepto concepto, BigDecimal monto,
                                   LocalDate fecha, String referencia) {
        return new SolicitudPago(estudiante, concepto, monto, fecha, referencia, 1, null);
    }
}
