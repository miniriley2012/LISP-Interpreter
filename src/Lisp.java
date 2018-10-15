import java.io.File;
import java.io.IOException;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class Lisp {
    private static StringCharacterIterator iterator;

    private static Map<String, String> variables;
    private static Map<String, Boolean> settings;

    private static boolean shouldPrint;

    private static String nextWord(String delimiters) {
        StringBuilder str = new StringBuilder();

        // HACK These two lines are needed to skip all spaces
        //noinspection StatementWithEmptyBody
        while (iterator.next() == ' ') {
        }
        iterator.previous(); // Backs up 1

        while (delimiters.indexOf(iterator.next()) == -1) {
            str.append(iterator.current());
        }
        iterator.previous(); // Includes delimiter
        return str.toString();
    }

    private static String nextString() {
        StringBuilder str = new StringBuilder();

        while (iterator.next() != iterator.DONE) {
            if (iterator.current() == '\"') {
                if (iterator.previous() == '\\') {
                    iterator.next();
                } else {
                    break;
                }
            }
            str.append(iterator.current());
        }

        return str.toString();
    }

    private static void print(String str, ArrayList<String> list) {
        if (str.equals("\\") || str.equals("$")) {
            return;
        }
        if (str.startsWith("\\")) {
            if (str.charAt(1) == '$') {
                return;
            } else if (str.charAt(1) == '\\') {
                return;
            }
        }
        if (str.startsWith("$")) {
            if (str.charAt(1) == '(') {
                list.remove(list.size() - 1);
                iterator.setIndex(iterator.getIndex() - str.length() + 1);
                iterator.next();
                str = String.valueOf(evaluate());
                list.add(str);
                return;
            } else {
                String variable = str.substring(1);
                list.remove(list.size() - 1);
                if (variables.containsKey(variable)) {
                    str = variables.get(variable);
                } else {
                    iterator.next();
                    printNotDefined(str);
                    System.exit(1);
                }
            }
            list.add(str);
        } else if (str.startsWith("(")) {
            StringBuilder temp = new StringBuilder();
            while (iterator.next() != ')') {
                temp.append(iterator.current());
            }
            temp.append(iterator.current());
            list.add(temp.toString());
        }
    }

    private static void printNotDefined(String str) {
        if (str.charAt(0) == '$') {
            str = str.substring(1);
        }

        iterator.setIndex(iterator.getIndex() - (str.length() - 1));
        throw new ArgumentException(str + " is not defined");
    }

    private static double makeNumber(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            printNotDefined(str);
            System.exit(1);
        }
        return 0;
    }

    private static ArrayList<Double> createList(String command, ArrayList<String> list) {
        var tt = new ArrayList<Double>();
        for (String currentString : list) {
            if (variables.containsKey(currentString)) {
                try {
                    tt.add(Double.valueOf(variables.get(currentString)));
                } catch (NumberFormatException e) {
                    iterator.setIndex(iterator.getIndex() - currentString.length() + 1);
                    throw new ArgumentException("Wrong argument type for " + command);
                }
                continue;
            }
            tt.add(makeNumber(currentString));
        }
        return tt;
    }

    private static Double execute(Group group) {
        Operation op = chooseOperation(group.command);
        var list = group.numbers;

        ExecuteHelpers.CheckNull checkNull = () -> {
            if (list == null || list.isEmpty()) {
                throw new ArgumentException(op.toString() + " requires arguments");
            }
        };

        switch (op) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
                checkNull.checkNull();
                return ExecuteHelpers.accumulate(op, list);

            case ROUND:
                checkNull.checkNull();
                return (double) Math.round(list.get(0));

            case FLOOR:
                checkNull.checkNull();
                return Math.floor(list.get(0));

            case CEIL:
                checkNull.checkNull();
                return Math.ceil(list.get(0));

            case SIN:
                checkNull.checkNull();
                return Math.sin(list.get(0));

            case COS:
                checkNull.checkNull();
                return Math.cos(list.get(0));

            case TAN:
                checkNull.checkNull();
                return Math.tan(list.get(0));

            case HYPOT:
                checkNull.checkNull();
                return Math.hypot(list.get(0), list.get(1));

            case LOG:
                return ExecuteHelpers.log(list);

            case RAND: // TODO add error message if lower bound is greater than upper bound
                return ExecuteHelpers.rand(list);

            case SET:
                shouldPrint = false;
                if (list != null && list.size() == 2) {
                    return list.get(1);
                }
            case READ:
            case PRINT:
            case NULL:
                break;
        }

        shouldPrint = false;
        return 0.0;
    }

    // TODO Break up this function
    private static double evaluate() {
        String command = nextWord(" \n").replaceAll("[()]", "");
        var list = new ArrayList<String>();

        iterator.next();
        for (; iterator.current() != ')' && iterator.current() != iterator.DONE; iterator.next()) {
            String str;

            if (iterator.next() == '\"') {
                str = nextString();
            } else {
                iterator.previous();
                str = nextWord(" )" + iterator.DONE);
            }

            if (str.isEmpty()) {
                if (list.isEmpty() && command.equals("SET")) {
                    throw new ArgumentException(command + " requires arguments");
                } else if (command.equals("READ")) {
                    break;
                } else if (command.equals("PRINT")) {
                    continue;
                }
                return execute(new Group(command, null));
            }

            if (str.charAt(0) == '(' && !command.equals("PRINT")) {
                iterator.setIndex(iterator.getIndex() - str.length() + 1);
                str = String.valueOf(evaluate());
            }

            list.add(str);

            if (command.equals("SET")) {
                iterator.next();
                str = nextWord(" )" + iterator.DONE);

                if (str.isBlank()) {
                    throw new ArgumentException("Missing value to assign \"" + list.get(0) + "\" to");
                }
                if (str.startsWith("\"")) {
                    str = getString(str);
                }
                if (str.charAt(0) == '(') { // TODO Find a way to remove this duplicate code
                    iterator.setIndex(iterator.getIndex() - str.length() + 1);
                    str = String.valueOf(evaluate());
                }
                if (settings.containsKey(list.get(0))) {
                    settings.put(list.get(0), Boolean.valueOf(str));
                    list.remove(0);
                    return execute(new Group(command, null));
                }
                variables.put(list.get(0), variables.getOrDefault(list.get(0), str));
                list.add(str);
                list.remove(0);
                return execute(new Group(command, null));
            }

            if (command.equals("PRINT")) {
                print(str, list);
            }
        }

        if (command.equals("READ")) {
            if (list.isEmpty()) {
                throw new ArgumentException("READ requires arguments");
            }
            Scanner scanner = new Scanner(System.in);
            variables.put(list.get(0), scanner.nextLine());
            return execute(new Group(command, null));
        }
        if (command.equals("PRINT")) {
            System.out.print(String.join("", list)
                    .replaceAll("(\\.0$)", "") // Remove ".0" if it is at the end of the string
                    .replaceAll("(\\.0) ", " ") // Replace the remaining ".0"s if they are within the string
                    .replaceAll("(\\.0)\\\\n", "\\\\n") // Replace the remaining ".0"s if they are before a newline
                    .replaceAll("(?<!\\\\)\\\\n", "\n") // Replace \n but not \\n with newline
                    .replaceAll("\\\\\\B", "")); // Remove first backslash from \\
            return execute(new Group(command, null));
        }

        var numbers = createList(command, list);
        return execute(new Group(command, numbers));
    }

    private static String getString(String str) {
        if (str.startsWith("\"")) {
            if (!str.endsWith("\"")) {
                StringBuilder temp = new StringBuilder(str.replaceFirst("\"", ""));
                for (char d = iterator.next(); d != '\"'; d = iterator.next()) {
                    temp.append(d);
                }
                str = temp.toString();
            }
        }
        return str.replaceAll("\"", "");
    }

    private static Operation chooseOperation(String str) {
        try {
            return Operation.valueOf(str);
        } catch (IllegalArgumentException e) {
            return Operation.NULL;
        }
    }

    private static boolean balanced(String str) {
        var stack = new Stack<Character>();

        for (char c : str.toCharArray()) {
            if (c == '(') {
                stack.push(c);
            } else if (c == ')') {
                if (stack.empty() || stack.pop() != '(') {
                    return false;
                }
            }
        }

        return stack.empty();
    }

    public static void main(String[] args) throws IOException {
        Scanner input = new Scanner(new File("lisp.dat")); // reads in the lisp.dat file

        variables = new HashMap<>();
        variables.put("PI", String.valueOf(Math.PI));
        variables.put("E", String.valueOf(Math.E));

        settings = new HashMap<>();
        settings.put("_PRINT_LINES_", false);
        settings.put("_STORE_ANS_", true);

        for (int currentLine = 1; input.hasNextLine(); ++currentLine) // loops through each data set
        {
            String line = input.nextLine() + '\n';

            if ((line.length() - line.replaceAll("(?<!\\\\)\"", "").length()) % 2 != 0) {
                try {
                    throw new SyntaxException("Quotes not balanced", line, currentLine);
                } catch (SyntaxException e) {
                    e.printError();
                    System.exit(1);
                }
            }

            if (!balanced(line)) {
                try {
                    throw new SyntaxException("Parentheses not balanced", line, currentLine);
                } catch (SyntaxException e) {
                    e.printError();
                    System.exit(1);
                }
            }

            iterator = new StringCharacterIterator(line);

            for (char c = iterator.first(); c != iterator.DONE; c = iterator.next()) {

                if (c == ' ') {
                    c = iterator.next();
                }

                if (c == '#') { // Although Common LISP uses ";;" as comments I will use "#" as it is easier to parse
                    do {
                        iterator.next();
                    } while (iterator.current() != iterator.DONE);
                    continue;
                }

                if (c == '(') {
                    try {
                        if (settings.get("_PRINT_LINES_")) {
                            shouldPrint = true;
                        }
                        Double evaluated = evaluate();
                        if (settings.get("_STORE_ANS_")) {
                            variables.put("ANS", String.valueOf(evaluated));
                        }
                        if (shouldPrint) {
                            System.out.println(String.valueOf(evaluated).replaceAll("(\\.0$)", ""));
                        }
                    } catch (ArgumentException e) {
                        System.err.printf("Argument error at %d:%d: %s%n", currentLine, iterator.getIndex(), e.getLocalizedMessage());
                        System.err.print(line);
                        System.err.print(" ".repeat(iterator.getIndex() - 1) + "^");
                        System.exit(1);
                    }
                }
            }
        }

        input.close();

    } // end of main method

    private static class ExecuteHelpers {
        private static double accumulate(Operation operation, ArrayList<Double> list) {
            double result = list.get(0);
            for (int i = 1; i < list.size(); i++) {
                double n = list.get(i);
                switch (operation) {
                    case ADD:
                        result += n;
                        break;
                    case SUB:
                        result -= n;
                        break;
                    case MUL:
                        result *= n;
                        break;
                    case DIV:
                        result /= n;
                        break;
                    case MOD:
                        result %= n;
                        break;
                }
            }
            return result;
        }

        private static double log(ArrayList<Double> list) {
            //noinspection StatementWithEmptyBody
            if (list == null || list.size() == 0) {
            } else if (list.size() == 1) {
                return Math.log(list.get(0));
            } else if (list.size() == 2) {
                return Math.log(list.get(0)) / Math.log(list.get(1));
            }
            throw new ArgumentException("Invalid arguments for LOG", Operation.LOG, list);
        }

        private static double rand(ArrayList<Double> list) {
            if (list == null || list.size() == 0) {
                return ThreadLocalRandom.current().nextDouble();
            } else if (list.size() == 1) {
                return (double) ThreadLocalRandom.current().nextInt(0, list.get(0).intValue() + 1);
            } else if (list.size() == 2) {
                return (double) ThreadLocalRandom.current().nextInt(list.get(0).intValue(), list.get(1).intValue() + 1);
            }
            throw new ArgumentException("Invalid arguments for RAND", Operation.RAND, list);
        }

        private interface CheckNull {
            void checkNull();
        }
    }

} // end of class
