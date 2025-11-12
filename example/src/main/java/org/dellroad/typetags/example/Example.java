/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.example;

public class Example {

    private final @PhoneNumber String string;

    // Example of compile time detection
    public Example(String input) {
        this.string = input;                                // COMPILER warning here
    }

    // Example of casting with type annotation
    public static void castToPhoneNumber(String input) {
        String phoneNumber = (@PhoneNumber String)input;    // RUNTIME exception here
    }

    // Example of 'instanceof' with type annotation
    public static boolean isPhoneNumber(String input) {
        return input instanceof @PhoneNumber String;        // RUNTIME validity test here
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

        // Test instanceof
        final boolean itest = Example.isPhoneNumber(input);
        System.out.println(String.format("Test: instanceof @PhoneNumber String: %s", itest));

        // Test cast
        System.out.print("Test: cast to @PhoneNumber String: ");
        try {
            Example.castToPhoneNumber(input);
            System.out.println("OK");
        } catch (ClassCastException e) {
            System.out.println("FAILED");
            e.printStackTrace(System.out);
        }
    }
}
