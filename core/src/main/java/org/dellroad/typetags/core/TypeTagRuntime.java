/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.core;

import java.lang.annotation.Annotation;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Runtime functionality required by the bytecode modifications applied by the {@link org.dellroad.typetags.weaver.TypeTagsWeaver}.
 */
public final class TypeTagRuntime {

    public static final String VALIDATE_METHOD_NAME = "validate";
    public static final String IS_VALID_METHOD_NAME = "isValid";

    private static final Map<Class<? extends Annotation>, TypeTagValidator> VALIDATOR_MAP = new WeakHashMap<>();

    private TypeTagRuntime() {
    }

    /**
     * Create an instance of the {@link TypeTagValidator} class associated with the specified {@link TypeTag &#64;TypeTag}
     * meta-annotated type annotation class.
     *
     * @param annotationType a {@link TypeTag &#64;TypeTag} meta-annotated annotation type
     * @throws IllegalArgumentException if {@code annotationType} is null
     * @throws IllegalArgumentException if {@code annotationType} is not meta-annotated with {@link TypeTag &#64;TypeTag}
     */
    public static synchronized TypeTagValidator validatorFor(Class<? extends Annotation> annotationType) {
        if (annotationType == null)
            throw new IllegalArgumentException("null annotationType");
        return VALIDATOR_MAP.computeIfAbsent(annotationType, TypeTagRuntime::computeValidatorFor);
    }

    private static TypeTagValidator computeValidatorFor(Class<? extends Annotation> annotationType) {
        try {
            final TypeTag typeTag = annotationType.getAnnotation(TypeTag.class);
            if (typeTag == null) {
                throw new IllegalArgumentException(String.format("@%s meta-annotation not found on annotation class %s",
                  TypeTag.class.getSimpleName(), annotationType.getName()));
            }
            final Class<? extends TypeTagValidator> validatorType = typeTag.validatedBy();
            final Constructor<? extends TypeTagValidator> constructor;
            try {
                constructor = validatorType.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(String.format("%s does not have a public default constructor", validatorType), e);
            }
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(String.format(
              "error acquiring %s instance for annotation @%s: %s", TypeTagValidator.class.getSimpleName(),
              annotationType.getName(), Optional.ofNullable(e.getMessage()).orElseGet(e::toString)));
        }
    }

    /**
     * Bootstrap method for weaver validation calls.
     */
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name,
      MethodType type, Class<? extends Annotation> annotationType) throws Throwable {

        // Get/create validator
        final TypeTagValidator validator = TypeTagRuntime.validatorFor(annotationType);

        // Build method handle for validator.validate() and bind the first parameter (annotationType)
        final MethodHandle methodHandle = switch (name) {
            case VALIDATE_METHOD_NAME -> TypeTagRuntime.buildValidateHandle(lookup, validator, annotationType);
            case IS_VALID_METHOD_NAME -> TypeTagRuntime.buildIsValidHandle(lookup, validator, annotationType);
            default -> throw new RuntimeException(String.format("unexpected name \"%s\"", name));
        };
        methodHandle.bindTo(annotationType);

        // Return call site
        return new ConstantCallSite(methodHandle);
    }

    private static MethodHandle buildValidateHandle(MethodHandles.Lookup lookup,
      TypeTagValidator validator, Class<? extends Annotation> annotationType) throws ReflectiveOperationException {
        return lookup.bind(validator, VALIDATE_METHOD_NAME, MethodType.methodType(void.class, Class.class, Object.class))
          .bindTo(annotationType);
    }

    private static MethodHandle buildIsValidHandle(MethodHandles.Lookup lookup,
      TypeTagValidator validator, Class<? extends Annotation> annotationType) throws ReflectiveOperationException {
        return lookup.bind(validator, IS_VALID_METHOD_NAME, MethodType.methodType(boolean.class, Class.class, Object.class))
          .bindTo(annotationType);
    }
}
