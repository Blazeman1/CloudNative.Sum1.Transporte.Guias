package com.transporte.guias.exception;

/**
 * Excepción lanzada cuando no se encuentra una guía por su ID
 * Resulta en una respuesta HTTP 404 Not Found
 */
public class GuiaNotFoundException extends RuntimeException {

    private final String guiaId;

    public GuiaNotFoundException(String guiaId) {
        super("Guía no encontrada con ID: " + guiaId);
        this.guiaId = guiaId;
    }

    public GuiaNotFoundException(String guiaId, String mensaje) {
        super(mensaje);
        this.guiaId = guiaId;
    }

    public String getGuiaId() {
        return guiaId;
    }
}
