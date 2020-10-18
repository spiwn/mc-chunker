package org.iz.cs.chunker.minecraft;

/**
 *  {@linkplain CompatibilityException} is thrown when the server version is not supported
 *
 */
public class CompatibilityException extends RuntimeException {

    private static final long serialVersionUID = -215215644410491975L;

    public CompatibilityException(String message) {
        super(message);
    }


}
