## About

- Provides java clock which can be used in production code and easily adjusted/changed in tests.
- No passing java `Clock` as a dependency or using mocking libraries is required to test time dependent code.
- Have one time provider across the whole application.

### Without `timeflow` library

- Passing `Clock` as a dependency in production code to make it testable
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

        startBackgroundSomething();

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

        startBackgroundSomething();

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
