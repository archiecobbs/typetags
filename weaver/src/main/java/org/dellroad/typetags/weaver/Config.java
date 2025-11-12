/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.weaver;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import org.dellroad.typetags.core.TypeTag;

/**
 * Configuration object for {@link TypeTagsWeaver} instances.
 *
 * <p>
 * New instances are created by obtaining a new {@link Config.Builder} via {@link #newBuilder},
 * configuring the builder, and then invoking {@link Config.Builder#build}.
 *
 * @param loader the {@link ClassLoader} used to find classes
 * @param filter optional filter that excludes some type annotations
 */
public record Config(
    ClassLoader loader,
    Predicate<? super Class<? extends Annotation>> filter) {

    private Config(Builder builder) {
        this(builder.loader, builder.filter);
    }

    /**
     * Create a new {@link Builder}.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

// Builder

    /**
     * Builder for {@link Config} instances.
     *
     * <p>
     * Instances are thread safe.
     */
    public static class Builder {

        private ClassLoader loader;
        private Predicate<? super Class<? extends Annotation>> filter;

        Builder() {
        }

        /**
         * Build the {@link Config}.
         *
         * @return new {@link Config}
         */
        public synchronized Config build() {
            return new Config(this);
        }

        /**
         * Specify a class loader for resolving classes.
         *
         * @param loader class loader, or null for system default
         */
        public synchronized Builder loader(final ClassLoader loader) {
            this.loader = loader;
            return this;
        }

        /**
         * Specify a filter that determines which {@link TypeTag &#64;TypeTag} meta-annotated type annotations are included.
         *
         * <p>
         * The default is to include all type annotations with a {@link TypeTag &#64;TypeTag} meta-annotation.
         *
         * @param filter filter that excludes unwanted type annotations
         */
        public synchronized Builder filter(final Predicate<? super Class<? extends Annotation>> filter) {
            this.filter = filter;
            return this;
        }
    }
}
