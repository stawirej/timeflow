package pl.amazingcode.timeflow;

import java.time.Clock;
import java.time.Instant;

/**
 * Time is a singleton class that provides access to the current time. It is a wrapper around {@link
 * Clock}. It instantiates an inner {@link Clock} with {@link Clock#systemUTC()}. Time is designed
 * to be used in production code.
 */
public sealed class Time permits TestTime {

  private static final Time INSTANCE = new Time();
  private volatile Clock clock;

  protected Time() {
    this.clock = Clock.systemUTC();
  }

  /**
   * Returns the singleton instance of Time.
   *
   * @return the singleton instance of Time
   */
  public static Time instance() {
    return INSTANCE;
  }

  /**
   * Returns the clock. Inner clock is instantiated with {@link Clock#systemUTC()}.
   *
   * @return the clock
   */
  public Clock clock() {
    return this.clock;
  }

  /**
   * Returns the current instant using the clock.
   *
   * @return the current instant
   */
  public Instant now() {
    return clock.instant();
  }

  protected synchronized void setClock(Clock clock) {
    this.clock = clock;
  }
}
