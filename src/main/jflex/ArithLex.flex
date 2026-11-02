package edu.itc.arithlex;

%%

%public
%class ArithLexScanner
%unicode
%line
%column
%type Token

DIGIT = [0-9]
LETTER = [a-zA-Z_]

IDENTIFIER = {LETTER}({LETTER}|{DIGIT})*
INTEGER = {DIGIT}+
DECIMAL = {DIGIT}+"."{DIGIT}+

%%

{DECIMAL} {
    return new Token(
        "DECIMAL", yytext(), yyline + 1, yycolumn + 1
    );
}

{INTEGER} {
    return new Token(
        "INTEGER", yytext(), yyline + 1, yycolumn + 1
    );
}

{IDENTIFIER} {
    return new Token(
        "IDENTIFIER", yytext(), yyline + 1, yycolumn + 1
    );
}

">=" | "<=" | "==" | "!=" | ">" | "<" {
    return new Token(
        "COMPARISON_OP", yytext(), yyline + 1, yycolumn + 1
    );
}

"=" {
    return new Token(
        "ASSIGN", yytext(), yyline + 1, yycolumn + 1
    );
}

"+" | "-" | "*" | "/" {
    return new Token(
        "ARITHMETIC_OP", yytext(), yyline + 1, yycolumn + 1
    );
}

"(" | ")" {
    return new Token(
        "PARENTHESIS", yytext(), yyline + 1, yycolumn + 1
    );
}

";" {
    return new Token(
        "SEMICOLON", yytext(), yyline + 1, yycolumn + 1
    );
}

[ \t\r\n]+ {
    // Ignore whitespace
}

. {
    return new Token(
        "UNKNOWN", yytext(), yyline + 1, yycolumn + 1
    );
}