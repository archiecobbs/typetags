/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.core;

import java.lang.annotation.Annotation;

/**
 * Implemented by classes capable of runtime validation of values whose types are annotated with
 * a {@link TypeTag &#64;TypeTag} meta-annotated annotation.
 *
 * <p>
 * <b>Note:</b> Implementations of this interface must have a public default constructor.
 */
public interface TypeTagValidator {

    /**
     * Validate the given value according to the given {@link TypeTag &#64;TypeTag} annotation type.
     *
     * <p>
     * Invalid values trigger a {@link ClassCastException} in order to remain "backward compatible" with
     * normal Java casts. However, if the value is invalid because of its value rather its runtime type,
     * it is preferrable to throw the more specific {@link TypeRestrictionException}.
     *
     * <p>
     * Whether this method admits null values is up to the implementation, but typically implementations
     * would always allow null values, since those can be checked separately (e.g., via {@code @NonNull}).
     *
     * <p><b>Checking {@link TypeTag#restrictTo &#64;TypeTag.restrictTo()} at Runtime</b>
     *
     * <p>
     * If {@link TypeTag#restrictTo &#64;TypeTag.restrictTo()} is non-empty, this method is responsible for validating
     * that {@code value} is an instance of one of the specified types. Since throwing {@link ClassCastException} is
     * how this method reports an invalid value, in many cases this check happens automatically.
     *
     * <p>
     * Note also that primitive types in {@link TypeTag#restrictTo &#64;TypeTag.restrictTo()} must be handled specially:
     * at runtime, primitive values will be wrapped by the time this method sees them, so this method may need to map any
     * primitive types in {@link TypeTag#restrictTo &#64;TypeTag.restrictTo()} to their corresponding wrapper types (e.g.,
     * via the utility method {@link TypeUtil#primitiveToWrapper TypeUtil.primitiveToWrapper()}).
     *
     * @param annotationType {@link TypeTag &#64;TypeTag} meta-annotated annotation type; must not be null
     * @param value the value to validate
     * @throws ClassCastException if {@code value} is invalid
     * @throws TypeRestrictionException if {@code value} is invalid due to its value rather than its runtime type
     * @throws NullPointerException if {@code annotationType} is null
     */
    void validate(Class<? extends Annotation> annotationType, Object value);

    /**
     * Determine whether the given value is valid according to the given specification.
     *
     * <p>
     * The implementation in {@link TypeTagValidator} invokes {@link #validate validate()}
     * and returns false if that method throws a {@link ClassCastException}, otherwise true.
     *
     * @param annotationType {@link TypeTag &#64;TypeTag} meta-annotated annotation type; must not be null
     * @param value the value to validate
     * @return true if {@code value} is valid for {@code annotationType}, otherwise false
     * @throws NullPointerException if {@code annotationType} is null
     */
    default boolean isValid(Class<? extends Annotation> annotationType, Object value) {
        try {
            this.validate(annotationType, value);
        } catch (ClassCastException e) {
            return false;
        }
        return true;
    }
}
