## About

- Provides java clock which can be used in production code and easily adjusted/changed in tests.
- No passing java `Clock` as a dependency or using mocking libraries is required to test time dependent code.
- Have one time provider across the whole application.

### Without `timeflow` library

- Passing a `Clock` as a dependency in production code to make it testable
  - if you have good design, and you can easily pass `Clock` as a dependency and test your time dependent code - go for it!
- Update design to pass `Clock` as a dependency (could be challenging in legacy code).
- In Spring Boot tests we can't alter provided fixed `Clock` bean.
- Using [Mockito](https://site.mockito.org/) - e.g. mocking Instant.now() not working outside of the test scope.

```java
class SomeService {

    private final Clock clock;

    SomeService(Clock clock) {
        this.clock = clock;
    }

    void doSomething() {
        var now = Instant.now(clock);
        // do something
    }
}
```

### With `timeflow` library

- No need to pass `Clock` as a dependency in production code anymore to make it testable.

```java
class SomeService {

    void doSomething() {
        var now = Time.instance().now(); // or var now = Instant.now(Time.instance().clock());
        // do something
    }

}
```

and just [alter time in tests](#in-tests) using

```java
TestTime.testInstance().setClock(fixedClock);
// or
TestTime.testInstance().fastForward(duration);
// or
TestTime.testInstance().timeFlow(step, endTime, flowSpeedMillis);
```


## Requirements

- Java 17+
- Java 11 - use [timeflow-java11](https://github.com/stawirej/timeflow-java11)
- Java 8 - use [timeflow-java8](https://github.com/stawirej/timeflow-java8)

## Dependencies

---

### Maven

```xml 

<dependency>
    <groupId>pl.amazingcode</groupId>
    <artifactId>timeflow</artifactId>
    <version>1.5.0</version>
</dependency>
```

### Gradle

```groovy
implementation group: 'pl.amazingcode', name: 'timeflow', version: "1.5.0"
```

## Usage

### In production code

#### Obtain current time

```java
Instant now = Time.instance().now();
```

#### Obtain Clock

```java
Clock clock = Time.instance().clock();
```

> **_NOTE:_**  Use `timeflow` library directly in production code. Do not use e.g. as a Spring Bean.

### In tests

```java
final class Time_Scenarios {

    private static final ZoneId ZONE_ID = TimeZone.getTimeZone("Europe/Warsaw").toZoneId();
    private static final Clock
        FIXED_CLOCK = Clock.fixed(LocalDateTime.of(1983, 10, 23, 9, 15).atZone(ZONE_ID).toInstant(), ZONE_ID);

    @AfterEach
    void afterEach() {
        TestTime.testInstance().resetClock();
    }

    @Test
    void Test_time_jump_for_something() {
        // Given
        TestTime.testInstance().setClock(FIXED_CLOCK);
        var duration = Duration.of(10, ChronoUnit.MINUTES);

        // time dependent code under test

        // When
        TestTime.testInstance().fastForward(duration);  // jump forward 10 minutes

        // Then
        // do assertions
    }

    @Test
    void Test_time_flow_for_something() {
        // Given
        TestTime.testInstance().setClock(FIXED_CLOCK);
        var step = Duration.of(1, ChronoUnit.MINUTES);
        var endTime = Time.instance().now().plus(10, ChronoUnit.MINUTES);
        var flowSpeedMillis = 100;

        // time dependent code under test e.g. scheduled task

        // When
        TestTime.testInstance().timeFlow(step, endTime, flowSpeedMillis); // simulate speed up time flow with given step 

        // Then
        // do assertions
    }
}
```

- More cases in [Time_Scenarios](src/test/java/pl/amazingcode/timeflow/Time_Scenarios.java) test class.

#### Provide consumer to observe time changes

```java
TestTime.testInstance().registerObserver(clock->System.out.println(clock.instant().toString()));
```

#### Ensure only `time` library is used in production code

- Add dependency for [ArchUnit](https://www.archunit.org/)
- Detect usage of `TestTime`, `Clock`, and `now()` method from `Instant`, `LocalDateTime`, `LocalDate`, `LocalTime` in production code by writing tests:

```java
final class TimeTest {

    private static final String ROOT_PACKAGE = "YOUR_APP_ROOT_PACKAGE";

    private final JavaClasses classes = new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeTests())
        .importPackages(ROOT_PACKAGE);

    @Test
    void TestTime_not_used_in_production_code() {
        noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("pl.amazingcode.timeflow.TestTime")
            .check(classes);
    }

    @Test
    void Clock_not_used_in_production_code() {
        noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.time.Clock")
            .check(classes);
    }

    @Test
    void Instant_now_not_used_in_production_code() {
        noClasses()
            .that()
            .should()
            .callMethod(Instant.class.getName(), "now")
            .check(classes);
    }

    @Test
    void LocalDateTime_now_not_used_in_production_code() {
        noClasses().should().callMethod(LocalDateTime.class.getName(), "now").check(classes);
    }

    @Test
    void LocalDate_now_not_used_in_production_code() {
        noClasses().should().callMethod(LocalDate.class.getName(), "now").check(classes);
    }

    @Test
    void LocalTime_now_not_used_in_production_code() {
        noClasses().should().callMethod(LocalTime.class.getName(), "now").check(classes);
    }
}
```
## Example

### Use cases

- Can't create ticket for past event
- Ticket is expired after event

### Production code

```java
final class Ticket {

    private final Instant eventDateTime;

    private Ticket(Instant eventDateTime) {
        this.eventDateTime = eventDateTime;
    }

    public static Ticket create(int year, int month, int dayOfMonth, int hour, int minute, String timezone) {
        var zoneId = ZoneId.of(timezone);
        var eventDateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute).atZone(zoneId).toInstant();

        var now = Time.instance().now();    // <1>
        if (eventDateTime.isBefore(now)) {
            throw new IllegalArgumentException("Cannot create ticket for past event!");
        }

        return new Ticket(eventDateTime);
    }

    public boolean isExpired() {
        var now = Time.instance().now();    // <2>
        return eventDateTime.isBefore(now);
    }
}
```

1. Use `timeflow` library to obtain current time
2. Use `timeflow` library to obtain current time

### Tests

```java
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
final class Ticket_Scenarios {

    private static final ZoneId ZONE_ID = TimeZone.getTimeZone("Europe/Warsaw").toZoneId();
    private static final Clock FIXED_CLOCK =
        Clock.fixed(LocalDateTime.of(2020, 10, 22, 20, 30).atZone(ZONE_ID).toInstant(), ZONE_ID);

    @AfterEach
    void afterEach() {
        TestTime.testInstance().resetClock();   // <5>
    }

    @Test
    void Cant_create_ticket_for_past_event() {
        // Given
        TestTime.testInstance().setClock(FIXED_CLOCK);

        // When
        var throwable = catchThrowable(() -> Ticket.create(2019, 1, 17, 20, 30, "Europe/Warsaw"));

        // Then
        then(throwable)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot create ticket for past event!");
    }

    @Test
    void Ticket_expires_after_event() {
        // Given
        TestTime.testInstance().setClock(FIXED_CLOCK);                      // <1>
        var ticket = Ticket.create(2020, 10, 23, 20, 30, ZONE_ID.getId());  // <2>
        TestTime.testInstance().fastForward(Duration.ofDays(2));            // <3>

        // Then
        then(ticket.isExpired()).isTrue();                                  // <4>
    }
}
```

1. Set current time to `2020-10-22 20:30` at given timezone.
2. Create ticket for future event at `2020-10-23 20:30` at given timezone.
3. Fast forward time by 2 days to `2020-10-24 20:30` at given timezone.
4. Check ticket is expired.
5. Reset clock after each test to default value not to affect other tests.