/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.example;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.dellroad.typetags.core.InvalidValueException;
import org.dellroad.typetags.core.TypeTag;

public class Example {

    // Example of compile time detection
    public static @PhoneNumber String compileWarningTest(String input) {
        return input;                                   // COMPILER error/warning here
    }

    // Example of runtime detection
    public static void main(String[] args) {

        // Parse command line
        if (args.length != 1) {
            System.err.println("Usage: java -jar typetags-example-*-run.jar <phone-number>");
            System.exit(1);
        }
        final String input = args[0];

        // Show input
        System.out.println(String.format("Input string: \"%s\"", input));

        // Try to cast
        final @PhoneNumber String number;
        try {
            number = (@PhoneNumber String)input;        // RUNTIME exception here
            System.out.println("The input is VALID!");
        } catch (InvalidValueException e) {
            System.out.println("The input is INVALID:");
            e.printStackTrace(System.out);
        }
    }
}
