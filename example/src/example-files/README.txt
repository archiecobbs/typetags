This demonstrates Java runtime checks added by TypeTags.

The example annotation is @PhoneNumber, which annotates the String type to
indicate that the value a valid E.164 phone number.

To run the demo, run these two commands and compare outcomes:

    java -jar typetags-example-*-run.jar +12024567041
    java -jar typetags-example-*-run.jar blah-blah

Project info: https://github.com/archiecobbs/typetags
