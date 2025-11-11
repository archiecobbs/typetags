/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for type annotations that can be validated at compile time and runtime by TypeTags.
 *
 * <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.27.0/prism.min.js"></script>
 * <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.27.0/components/prism-java.min.js"></script>
 * <link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.27.0/themes/prism.min.css" rel="stylesheet"/>
 *
 * <p>
 * The {@link org.dellroad.typetags.checker.TypeTagsChecker} plug-in to the Checker framework recognizes such annotations
 * at compile time and checks for invalid assignments. The TypeTags Runtime Weaver provides support for runtime
 * checking (see below).
 *
 * <p>
 * In the descriptions below, the "target annotation" is the annotation to which this meta-annotation is applied.
 * The target annotation, also known as a "TypeTag", is some annotation that is applied to Java types and which
 * represents the assertion that some predicate is true of any value assigned to the corresponding field, parameter,
 * or variable.
 *
 * <p>
 * The {@link TypeTag @TypeTag} meta-annotation defines how, at runtime, those assertion(s) are verified. For example:
 *
 * <pre><code class="language-java">
 *  &#47;**
 *   * Annotates declarations of type {&#64;link String} for which
 *   * the value, if non-null, is a valid E.164 phone number.
 *   *
 *   * &lt;p&gt;
 *   * Optionally, a North American Numbering Plan (NANP) number
 *   * may be required.
 *   *&#47;
 *  &#64;Retention(RetentionPolicy.RUNTIME)
 *  &#64;Target(ElementType.TYPE_USE)
 *  &#64;TypeTag(restrictTo = String.class, validatedBy = PhoneNumberChecker.class)
 *  public &#64;interface PhoneNumber {
 *
 *      &#47;**
 *       * Determine whether North American Numbering Plan (NANP) numbers are required.
 *       *
 *       * &#64;return true if only NANP numbers are valid
 *       *&#47;
 *      boolean nanpOnly() default false;
 *  }
 *
 *  // Validator for the &#64;PhoneNumber annotation
 *  public static class PhoneNumberChecker implements AnnotationValidator&lt;PhoneNumber&gt; {
 *
 *      // E.164 format
 *      private static final String PATTERN = "\\+[1-9][0-9]{6,14}";
 *
 *      // North American Numbering Plan (NANP) numbers
 *      private static final String NANP_PATTERN = "\\+1[2-9][0-9]{2}[2-9][0-9]{6}";
 *
 *      &#64;Override
 *      public &lt;T&gt; T validate(PhoneNumber spec, T value) {
 *
 *          // Null checks are performed elsewhere, so we always allow null here
 *          if (value == null)
 *              return value;
 *
 *          // Value must be a {@link String}
 *          final String string = (String)value;
 *
 *          // Check for proper format
 *          if (!string.matches(PhoneNumber.PATTERN))
 *              throw new TypeRestrictionException("not a valid E.164 phone number");
 *
 *          // Optionally, require number to be North American
 *          if (spec.nanpOnly() &amp;&amp; !string.matches(PhoneNumber.NANP_PATTERN))
 *              throw new TypeRestrictionException("not a North American phone number");
 *
 *          // OK
 *          return value;
 *      }
 *  }
 * </code></pre>
 *
 * <p>
 * At compile time, the checker verifies the type restriction when it's possible to do so at compile time:
 *
 * <pre><code class="language-java">
 *  String input = scanner.nextLine();
 *  &#64;PhoneNumber String pn = input;     // error: type restriction not satisfied
 * </code></pre>
 *
 * <p>
 * Otherwise, casts and {@code instanceof} checks will be performed at runtime:
 *
 * <pre><code class="language-java">
 *  String input = scanner.nextLine();
 *  if (input instanceof &#64;PhoneNumber String)
 *      &#64;PhoneNumber String pn = (&#64;PhoneNumber String)input2;
 * </code></pre>
 *
 * <p>
 * These runtime checks are added by the TypeTags Runtime Weaver using classfile modification.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface TypeTag {

    /**
     * Restrict valid values to being instances of one the specified types.
     * This property is applied both at compile time and at runtime.
     *
     * <p>
     * If this property is set, then during compilation any value that is not assignable to one of the
     * specified types is considered invalid and will generate an error. At runtime, it is the responsibility
     * of the {@link #validatedBy} class to perform the same check.
     *
     * <p>
     * To not restrict values by type, leave this property set to its default value, i.e., an empty array.
     *
     * @return the supertype(s) of all valid values, or empty for no restriction
     */
    Class<?>[] restrictTo() default { };

    /**
     * Specify the {@link TypeTagValidator} class that validates values at runtime.
     *
     * <p>
     * The specified class must have a static, zero-argument method named {@code getInstance()} that provides
     * an instance of itself. That instance is responsible for validating the {@code value} is an instance of
     * one of the {@link #restrictTo} types (if any), as well as any other requirements specific to the
     * target annotation.
     *
     * @return runtime validator class
     */
    @SuppressWarnings("rawtypes")
    Class<? extends TypeTagValidator/*<?>*/> validatedBy();
}
