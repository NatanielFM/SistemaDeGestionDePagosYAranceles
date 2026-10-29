package com.ipesmi.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Concepto de pago (matrícula, cuota, derecho de examen...). Todos los
 * estudiantes activos deben abonar cada concepto registrado.
 */
@Entity
@Table(name = "concepto")
public class Concepto {

    @Id
    @Column(length = 20)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoConcepto tipo;

    /** Si es false, la beca del estudiante no reduce este concepto. */
    @Column(nullable = false)
    private boolean becable = true;

    private LocalDate vencimiento;

    protected Concepto() {
        // requerido por JPA
    }

    public Concepto(String codigo, String descripcion, BigDecimal monto, TipoConcepto tipo, boolean becable) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.monto = monto;
        this.tipo = tipo;
        this.becable = becable;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public TipoConcepto getTipo() { return tipo; }
    public void setTipo(TipoConcepto tipo) { this.tipo = tipo; }

    public boolean isBecable() { return becable; }
    public void setBecable(boolean becable) { this.becable = becable; }

    public LocalDate getVencimiento() { return vencimiento; }
    public void setVencimiento(LocalDate vencimiento) { this.vencimiento = vencimiento; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Concepto otro)) return false;
        return codigo != null && codigo.equals(otro.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }

    @Override
    public String toString() {
        return codigo + " - " + descripcion;
    }
}
