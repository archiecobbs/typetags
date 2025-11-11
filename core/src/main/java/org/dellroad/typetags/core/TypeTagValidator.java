/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.core;

import java.lang.annotation.Annotation;

/**
 * Implemented by classes capable of validating values according to an annotation-based specification,
 * where the specifying annotation is meta-annotated as a {@link TypeTag}.
 *
 * <p>
 * <b>Note:</b> Implementations of this interface must also have a static, zero-argument method named
 * {@code getInstance()} that provides an instance of itself.
 *
 * @param <A> the type of the {@link TypeTag} meta-annotated annotation defining the validation specification
 */
public interface TypeTagValidator<A extends Annotation> {

    /**
     * Validate the given value according to the given specification, and if valid, return it.
     *
     * <p>
     * This method <i>should</i> return {@code value} unmodified in order to preserve composability.
     * However, in some cases, a different but equivalent "normalized" value may be returned.
     *
     * <p>
     * In any case, the choice is up to the implementer of this class: i.e., the caller of this method
     * <i>must</i> use the returned value, not the passed-in value, if validation is successful, i.e.,
     * no exception is thrown.
     *
     * <b>Checking {@link TypeTag#restrictTo} at Runtime</b>
     *
     * <p>
     * If {@link TypeTag#restrictTo} is non-empty, this method is responsible for validating that {@code value}
     * is an instance of one of the specified types. Since throwing {@link ClassCastException} is how this
     * method reports an invalid value, in many cases that check will happen automatically.
     *
     * <p>
     * Note also that primitive types in {@link TypeTag#restrictTo} must be handled speciallly: at runtime,
     * primitive values will be wrapped by the time this method sees them, so this method may need to map any
     * primitive types in {@link TypeTag#restrictTo} to their corresponding wrapper types (e.g.,
     * via the utility method {@link TypeUtil#primitiveToWrapper TypeUtil.primitiveToWrapper()}).
     *
     * @param spec {@link TypeTag} meta-annotated annotation specifing validity constraints; must not be null
     * @param value the value to validate
     * @param <T> the type of {@code value}
     * @return the validated value
     * @throws ClassCastException if {@code value} is invalid
     * @throws TypeRestrictionException if {@code value} is invalid due to its value rather than its runtime type
     * @throws NullPointerException if {@code spec} is null
     */
    <T> T validate(A spec, T value);

    /**
     * Determine whether the given value is valid according to the given specification.
     *
     * <p>
     * The implementation in {@link TypeTagValidator} invokes {@link #validate validate()}
     * and returns false if that method throws a {@link ClassCastException}, otherwise true.
     *
     * @param spec {@link TypeTag} meta-annotated annotation specifing validity constraints; must not be null
     * @param value the value to validate
     * @param <T> the type of {@code value}
     * @return true if {@code value} is valid according to {@code spec}, otherwise false
     * @throws NullPointerException if {@code spec} is null
     */
    default <T> boolean isValid(A spec, T value) {
        try {
            this.validate(spec, value);
        } catch (ClassCastException e) {
            return false;
        }
        return true;
    }
}
