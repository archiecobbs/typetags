/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.example;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.dellroad.typetags.core.TypeRestrictionException;
import org.dellroad.typetags.core.TypeTag;
import org.dellroad.typetags.core.TypeTagValidator;

/**
 * Annotates declarations of type {@link String} for which the value, if non-null, must be valid E.164 phone number.
 *
 * @see <a href="https://en.wikipedia.org/wiki/E.164">E.164</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@TypeTag(restrictTo = String.class, validatedBy = PhoneNumber.Validator.class)
public @interface PhoneNumber {

    /**
     * Validator for the {@link PhoneNumber &#64;PhoneNumber} annotation.
     */
    class Validator implements TypeTagValidator {

        /**
         * Regular expression matching the E.164 format.
         */
        public static final String E164_PATTERN = "\\+[1-9][0-9]{6,14}";

        @Override
        public void validate(Class<? extends Annotation> annotationType, Object value) {

            // Sanity check
            if (annotationType != PhoneNumber.class)
                throw new RuntimeException("unexpected annotation type");

            // We may assume String here because of @TypeTag.restrictTo()
            final String string = (String)value;

            // Check for proper format
            if (string != null && !string.matches(E164_PATTERN))
                throw new TypeRestrictionException("not a valid E.164 phone number");
        }
    }
}
