package com.ipesmi.pagos.exception;

/**
 * Se lanza cuando se intenta registrar un pago sobre un concepto que el
 * estudiante ya tiene cubierto, o con una referencia (recibo / nro. de
 * operación) que ya existe para el mismo medio de pago.
 */
public class PagoDuplicadoException extends ArancelException {

    public PagoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
