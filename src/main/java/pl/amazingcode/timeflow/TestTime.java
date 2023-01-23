package pl.amazingcode.timeflow;

import static java.lang.Thread.sleep;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class TestTime extends Time {

    private static final TestTime TEST_INSTANCE = new TestTime();
    private final List<Consumer<Clock>> observers;
    private volatile Clock originalClock;

    private TestTime() {
        super();
        this.observers = new ArrayList<>();

    }

    public static TestTime testInstance() {
        return TEST_INSTANCE;
    }

    @Override
    public Instant now() {
        return instance().now();
    }

    @Override
    public Clock clock() {
        return instance().clock();
    }

    @Override
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
        var updatedClock = Clock.offset(instance().clock(), duration);
        instance().setClock(updatedClock);
        notifyObservers();
    }

    public synchronized void fastBackward(Duration duration) {
        var updatedClock = Clock.offset(instance().clock(), duration.negated());
        instance().setClock(updatedClock);
        notifyObservers();
    }

    public synchronized void timeFlow(
        Duration step,
        Instant endTime,
        int flowSpeedMillis) {

        Preconditions.checkArgument(flowSpeedMillis > 0, "Flow speed must be positive");
        Preconditions.checkArgument(endTime.isAfter(instance().now()), "End time must be after current time");

        try {
            while (instance().now().isBefore(endTime)) {
                sleep(flowSpeedMillis);
                fastForward(step);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void registerObserver(Consumer<Clock> clockConsumer) {
        observers.add(clockConsumer);
    }

    public synchronized void clearObservers() {
        observers.clear();
    }

    private void notifyObservers() {
        observers.forEach(observer -> observer.accept(instance().clock()));
    }
}
