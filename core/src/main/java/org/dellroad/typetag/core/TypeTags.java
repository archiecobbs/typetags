/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetag.core;

import java.util.WeakHashMap;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * TypeTags utility methods.
 */
public class TypeTags {

    private static final WeakHashMap<Annotation, Validator> VALIDATOR_MAP = new WeakHashMap<>();

    private TypeTags() {
    }

    /**
     * Build a validator that validates values according to the {@link TypeTag @TypeTag}-meta-annotated annotations
     * on the given method's return type.
     *
     * <p>
     * This is a convenience method for the common use case where a validation method wants to obtain the
     * TypeTag's on its own return type.
     *
     * @param method the method whose return type should be inspected
     * @return a validator that throws {@link InvalidValueException} when invalid values are encountered
     * @throws IllegalArgumentException if {@code method} has zero {@link TypeTag @TypeTag} meta-annotated annotations
     * @throws IllegalArgumentException if {@code method} is null
     */
    public static Validator getValidatorFromReturnType(Method method) {
        if (method == null)
            throw new IllegalArgumentException("null method");
        final AnnotatedType returnType = method.getAnnotatedReturnType();
        return Stream.of(returnType.getAnnotations())
          .filter(spec -> spec.annotationType().isAnnotationPresent(TypeTag.class))
          .map(TypeTags::getValidator)
          .reduce(Validator::andThen)
          .orElseThrow(() -> new IllegalArgumentException(String.format(
            "no @%s meta-annotated annotations found on method return type %s", TypeTag.class.getSimpleName(), returnType)));
    }

    /**
     * Build a validator that validates values according to the given {@link TypeTag @TypeTag}-meta-annotated annotation.
     *
     * @param spec the TypeTag type annotation
     * @return a validator that throws {@link InvalidValueException} when invalid values are encountered
     * @throws IllegalArgumentException if {@code typeTag} does not have an {@link TypeTag @TypeTag} meta-annotation
     * @throws IllegalArgumentException if {@code typeTag} is null
     */
    public static Validator getValidator(Annotation spec) {
        synchronized (VALIDATOR_MAP) {
            return VALIDATOR_MAP.computeIfAbsent(spec, TypeTags::buildValidator);
        }
    }

    @SuppressWarnings("unchecked")
    private static <A extends Annotation> Validator buildValidator(A spec) {

        // Santiy check
        if (spec == null)
            throw new IllegalArgumentException("null spec");

        // Find the @TypeTag meta-annotation on the given annotation class
        final Class<? extends Annotation> annotationType = (Class<A>)spec.annotationType();
        final TypeTag tagType = annotationType.getDeclaredAnnotation(TypeTag.class);
        if (tagType == null) {
            throw new IllegalArgumentException(String.format(
              "no @%s meta-annotation found on annotation type %s", TypeTag.class.getSimpleName(), annotationType.getName()));
        }

        // Build the type checking validator, if any
        final Class<?> appliesTo = tagType.appliesTo();
        if (appliesTo.isPrimitive()) {
            throw new IllegalArgumentException(String.format(
              "@%s.appliesTo() specified for annotation type %s must not be primitive (found %s)",
              TypeTag.class.getSimpleName(), annotationType.getName(), appliesTo.getName()));
        }
        final Validator appliesToValidator = appliesTo == Object.class ? null : Validator.checkingType(appliesTo);

        // Build the custom validator, if any
        final Class<? extends AnnotationValidator<A>> validatedBy = (Class<? extends AnnotationValidator<A>>)tagType.validatedBy();
        final Validator validatedByValidator = (Class<?>)validatedBy == AnnotationValidator.class ?
          null : TypeTags.buildCustomValidator(validatedBy, spec);

        // Build the combined validator
        return Stream.of(appliesToValidator, validatedByValidator)
          .filter(Objects::nonNull)
          .reduce(Validator::andThen)
          .orElseGet(Validator::alwaysValid);
    }

    private static <A extends Annotation> Validator buildCustomValidator(
      Class<? extends AnnotationValidator<A>> annotationValidatorType, A spec) {
        final AnnotationValidator<A> annotationValidator;
        try {
            annotationValidator = annotationValidatorType.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(String.format(
              "error instantiating @%s.validatedBy() class %s for annotation type %s",
              TypeTag.class.getSimpleName(), annotationValidatorType.getName(), spec.annotationType().getName()), e);
        }
        return annotationValidator.toValidator(spec);
    }
}
