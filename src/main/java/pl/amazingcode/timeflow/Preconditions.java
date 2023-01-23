package pl.amazingcode.timeflow;

final class Preconditions {

    private Preconditions() {
    }

    static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
