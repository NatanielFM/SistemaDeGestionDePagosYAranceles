package com.ipesmi.pagos.exception;

/**
 * Se lanza cuando un monto es nulo, menor o igual a cero, o supera el saldo
 * que el estudiante tiene pendiente para ese concepto.
 */
public class MontoInvalidoException extends ArancelException {

    public MontoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
