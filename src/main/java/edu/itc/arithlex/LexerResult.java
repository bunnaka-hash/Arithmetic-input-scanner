package edu.itc.arithlex;

import java.util.List;

public class LexerResult {

    private final List<Token> tokens;
    private final List<LexicalError> errors;

    public LexerResult(List<Token> tokens, List<LexicalError> errors) {
        this.tokens = tokens;
        this.errors = errors;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public List<LexicalError> getErrors() {
        return errors;
    }
}