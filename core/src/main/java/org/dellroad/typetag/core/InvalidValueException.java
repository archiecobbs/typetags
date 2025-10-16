/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetag.core;

/**
 * Thrown by a {@link Validator} or {@link AnnotationValidator} when an invalid value is encountered.
 */
@SuppressWarnings("serial")
public class InvalidValueException extends RuntimeException {

    /**
     * Constructor.
     *
     * @param message a description of the failure, or null for none
     */
    public InvalidValueException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message a description of the failure, or null for none
     * @param cause underlying cause
     */
    public InvalidValueException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     *
     * @param cause underlying cause
     */
    public InvalidValueException(Throwable cause) {
        super(cause);
    }

    @Override
    public InvalidValueException initCause(Throwable t) {
        return (InvalidValueException)super.initCause(t);
    }
}
