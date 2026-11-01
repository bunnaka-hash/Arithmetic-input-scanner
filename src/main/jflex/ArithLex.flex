package edu.itc.arithlex;

%%

%public
%class ArithLexScanner
%unicode
%line
%column
%type Object

%%

[0-9]+ {
    System.out.println("INTEGER: " + yytext()
        + " | Line: " + (yyline + 1)
        + " | Column: " + (yycolumn + 1));
}

"+" {
    System.out.println("PLUS: " + yytext()
        + " | Line: " + (yyline + 1)
        + " | Column: " + (yycolumn + 1));
}

[ \t\r\n]+ {
    // Ignore whitespace
}

. {
    System.out.println("UNKNOWN: " + yytext()
        + " | Line: " + (yyline + 1)
        + " | Column: " + (yycolumn + 1));
}