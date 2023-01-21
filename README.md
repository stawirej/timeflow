## About

- Provides java clock which can be used in production code and easily adjusted/changed in tests.
- No passing java `Clock` as a dependency in production code is required to make it testable or use mocking libraries.

### Without `time` library

- Passing `Clock` as a dependency in production code to make it testable.
- Sometimes it may require to alter a lot of design to pass `Clock` as a dependency in production code.
- In Spring Boot tests we can't inject during one test different fixed `Clock`.
- Using [Mockito](https://site.mockito.org/) - e.g. mocking Instant.now() not working outside test scope.

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

### With `time` library

- No need to pass `Clock` as a dependency in production code anymore to make it testable.

```java
class SomeService {

    void doSomething() {
        var now = Instant.now(Time.instance().clock());
        // do something
    }

}
```

## Requirements

- Java 17
- _If you need to use in other LTS java version, please create an [issue](https://github.com/stawirej/time/issues).

## Dependencies

---

- Not yet released.Snapshot version to play with: `0.0.1-SNAPSHOT`.
- Add https://s01.oss.sonatype.org/content/repositories/snapshots/ as snapshot repository.
- For gradle:

```groovy
repositories {
    maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots/" }
}
```

---

### Maven

```xml 

<dependency>
    <groupId>pl.amazingcode</groupId>
    <artifactId>time</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation group: 'pl.amazingcode', name: 'time', version: "0.0.1-SNAPSHOT"
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

        startSomething();

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

        startSomething();

        // When
        TestTime.testInstance().timeFlow(step, endTime, flowSpeedMillis); // simulate speed up time flow with given step 

        // Then
        // do assertions
    }
}
```

#### Provide consumer to observe time changes

```java
TestTime.testInstance().registerObserver(clock->System.out.println(clock.instant().toString()));
```

#### Ensure `TestTime` class is not used in production code

- Add dependency for [ArchUnit](https://www.archunit.org/)
- Detect usage of `TestTime` class in production code by writing test:

```java
...
private static final String ROOT_PACKAGE="YOUR_APP_ROOT_PACKAGE";

private final JavaClasses classes=new ClassFileImporter()
    .withImportOption(new ImportOption.DoNotIncludeTests())
    .importPackages(ROOT_PACKAGE);
...

@Test
void TestTime_not_used_in_production_code(){
    noClasses()
    .should()
    .dependOnClassesThat()
    .haveFullyQualifiedName("pl.amazingcode.time.TestTime")
    .check(classes);
}
```