package com.ipesmi.pagos.model;

public enum EstadoPago {
    /** Registrado pero todavía no acreditado (por ejemplo, una transferencia). */
    PENDIENTE("Pendiente"),
    ACREDITADO("Acreditado"),
    ANULADO("Anulado");

    private final String etiqueta;

    EstadoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
