package pl.amazingcode.timeflow;

import static java.lang.Thread.sleep;
import static pl.amazingcode.timeflow.Preconditions.checkArgument;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * TestTime is a singleton class that provides access to the current time and allows to manipulate
 * it both in production and test code. It is an extension of {@link Time}. TestTime is designed to
 * be used in tests only.
 */
public final class TestTime extends Time {

  private static final TestTime TEST_INSTANCE = new TestTime();
  private final List<Consumer<Clock>> observers;

  private TestTime() {
    super();
    this.observers = new ArrayList<>();
  }

  /**
   * Returns the singleton instance of TestTime.
   *
   * @return the singleton instance of TestTime
   */
  public static TestTime testInstance() {
    return TEST_INSTANCE;
  }

  /** {@inheritDoc} */
  @Override
  public Instant now() {
    return instance().now();
  }

  /** {@inheritDoc} */
  @Override
  public Clock clock() {
    return instance().clock();
  }

  /**
   * Replace the clock used by {@link Time} and {@link TestTime} with a custom one.
   *
   * <pre>
   *    {@code // Prepare a fixed clock}
   *    {@code ZoneId zoneId = TimeZone.getTimeZone("Europe/Warsaw").toZoneId();}
   *    {@code ZonedDateTime dateTime = LocalDateTime.of(2001, 11, 23, 12, 15).atZone(zoneId);}
   *    {@code Clock fixedClock = Clock.fixed(dateTime.toInstant(), zoneId);}
   *
   *    {@code // Set the fixed clock as the clock used by Time in the production code}
   *    {@code TestTime.testInstance().setClock(fixedClock);}
   * </pre>
   *
   * @param clock
   */
  @Override
  public synchronized void setClock(Clock clock) {
    instance().setClock(clock);
  }

  /**
   * Reset the clock used by {@link Time} and {@link TestTime} with current time using {@link
   * Clock#systemUTC()}.
   */
  public synchronized void resetClock() {
    instance().setClock(Clock.systemUTC());
  }

  /**
   * Jump the clock forward by the given duration.
   *
   * @param duration the duration to jump forward
   */
  public synchronized void fastForward(Duration duration) {
    var updatedClock = Clock.offset(instance().clock(), duration);
    instance().setClock(updatedClock);
    notifyObservers();
  }

  /**
   * Jump the clock backward by the given duration.
   *
   * @param duration the duration to jump backward
   */
  public synchronized void fastBackward(Duration duration) {
    var updatedClock = Clock.offset(instance().clock(), duration.negated());
    instance().setClock(updatedClock);
    notifyObservers();
  }

  /**
   * Simulate time flow by jumping the clock forward by the given step every flowSpeedMillis. This
   * will replace the clock used by {@link Time} in the production code.
   *
   * <pre>
   *    {@code // Prepare a fixed clock}
   *    {@code ZoneId zoneId = TimeZone.getTimeZone("Europe/Warsaw").toZoneId();}
   *    {@code ZonedDateTime dateTime = LocalDateTime.of(2001, 11, 23, 12, 15).atZone(zoneId);}
   *    {@code Clock fixedClock = Clock.fixed(dateTime.toInstant(), zoneId);}
   *
   *    {@code // Set the fixed clock as the clock used by Time in the production code}
   *    {@code TestTime.testInstance().setClock(fixedClock);}
   *
   *    {@code // Prepare time flow parameters}
   *    {@code Duration step = Duration.of(1, ChronoUnit.MINUTES);}
   *    {@code Instant endTime = fixedClock.instant().plus(10, ChronoUnit.MINUTES);}
   *    {@code int flowSpeedMillis = 100;}
   *
   *    {@code // Simulate time flow}
   *    {@code TestTime.testInstance().timeFlow(step, endTime, flowSpeedMillis);}
   * </pre>
   *
   * @param step the duration to jump forward
   * @param endTime the time when the simulated time flow should stop
   * @param flowSpeedMillis the speed of the simulated time flow in milliseconds
   * @throws IllegalArgumentException if flowSpeedMillis is not positive or endTime is before the
   *     current time
   * @throws RuntimeException if interrupted while sleeping between jumps
   */
  public synchronized void timeFlow(Duration step, Instant endTime, int flowSpeedMillis) {

    checkArgument(flowSpeedMillis > 0, "Flow speed must be positive");
    checkArgument(endTime.isAfter(instance().now()), "End time must be after current time");

    try {
      while (instance().now().isBefore(endTime)) {
        sleep(flowSpeedMillis);
        fastForward(step);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Register an observer that will be notified every time the clock is changed by {@link
   * TestTime#fastForward(Duration)}, {@link TestTime#fastBackward(Duration)} or {@link
   * TestTime#timeFlow(Duration, Instant, int)}.
   *
   * <pre>
   *    {@code TestTime}
   *        {@code .testInstance()}
   *        {@code .registerObserver(clock -> System.out.println(clock.instant()));}
   * </pre>
   *
   * @param clockConsumer the observer
   */
  public synchronized void registerObserver(Consumer<Clock> clockConsumer) {
    observers.add(clockConsumer);
  }

  /** Remove all registered observers. */
  public synchronized void clearObservers() {
    observers.clear();
  }

  private void notifyObservers() {
    observers.forEach(observer -> observer.accept(instance().clock()));
  }
}
