/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.core;

import java.lang.annotation.Annotation;

/**
 * Implemented by classes capable of validating values according to some annotation-based specification.
 *
 * @param <A> the type of the annotation specification
 */
public interface AnnotationValidator<A extends Annotation> {

    /**
     * Validate and return the given value according to the given specification.
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
     * @param spec the annotation specifing validity constraints
     * @param value the value to validate
     * @param <T> the type of {@code value}
     * @return the validated value
     * @throws InvalidValueException if {@code value} is not valid
     * @throws IllegalArgumentException if {@code spec} is null
     */
    <T> T validate(A spec, T value);

    /**
     * Create a {@link Validator} from this instance and the given annotation.
     *
     * @param spec the annotation specifing validity constraints
     * @return corresponding {@link Validator}
     * @throws InvalidValueException if {@code value} is not valid
     * @throws IllegalArgumentException if {@code spec} is null
     */
    default Validator toValidator(A spec) {
        return new Validator() {
            @Override
            public <T> T validate(T value) {
                return AnnotationValidator.this.validate(spec, value);
            }
        };
    }
}
