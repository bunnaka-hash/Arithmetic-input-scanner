package edu.itc.arithlex;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class LexerService {

    public LexerResult analyze(String input) throws IOException {

        List<Token> tokens = new ArrayList<>();
        List<LexicalError> errors = new ArrayList<>();

        ArithLexScanner scanner =
                new ArithLexScanner(new StringReader(input));

        Token token;

        while ((token = scanner.yylex()) != null) {

            if (token.getType().equals("UNKNOWN")) {
                errors.add(new LexicalError(
                        token.getLexeme(),
                        token.getLine(),
                        token.getColumn()
                ));
            } else {
                tokens.add(token);
            }
        }

        return new LexerResult(tokens, errors);
    }
}