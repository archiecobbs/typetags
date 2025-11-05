/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.core;

import java.util.HashMap;
import java.util.Optional;

/**
 * Utility methods.
 */
public final class TypeUtil {

    private static final HashMap<Class<?>, Class<?>> PRIMITVE_TO_WRAPPER = new HashMap<>(9);
    private static final HashMap<Class<?>, Class<?>> WRAPPER_TO_PRIMITVE = new HashMap<>(9);

    static {
        PRIMITVE_TO_WRAPPER.put(void.class,     Void.class);
        PRIMITVE_TO_WRAPPER.put(boolean.class,  Boolean.class);
        PRIMITVE_TO_WRAPPER.put(byte.class,     Byte.class);
        PRIMITVE_TO_WRAPPER.put(char.class,     Character.class);
        PRIMITVE_TO_WRAPPER.put(short.class,    Short.class);
        PRIMITVE_TO_WRAPPER.put(int.class,      Integer.class);
        PRIMITVE_TO_WRAPPER.put(float.class,    Float.class);
        PRIMITVE_TO_WRAPPER.put(long.class,     Long.class);
        PRIMITVE_TO_WRAPPER.put(double.class,   Double.class);
        PRIMITVE_TO_WRAPPER.forEach((p, w) -> WRAPPER_TO_PRIMITVE.put(w, p));
    }

    private TypeUtil() {
    }

    /**
     * Get the wrapper type corresponding to the given primitive type.
     *
     * @param type primitive type
     * @return corresponding wrapper type, or {@code type} if {@code type} is not primitive
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> primitiveToWrapper(Class<T> type) {
        return Optional.ofNullable((Class<T>)PRIMITVE_TO_WRAPPER.get(type)).orElse(type);
    }

    /**
     * Get the primitive type corresponding to the given primitive wrapper type.
     *
     * @param type primitive wrapper type
     * @return corresponding primitive type, or {@code type} if {@code type} is not a primitive wrapper type
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> wrapperToPrimitive(Class<T> type) {
        return Optional.ofNullable((Class<T>)WRAPPER_TO_PRIMITVE.get(type)).orElse(type);
    }
}
