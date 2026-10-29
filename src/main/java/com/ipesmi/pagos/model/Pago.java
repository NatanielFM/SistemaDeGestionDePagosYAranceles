package com.ipesmi.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Pago de un estudiante sobre un concepto. Es abstracta: cada medio de pago es
 * una subclase (herencia SINGLE_TABLE, la columna "medio" es el discriminador)
 * y las instancias se crean con {@link com.ipesmi.pagos.factory.PagoFactory}.
 */
@Entity
@Table(name = "pago")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "medio", length = 20)
public abstract class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "legajo_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "codigo_concepto", nullable = false)
    private Concepto concepto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Column(length = 60)
    private String referencia;

    protected Pago() {
        // requerido por JPA
    }

    protected Pago(Estudiante estudiante, Concepto concepto, BigDecimal monto, LocalDate fecha, String referencia) {
        this.estudiante = estudiante;
        this.concepto = concepto;
        this.monto = monto;
        this.fecha = fecha;
        this.referencia = referencia;
    }

    public abstract MedioPago getMedio();

    /** Texto corto que describe el medio, por ejemplo "Tarjeta ****4821 (3 cuotas)". */
    public abstract String getDetalleMedio();

    public boolean isAcreditado() { return estado == EstadoPago.ACREDITADO; }
    public boolean isAnulado() { return estado == EstadoPago.ANULADO; }

    public Long getId() { return id; }

    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }

    public Concepto getConcepto() { return concepto; }
    public void setConcepto(Concepto concepto) { this.concepto = concepto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pago otro)) return false;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Pago #" + id + " " + getMedio() + " $" + monto + " (" + estado + ")";
    }
}
