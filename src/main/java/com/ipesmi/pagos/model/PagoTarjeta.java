package com.ipesmi.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pago con tarjeta. La referencia guarda solo los últimos 4 dígitos. */
@Entity
@DiscriminatorValue("TARJETA")
public class PagoTarjeta extends Pago {

    @Column(name = "cuotas")
    private Integer cuotas;

    protected PagoTarjeta() {
    }

    public PagoTarjeta(Estudiante estudiante, Concepto concepto, BigDecimal monto, LocalDate fecha,
                       String ultimosDigitos, int cuotas) {
        super(estudiante, concepto, monto, fecha, ultimosDigitos);
        this.cuotas = cuotas;
    }

    public Integer getCuotas() { return cuotas; }
    public void setCuotas(Integer cuotas) { this.cuotas = cuotas; }

    @Override
    public MedioPago getMedio() {
        return MedioPago.TARJETA;
    }

    @Override
    public String getDetalleMedio() {
        String plan = (cuotas == null || cuotas <= 1) ? "1 pago" : cuotas + " cuotas";
        return "Tarjeta ****" + getReferencia() + " · " + plan;
    }
}
