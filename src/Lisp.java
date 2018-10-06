import java.io.File;
import java.io.IOException;
import java.text.StringCharacterIterator;
import java.util.*;

import static java.lang.System.out;

enum Operation {
    ADD,
    SUB,
    MUL,
    DIV,
    SET,
    NULL
}

class Lisp {
    // no instance variables are needed

    private static StringCharacterIterator iterator;

    // --Commented out by Inspection (10/6/18, 10:34 AM):private static final Pattern pattern = Pattern.compile("\\(.?(ADD|SUB|MUL|DIV).(.*\\d*).*.*\\)");

// --Commented out by Inspection START (10/6/18, 10:32 AM):
//    private static boolean checkValid(String line) {
//        return pattern.matcher(line).find();
//    }
// --Commented ou   t by Inspection STOP (10/6/18, 10:32 AM)

    private static int currentLine;

    private static Map<String, Double> variables;

// --Commented out by Inspection START (10/6/18, 10:32 AM):
//    private static Group makeGroup(String line) {
//        Matcher matcher = pattern.matcher(line);
//
//        if (matcher.matches()) {
//            String numbers = matcher.group(2);
//            var thing = new ArrayList<>(Arrays.asList(numbers.split("\\s")));
//            return new Group(matcher.group(1), createList(thing));
//        }
//
//        return null;
//    }
// --Commented out by Inspection STOP (10/6/18, 10:32 AM)

    private static String nextWord(String delimiter) {
        StringBuilder str = new StringBuilder();
        char c;
        while (delimiter.indexOf((c = iterator.next())) == -1) {
            str.append(c);
        }
        iterator.previous(); // Includes delimiter
        return str.toString();
    }

    private static double makeNumber(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            int firstChar = (iterator.getIndex() - str.length());
            System.out.printf("Error at %d:%s%n\"%s\" is not defined%n", currentLine, firstChar, str);
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

    private static double execute(Group group) {
        double result = 0;

        Operation op = chooseOperation(group.command);
        var list = group.numbers;

        switch (op) {
            case ADD:
                for (var n : list) {
                    result += n;
                }
                break;

            case SUB:
                result = list.get(0);
                for (int i = 1; list.size() > i; i++) {
                    result -= list.get(i);
                }
                break;

            case MUL:
                result = 1;
                for (Double aList : list) {
                    result *= aList;
                }
                break;

            case DIV:
                result = list.get(0);
                for (int i = 1; list.size() > i; i++) {
                    result /= list.get(i);
                }
                break;

            case SET:
                return 0;
        }

//        out.println("Result of: " + op);
//        out.println("Result: " + result);
        return result;
    }

    private static Operation chooseOperation(String str) {
        switch (str) {
            case "ADD":
                return Operation.ADD;
            case "SUB":
                return Operation.SUB;
            case "MUL":
                return Operation.MUL;
            case "DIV":
                return Operation.DIV;
            case "SET":
                return Operation.SET;
        }
        return Operation.NULL;
    }

    private static double evaluate() {
        String command = nextWord(" ");
        ArrayList<String> list = new ArrayList<>();
        boolean setVar = false;

        iterator.next();
        for (char c = iterator.current(); c != ')'; c = iterator.next()) {
            String str = nextWord(" )");
            if (str.charAt(0) == '(') {
                iterator.setIndex(iterator.getIndex() - str.length() + 1);
                str = String.valueOf(evaluate());
            }
            list.add(str);
            if (command.equals("SET") && !setVar) {
                iterator.next();
                str = nextWord(" )");
                if (str.charAt(0) == '(') { // TODO Find a way to remove this duplicate code
                    iterator.setIndex(iterator.getIndex() - str.length() + 1);
                    str = String.valueOf(evaluate());
                }
                variables.put(list.get(0), makeNumber(str));
                list.remove(0);
                setVar = true;
            }
        }


        var numbers = createList(list);
        return execute(new Group(command, numbers));
    }

    public static void main(String[] args) throws IOException {

        // print out your name


        out.println("Riley Quinn");

        Scanner input = new Scanner(new File("lisp.dat")); // reads in the lisp.dat file

        variables = new HashMap<>();
        for (currentLine = 0; input.hasNextLine(); currentLine++) // loops through each data set
        {

            // HINT: Read in an entire line as a String
            //       Strip off the ( and ) parenthesis
            //       Then create a Scanner object and pass it your String
            //       Use scan.next() to read in the command
            //       Finally, use a while (scan.hasNextInt()) to get the numbers
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
                    System.out.println(String.valueOf(evaluate()).replaceAll("[\\[\\],]|(\\.0)", ""));
                }
            }
        }

        input.close();

    } // end of main method

} // end of class
