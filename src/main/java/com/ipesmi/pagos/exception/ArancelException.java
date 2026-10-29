package com.ipesmi.pagos.exception;

/**
 * Base de las excepciones de negocio del sistema. Es verificada (checked) para
 * que el compilador obligue a la capa de vista a tratar cada caso.
 */
public abstract class ArancelException extends Exception {

    protected ArancelException(String mensaje) {
        super(mensaje);
    }
}
