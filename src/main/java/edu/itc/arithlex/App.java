package edu.itc.arithlex;

import java.io.StringReader;
import java.io.IOException;

public class App {

    public static void main(String[] args) throws IOException {

        String input = "total = 12 + 5;";

        System.out.println("Input: " + input);
        System.out.println("\nLexical Analysis Results:");

        ArithLexScanner scanner =
                new ArithLexScanner(new StringReader(input));

        scanner.yylex();
    }
}