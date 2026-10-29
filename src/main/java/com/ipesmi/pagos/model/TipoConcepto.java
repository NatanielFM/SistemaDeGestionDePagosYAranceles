package com.ipesmi.pagos.model;

public enum TipoConcepto {
    MATRICULA("Matrícula"),
    CUOTA("Cuota"),
    EXAMEN("Examen"),
    OTRO("Otro");

    private final String etiqueta;

    TipoConcepto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
