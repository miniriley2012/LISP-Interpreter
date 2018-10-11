import java.io.File;
import java.io.IOException;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

class Lisp {
    private static StringCharacterIterator iterator;

    private static Map<String, Double> variables;
    private static Map<String, Boolean> settings;

    private static boolean shouldPrint;

    private static String nextWord(String delimiters) {
        StringBuilder str = new StringBuilder();
        char c;

        // HACK These two lines are needed to skip all spaces
        //noinspection StatementWithEmptyBody
        while (iterator.next() == ' ') {
        }
        iterator.previous(); // Backs up 1

        while (delimiters.indexOf((c = iterator.next())) == -1) {
            str.append(c);
        }
        iterator.previous(); // Includes delimiter
        return str.toString();
    }

    private static void print(String str, ArrayList<String> list) {
        if (str.equals("\\") || str.equals("$")) {
            return;
        }
        if (str.charAt(0) == '\\') {
            if (str.charAt(1) == '$') {
                return;
            } else if (str.charAt(1) == '\\') {
                return;
            }
        }
        if (str.charAt(0) == '$') {
            if (str.charAt(1) == '(') { // TODO Find a way to remove this duplicate code
                list.remove(list.size() - 1);
                iterator.setIndex(iterator.getIndex() - str.length() + 1);
                iterator.next();
                str = String.valueOf(evaluate());
            } else {
                String variable = str.substring(1);
                list.remove(list.size() - 1);
                if (variables.containsKey(variable)) {
                    str = variables.get(variable).toString();
                } else {
                    iterator.next();
                    printNotDefined(str);
                    System.exit(1);
                }
            }
            list.add(str);
        } else if (str.charAt(0) == '(') {
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

    private static ArrayList<Double> createList(ArrayList<String> list) {
        var tt = new ArrayList<Double>();
        for (String currentString : list) {
            if (variables.containsKey(currentString)) {
                tt.add(variables.get(currentString));
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
        boolean variableSet = false;
        var list = new ArrayList<String>();

        iterator.next();
        for (char c = iterator.current(); c != ')' && c != iterator.DONE; c = iterator.next()) {
            String str = nextWord(" )" + iterator.DONE);
            if (str.isEmpty()) {
                if (list.isEmpty() && command.equals("SET")) {
                    throw new ArgumentException(command + " requires arguments");
                }
                if (command.equals("READ")) {
                    break;
                }
                return execute(new Group(command, null));
            }
            if (str.charAt(0) == '(' && !command.equals("PRINT")) {
                iterator.setIndex(iterator.getIndex() - str.length() + 1);
                str = String.valueOf(evaluate());
            }

            list.add(str);

            if (command.equals("SET") && !variableSet) {
                iterator.next();
                str = nextWord(" )" + iterator.DONE);

                if (str.isBlank()) {
                    throw new ArgumentException("Missing value to assign \"" + list.get(0) + "\" to");
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
                variables.put(list.get(0), variables.containsKey(str) ? variables.get(str) : makeNumber(str));
                list.add(str);
                list.remove(0);
                variableSet = true;
            }

            if (command.equals("PRINT")) {
                print(str, list);
            }

        }
        if (command.equals("READ")) {
            Scanner scanner = new Scanner(System.in);
            System.out.print(list.toString().replaceAll("[\\[\\],]", ""));
            //noinspection ConstantConditions
            return scanner.nextDouble();
        }
        if (command.equals("PRINT")) {
            System.out.println(list.toString()
                    .replaceAll("[\\[\\],]", "") // Remove braces and commas from list
                    .replaceAll("(\\.0$)", "") // Remove ".0" if it is at the end of the string
                    .replaceAll("(?<!\\\\)\\\\n", "\n") // Replace \n but not \\n with newline
                    .replaceAll("\\\\\\B", "")); // Remove first backslash from \\
            return execute(new Group(command, null));
        }

        var numbers = createList(list);
        return execute(new Group(command, numbers));
    }

    private static Operation chooseOperation(String str) {
        try {
            return Operation.valueOf(str);
        } catch (IllegalArgumentException e) {
            return Operation.NULL;
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner input = new Scanner(new File("lisp.dat")); // reads in the lisp.dat file

        variables = new HashMap<>();
        variables.put("PI", Math.PI);
        variables.put("E", Math.E);

        settings = new HashMap<>();
        settings.put("_PRINT_LINES_", false);
        settings.put("_STORE_ANS_", true);

        for (int currentLine = 1; input.hasNextLine(); ++currentLine) // loops through each data set
        {
            String line = input.nextLine() + '\n';

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
                            variables.put("ANS", evaluated);
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
