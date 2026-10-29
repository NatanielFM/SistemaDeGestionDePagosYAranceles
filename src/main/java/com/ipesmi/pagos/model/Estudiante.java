package com.ipesmi.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Estudiante del instituto. El legajo es la clave primaria y también la clave
 * del HashMap&lt;Integer, Estudiante&gt; que usa la capa de servicio.
 */
@Entity
@Table(name = "estudiante")
public class Estudiante {

    @Id
    @Column(name = "legajo")
    private Integer legajo;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String apellido;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** Porcentaje de beca entre 0 y 100. Solo se aplica a conceptos becables. */
    @Column(name = "porcentaje_beca", nullable = false)
    private double porcentajeBeca;

    @Column(nullable = false)
    private boolean activo = true;

    protected Estudiante() {
        // requerido por JPA
    }

    public Estudiante(Integer legajo, String nombre, String apellido, String email) {
        this.legajo = legajo;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
    }

    public Integer getLegajo() { return legajo; }
    public void setLegajo(Integer legajo) { this.legajo = legajo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getPorcentajeBeca() { return porcentajeBeca; }
    public void setPorcentajeBeca(double porcentajeBeca) { this.porcentajeBeca = porcentajeBeca; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public boolean tieneBeca() {
        return porcentajeBeca > 0;
    }

    public String getNombreCompleto() {
        return apellido + ", " + nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Estudiante otro)) return false;
        return legajo != null && legajo.equals(otro.legajo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(legajo);
    }

    @Override
    public String toString() {
        return legajo + " - " + getNombreCompleto();
    }
}
