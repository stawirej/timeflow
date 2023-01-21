package pl.amazingcode.time;

final class Preconditions {

    private Preconditions() {
    }

    static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
