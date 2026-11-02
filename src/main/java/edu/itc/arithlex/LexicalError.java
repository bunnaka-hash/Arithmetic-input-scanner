package edu.itc.arithlex;

public class LexicalError {

    private final String lexeme;
    private final int line;
    private final int column;

    public LexicalError(String lexeme, int line, int column) {
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return "Unexpected character '" + lexeme
                + "' at Line " + line
                + ", Column " + column;
    }
}