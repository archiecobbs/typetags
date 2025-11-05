/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.core;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implemented by classes capable of validating values.
 */
public interface Validator {

    /**
     * Validate and return the given value.
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
     * @param value the value to validate
     * @param <T> the type of {@code value}
     * @return the validated value
     * @throws InvalidValueException if {@code value} is not valid
     */
    <T> T validate(T value);

    /**
     * Compose this and the given {@link Validator}.
     *
     * <p>
     * This will return a validator that applies this validator first, then followed by {@code next}.
     *
     * @param next validator to apply after this validator
     * @return combined validator
     * @throws IllegalArgumentException if {@code next} is null
     */
    default Validator andThen(Validator next) {
        final Validator prev = this;
        return new Validator() {
            @Override
            public <T> T validate(T value) {
                return next.validate(prev.validate(value));
            }
        };
    }

    /**
     * Obtain a "do nothing" validator, i.e., one that always returns its given value.
     *
     * @return an always-accepting validator
     */
    public static Validator alwaysValid() {
        return new Validator() {
            @Override
            public <T> T validate(T value) {
                return value;
            }
        };
    }

    /**
     * Build a validator that verifies that values are assignable to one of the given types.
     *
     * @param allowedTypes allowed types for values; must not be primitive
     * @return validator that verifies values are instances of one of the {@code allowedTypes}
     * @throws IllegalArgumentException if {@code allowedTypes} is null, empty, or contains a null value
     */
    public static Validator restrictTo(Class<?>[] allowedTypes) {
        if (allowedTypes == null)
            throw new IllegalArgumentException("null array");
        if (allowedTypes.length == 0)
            throw new IllegalArgumentException("empty array");
        if (Stream.of(allowedTypes).anyMatch(Objects::isNull))
            throw new IllegalArgumentException("array contains a null type");
        return new Validator() {
            @Override
            public <T> T validate(T value) {
                if (Stream.of(allowedTypes).map(TypeUtil::primitiveToWrapper).anyMatch(t -> t.isInstance(value)))
                    return value;
                final String allowed = allowedTypes.length == 1 ? allowedTypes[0].getName() :
                  "one of " + Stream.of(allowedTypes).map(Class::getName).collect(Collectors.joining(", "));
                throw new InvalidValueException(String.format("value is required to be %s but %s",
                  allowed, value != null ? "has type " + value.getClass().getName() : "is null"));
            }
        };
    }
}
