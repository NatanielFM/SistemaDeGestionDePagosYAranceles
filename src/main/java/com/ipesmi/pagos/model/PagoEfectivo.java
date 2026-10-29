package com.ipesmi.pagos.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pago en caja. Se acredita en el momento; la referencia es el número de recibo. */
@Entity
@DiscriminatorValue("EFECTIVO")
public class PagoEfectivo extends Pago {

    protected PagoEfectivo() {
    }

    public PagoEfectivo(Estudiante estudiante, Concepto concepto, BigDecimal monto, LocalDate fecha, String nroRecibo) {
        super(estudiante, concepto, monto, fecha, nroRecibo);
    }

    @Override
    public MedioPago getMedio() {
        return MedioPago.EFECTIVO;
    }

    @Override
    public String getDetalleMedio() {
        return "Efectivo · recibo " + getReferencia();
    }
}
