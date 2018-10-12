public class SyntaxException extends Exception {
    private final String str;
    private final int line;

    public SyntaxException(String message, String str, int line) {
        super(message);
        this.str = str;
        this.line = line;
    }

    public void printError() {
        System.err.printf("Syntax error on line %d: %s%n", line, getLocalizedMessage());
        System.err.print(str);
//        System.err.print(" ".repeat(position - 1) + "^");
    }
}
