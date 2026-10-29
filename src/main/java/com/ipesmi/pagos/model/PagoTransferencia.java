package com.ipesmi.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago por transferencia bancaria. Queda PENDIENTE hasta que tesorería lo
 * acredita; la referencia es el número de operación del banco.
 */
@Entity
@DiscriminatorValue("TRANSFERENCIA")
public class PagoTransferencia extends Pago {

    @Column(name = "banco", length = 60)
    private String banco;

    protected PagoTransferencia() {
    }

    public PagoTransferencia(Estudiante estudiante, Concepto concepto, BigDecimal monto, LocalDate fecha,
                             String nroOperacion, String banco) {
        super(estudiante, concepto, monto, fecha, nroOperacion);
        this.banco = banco;
    }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }

    @Override
    public MedioPago getMedio() {
        return MedioPago.TRANSFERENCIA;
    }

    @Override
    public String getDetalleMedio() {
        String origen = (banco == null || banco.isBlank()) ? "" : " · " + banco;
        return "Transferencia op. " + getReferencia() + origen;
    }
}
