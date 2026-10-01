package com.quinielas.del.canario.api.exception;

/** Lanzada cuando un usuario con cuenta desactivada (activo=false) intenta iniciar sesión. */
public class CuentaInactivaException extends RuntimeException {
    public CuentaInactivaException(String message) {
        super(message);
    }
}
