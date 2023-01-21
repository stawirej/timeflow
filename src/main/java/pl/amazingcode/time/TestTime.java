package pl.amazingcode.time;

import static pl.amazingcode.time.Preconditions.checkArgument;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class TestTime extends Time {

    private static final TestTime TEST_INSTANCE = new TestTime();
    private volatile Clock originalClock;

    public static TestTime testInstance() {
        return TEST_INSTANCE;
    }

    public synchronized void setClock(Clock clock) {
        if (originalClock == null) {
            originalClock = clock();
        }
        instance().setClock(clock);
    }

    public synchronized void resetClock() {
        if (originalClock != null) {
            instance().setClock(originalClock);
            originalClock = null;
        }
    }

    public synchronized void fastForward(Duration duration) {
        instance().setClock(Clock.offset(instance().clock(), duration));
    }

    public synchronized void fastBackward(Duration duration) {
        instance().setClock(Clock.offset(instance().clock(), duration.negated()));
    }

    public synchronized void timeFlow(
        Duration step,
        Instant endTime,
        int flowSpeedMillis) {

        checkArgument(flowSpeedMillis > 0, "Flow speed must be positive");
        checkArgument(endTime.isAfter(instance().now()), "End time must be after current time");

        try {
            while (instance().now().isBefore(endTime)) {
                Thread.sleep(flowSpeedMillis);
                fastForward(step);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
