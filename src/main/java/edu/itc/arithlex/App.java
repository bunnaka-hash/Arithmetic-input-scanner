package edu.itc.arithlex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class App {

    public static void main(String[] args) throws IOException {

        String input = Files.readString(
                Path.of("input/sample.txt"),
                StandardCharsets.UTF_8
        );

        LexerService lexerService = new LexerService();

        LexerResult result = lexerService.analyze(input);

        System.out.println("LEXICAL ANALYSIS RESULTS");
        System.out.println("----------------------------------------------");

        for (Token token : result.getTokens()) {
            System.out.println(token);
        }

        System.out.println("\nLEXICAL ERRORS");
        System.out.println("----------------------------------------------");

        if (result.getErrors().isEmpty()) {
            System.out.println("No lexical errors found.");
        } else {
            for (LexicalError error : result.getErrors()) {
                System.out.println(error);
            }
        }

        System.out.println("----------------------------------------------");
        System.out.println("Total tokens: " + result.getTokens().size());
        System.out.println("Total errors: " + result.getErrors().size());
    }
}