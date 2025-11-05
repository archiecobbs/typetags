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
 * The {@link org.dellroad.typetags.checker.TypeTagChecker} plug-in to the Checker framework recognizes such annotations
 * at compile time and checks for invalid assignments. The {@link TypeTags} utility class provides support for runtime
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
 *   * the value must be non-null and a valid E.164 phone number.
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
 *          // Value must not be null
 *          if (value == null)
 *              throw new InvalidValueException("phone number can't be null");
 *
 *          // We may assume String here because of &#64;TypeTag.restrictTo()
 *          final String string = (String)value;
 *
 *          // Check for proper format
 *          if (!string.matches(PhoneNumber.PATTERN))
 *              throw new InvalidValueException("not a valid E.164 phone number");
 *
 *          // Optionally, require number to be North American
 *          if (spec.nanpOnly() &amp;&amp; !string.matches(PhoneNumber.NANP_PATTERN))
 *              throw new InvalidValueException("not a North American phone number");
 *
 *          // OK
 *          return value;
 *      }
 *  }
 * </code></pre>
 *
 * <p>
 * At compile time, the checker verifies the type restriction:
 *
 * <pre><code class="language-java">
 *  String input = scanner.nextLine();
 *  &#64;PhoneNumber String pn = input;     // error: type restriction not satisfied
 * </code></pre>
 *
 * <p>
 * At runtime, the annotation can be supplied to {@link TypeTags#getValidator TypeTags.getValidator()} to build
 * a runtime validator. Unfortunately, this requires a bit of reflection gymnastics:
 *
 * <pre><code class="language-java">
 *  &#47;**
 *   * Verify that the string is valid E.164 phone number.
 *   *
 *   * &#64;param string the value to validate
 *   * &#64;return string
 *   * &#64;throws InvalidValueException if string is invalid
 *   *&#47;
 *  public &#64;PhoneNumber String validatePhoneNumber(String string) {
 *
 *      // Get the annotation (in a real application this would be cached)
 *      PhoneNumber spec;
 *      try {
 *          spec = (PhoneNumber)this.getClass()
 *            .getMethod("validatePhoneNumber", String.class)
 *            .getAnnotatedReturnType()
 *            .getAnnotations()[0];
 *      } catch (ReflectiveOperationException e) {
 *          throw new RuntimeException("unexpected error", e);
 *      }
 *
 *      // Build a Validator from the annotation and validate the value
 *      return TypeTags.getValidator(spec).apply(string);
 *  }
 * </code></pre>
 *
 * <p>
 * Now we can ensure both compile-time and runtime correctness as follows:
 *
 * <pre><code class="language-java">
 *  String input = scanner.nextLine();
 *  &#64;PhoneNumber String pn = validatePhoneNumber(input);    // ok! we're safe
 * </code></pre>
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface TypeTag {

    /**
     * Restrict valid values to being instances of one the specified types.
     *
     * <p>
     * If this property is set, then any value that is not assignable to one of the specified types
     * is considered invalid. During compilation, annotating a type that can't possibly overlap with
     * one of the specified types will cause an error; at runtime, values' actual types should be checked
     * and any that don't match immediately rejected (by throwing {@link InvalidValueException}) and not
     * passed to the validator specified by {@link #validatedBy}, if any.
     *
     * <p>
     * Primitive types are special: at compile time, they are distinct from their corresponding wrapper
     * types. For example, if {@code restrictTo = int.class}, then the target annotation may be applied to type
     * {@code int} but not type {@code Integer}. At runtime, primitive types and their corresponding wrapper types
     * are not distinguished because by the time they are checked, primitive values are always wrapped. So
     * for example, an {@link Integer} value would be permitted by {@code restrictTo = int.class}, and an
     * {@link int} value would be permitted by {@code restrictTo = Integer.class}.
     *
     * <p>
     * To not restrict values by type, leave this property set to its default value (i.e., empty).
     *
     * @return the supertype(s) of all valid values, or empty for no restriction
     */
    Class<?>[] restrictTo() default { };

    /**
     * Restrict valid values to those that are accepted by an instance of the specified class.
     *
     * <p>
     * The class must have a public default constructor.
     *
     * <p>
     * This property is only used at runtime, and only for values that have satisfied the type constraints
     * imposed by {@link #restrictTo}, if any.
     *
     * <p>
     * If no additional validation is required beyond {@link #restrictTo}, you can leave this property
     * set to its default value. That can be useful in situations where you want to restrict values
     * by type only, e.g., "Must be either a {@code String} or a {@code byte[]}".
     *
     * @return annotation-based validation class
     */
    @SuppressWarnings("rawtypes")
    Class<? extends AnnotationValidator/*<?>*/> validatedBy() default AnnotationValidator.class;
}
