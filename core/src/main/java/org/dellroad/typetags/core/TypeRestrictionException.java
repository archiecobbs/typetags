/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.core;

/**
 * Specialization of {@link ClassCastException} thrown by {@link TypeTagValidator#validate TypeTagValidator.validate()}
 * to indicate that a value is invalid because of its actual value rather than simply by having the wrong Java type.
 */
@SuppressWarnings("serial")
public class TypeRestrictionException extends ClassCastException {

    /**
     * Constructor.
     *
     * @param message a description of the failure, or null for none
     */
    public TypeRestrictionException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message a description of the failure, or null for none
     * @param cause underlying cause
     */
    @SuppressWarnings("this-escape")
    public TypeRestrictionException(String message, Throwable cause) {
        super(message);
        this.initCause(cause);
    }

    /**
     * Constructor.
     *
     * @param cause underlying cause
     */
    @SuppressWarnings("this-escape")
    public TypeRestrictionException(Throwable cause) {
        this.initCause(cause);
    }

    @Override
    public TypeRestrictionException initCause(Throwable t) {
        return (TypeRestrictionException)super.initCause(t);
    }
}
