package example;

public class App {

    public static void main(String[] args) {
        App app = new App();
        app.run(args);
    }

    public String run(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing input");
        }
        return normalize(args[0]);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
