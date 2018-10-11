import java.util.ArrayList;

public class ArgumentException extends IllegalArgumentException {
    private final Operation operation;
    private final ArrayList<Double> numbers;

    public ArgumentException(Operation operation, ArrayList<Double> numbers) {
        this.operation = operation;
        this.numbers = numbers;
    }

    public ArgumentException(String s, Operation operation, ArrayList<Double> numbers) {
        super(s);
        this.operation = operation;
        this.numbers = numbers;
    }

    public ArgumentException(String message, Throwable cause, Operation operation, ArrayList<Double> numbers) {
        super(message, cause);
        this.operation = operation;
        this.numbers = numbers;
    }

    public ArgumentException(Throwable cause, Operation operation, ArrayList<Double> numbers) {
        super(cause);
        this.operation = operation;
        this.numbers = numbers;
    }

    public ArgumentException(String message) {
        super(message);
        this.operation = Operation.NULL;
        this.numbers = new ArrayList<>();
    }

    public Operation getOperation() {
        return operation;
    }

    public ArrayList<Double> getNumbers() {
        return numbers;
    }
}
