/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.example;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.dellroad.typetags.core.AnnotationValidator;
import org.dellroad.typetags.core.InvalidValueException;
import org.dellroad.typetags.core.TypeTag;

/**
 * Annotates declarations of type {@link String} for which the value must be non-null and a valid E.164 phone number.
 *
 * <p>
 * Optionally, values may be restricted to only allow North American Numbering Plan (NANP) phone numbers.
 *
 * @see <a href="https://en.wikipedia.org/wiki/E.164">E.164</a>
 * @see <a href="https://en.wikipedia.org/wiki/North_American_Numbering_Plan">North American Numbering Plan</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@TypeTag(restrictTo = String.class, validatedBy = PhoneNumber.Validator.class)
public @interface PhoneNumber {

    /**
     * Determine whether North American Numbering Plan (NANP) numbers are required.
     *
     * @return true if only NANP numbers are valid
     */
    boolean nanpOnly() default false;

    /**
     * Validator for the {@link PhoneNumber &#64;PhoneNumber} annotation.
     */
    public static class Validator implements AnnotationValidator<PhoneNumber> {

        /**
         * Regular expression matching the E.164 format.
         */
        public static final String E164_PATTERN = "\\+[1-9][0-9]{6,14}";

        /**
         * Regular expression matching the North American Numbering Plan (NANP) format.
         */
        public static final String NANP_PATTERN = "\\+1[2-9][0-9]{2}[2-9][0-9]{6}";

        @Override
        public <T> T validate(PhoneNumber spec, T value) {

            // Value must not be null
            if (value == null)
                throw new InvalidValueException("phone number can't be null");

            // We may assume String here because of @TypeTag.restrictTo()
            final String string = (String)value;

            // Check for proper format
            if (!string.matches(E164_PATTERN))
                throw new InvalidValueException("not a valid E.164 phone number");

            // Optionally, require number to be North American
            if (spec.nanpOnly() && !string.matches(NANP_PATTERN))
                throw new InvalidValueException("not a North American phone number");

            // OK
            return value;
        }
    }
}
