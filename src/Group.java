import java.util.ArrayList;
import java.util.regex.Pattern;

public class Group {
    public final String command;
    public final ArrayList<Double> numbers;
    // --Commented out by Inspection (10/6/18, 10:34 AM):private static final Pattern pattern = Pattern.compile("\\(.?(ADD|SUB|MUL|DIV).(.*\\d*).*.*\\)");

    public Group(String command, ArrayList<Double> numbers) {
        this.command = command;
        this.numbers = numbers;
    }

// --Commented out by Inspection START (10/6/18, 10:32 AM):
//    public static Group makeGroup(String line) {
//        Matcher matcher = pattern.matcher(line);
//
//        if (matcher.matches()) {
//            String numbers = matcher.group(2);
//            var thing = new ArrayList<>(Arrays.asList(numbers.split("\\s")));
//            return new Group(matcher.group(1), Lisp_RegEx.createList(thing));
//        }
//
//        return null;
//    }
// --Commented out by Inspection STOP (10/6/18, 10:32 AM)

    @Override
    public String toString() {
        return "(" + command + " " + numbers.toString().replaceAll("[\\[\\],]|(\\.0)", "") + ")";
    }
}
