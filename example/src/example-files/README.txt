This demonstrates a Java *runtime* check added by TypeTags.

Our example annotation is @PhoneNumber which annotates the String type to verify that the value is non-null and a valid E.164 phone number.

To run the demo, run this command:

    java -jar typetags-example-*-run.jar NUMBER

where NUMBER is any string. If NUMBER is an invalid E.164 phone number, an InvalidValueException should be thrown.

Project info: https://github.com/archiecobbs/typetags
