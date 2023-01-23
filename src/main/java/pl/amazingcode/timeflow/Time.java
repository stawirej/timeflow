package pl.amazingcode.timeflow;

import java.time.Clock;
import java.time.Instant;

public sealed class Time permits TestTime {

    private static final Time INSTANCE = new Time();
    private volatile Clock clock;

    protected Time() {
        this.clock = Clock.systemUTC();
    }

    public static Time instance() {
        return INSTANCE;
    }

    public Clock clock() {
        return this.clock;
    }

    public Instant now() {
        return Instant.now(clock);
    }

    protected synchronized void setClock(Clock clock) {
        this.clock = clock;
    }
}
