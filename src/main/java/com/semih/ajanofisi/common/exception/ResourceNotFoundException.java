package com.semih.ajanofisi.common.exception;

/** Thrown when a requested record does not exist; mapped to 404. */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
