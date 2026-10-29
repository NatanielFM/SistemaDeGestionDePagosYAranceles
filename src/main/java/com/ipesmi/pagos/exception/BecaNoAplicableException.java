package com.ipesmi.pagos.exception;

/**
 * Se lanza cuando no se puede otorgar una beca: porcentaje fuera de (0, 100],
 * estudiante inactivo o sin conceptos becables con saldo.
 */
public class BecaNoAplicableException extends ArancelException {

    public BecaNoAplicableException(String mensaje) {
        super(mensaje);
    }
}
