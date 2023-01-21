package pl.amazingcode.time;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.within;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.function.Consumer;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
final class Time_Scenarios {

    private static final ZoneId ZONE_ID = TimeZone.getTimeZone("Europe/Warsaw").toZoneId();
    private static final Clock
        FIXED_CLOCK = Clock.fixed(LocalDateTime.of(1983, 10, 23, 9, 15).atZone(ZONE_ID).toInstant(), ZONE_ID);

    private static final String ROOT_PACKAGE = "pl.amazingcode";

    private final JavaClasses classes = new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeTests())
        .importPackages(ROOT_PACKAGE);

    @AfterEach
    void afterEach() {
        TestTime.testInstance().resetClock();
    }

    @Test
    void Get_instant() {
        // When
        var now = Time.instance().now();

        // Then
        var instantNow = Instant.now();
        then(now).isCloseTo(instantNow, within(1, ChronoUnit.MILLIS));
    }

    @Test
    void Get_clock() {
        // When
        var clock = Time.instance().clock();

        // Then
        var now = Instant.now(clock);
        var instantNow = Instant.now();
        then(now).isCloseTo(instantNow, within(1, ChronoUnit.MILLIS));
    }

    @Test
    void Use_custom_clock() {
        // Given
        TestTime.testInstance().setClock(FIXED_CLOCK);

        // When
        var now = Time.instance().now();

        // Then
        then(now).isCloseTo(FIXED_CLOCK.instant(), within(1, ChronoUnit.MILLIS));
    }

    @Test
    void Reset_to_original_clock() {
        // Given
        TestTime.testInstance().setClock(FIXED_CLOCK);

        // When
        TestTime.testInstance().resetClock();

        // Then
        var now = Time.instance().now();
        var instantNow = Instant.now();
        then(now).isCloseTo(instantNow, within(1, ChronoUnit.MILLIS));
    }

    @Test
    void Multiple_calls_has_the_same_result() {
        // Given
        TestTime.testInstance().setClock(FIXED_CLOCK);

        // When
        var instant1 = Time.instance().now();
        var instant2 = Time.instance().now();
        var instant3 = Time.instance().now();

        // Then
        then(instant1).isCloseTo(instant2, within(1, ChronoUnit.MILLIS));
        then(instant2).isCloseTo(instant3, within(1, ChronoUnit.MILLIS));
        then(instant1).isCloseTo(Instant.now(FIXED_CLOCK), within(1, ChronoUnit.MILLIS));
    }

    @Test
    void Increase_by_duration() {
        // Given
        TestTime.testInstance().setClock(FIXED_CLOCK);
        var beginning = Time.instance().now();
        var duration = Duration.of(10, ChronoUnit.MINUTES);

        // When
        TestTime.testInstance().fastForward(duration);

        // Then
        var now = Time.instance().now();
        then(now).isAfter(beginning);
        then(now).isCloseTo(beginning.plus(10, ChronoUnit.MINUTES), within(1, ChronoUnit.MILLIS));
    }

    @Test
    void Decrease_by_duration() {
        // Given
        TestTime.testInstance().setClock(FIXED_CLOCK);
        var now = Time.instance().now();
        var duration = Duration.of(10, ChronoUnit.MINUTES);

        // When
        TestTime.testInstance().fastBackward(duration);

        // Then
        var past = Time.instance().now();
        then(past).isBefore(now);
        then(past).isCloseTo(now.minus(10, ChronoUnit.MINUTES), within(1, ChronoUnit.MILLIS));
    }

    @Test
    void Speed_up_time_flow() {
        // Given
        TestTime.testInstance().setClock(FIXED_CLOCK);
        var step = Duration.of(1, ChronoUnit.MINUTES);
        var endTime = Time.instance().now().plus(10, ChronoUnit.MINUTES);
        var flowSpeedMillis = 100;
        // When
        TestTime.testInstance().timeFlow(step, endTime, flowSpeedMillis);

        // Then
        var now = Time.instance().now();
        then(now).isCloseTo(endTime, within(1, ChronoUnit.MILLIS));
    }

    @Test
    void Test_usage() {

        noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("pl.amazingcode.time.TestTime")
            .check(classes);
    }

    @Test
    void Observe_test_clock() {
        // Given
        TestTime.testInstance().setClock(FIXED_CLOCK);
        var step = Duration.of(1, ChronoUnit.MINUTES);
        var endTime = Time.instance().now().plus(10, ChronoUnit.MINUTES);
        var flowSpeedMillis = 10;

        var timeChanges = new ArrayList<>();
        Consumer<Clock> clockConsumer = (clock) -> timeChanges.add(clock.instant().toString());

        // When
        TestTime.testInstance().registerObserver(clockConsumer);

        // Then
        TestTime.testInstance().timeFlow(step, endTime, flowSpeedMillis);
        then(timeChanges).hasSize(10);
    }

    @Test
    void Clear_observers() {
        // Given
        TestTime.testInstance().setClock(FIXED_CLOCK);
        var step = Duration.of(1, ChronoUnit.MINUTES);
        var endTime = Time.instance().now().plus(10, ChronoUnit.MINUTES);
        var flowSpeedMillis = 10;

        var timeChanges = new ArrayList<>();
        Consumer<Clock> clockConsumer = (clock) -> timeChanges.add(clock.instant().toString());
        TestTime.testInstance().registerObserver(clockConsumer);

        // When
        TestTime.testInstance().clearObservers();

        // Then
        TestTime.testInstance().timeFlow(step, endTime, flowSpeedMillis);
        then(timeChanges).hasSize(0);
    }

    @Nested
    class Report_error_on {

        @Test
        void end_time_flow_smaller_than_now() {
            // Given
            TestTime.testInstance().setClock(FIXED_CLOCK);
            var step = Duration.of(1, ChronoUnit.MINUTES);
            var invalidEndTime = Time.instance().now().minus(10, ChronoUnit.MINUTES);
            var flowSpeedMillis = 100;

            // When
            var throwable = catchThrowable(() -> TestTime.testInstance().timeFlow(step, invalidEndTime, flowSpeedMillis));

            // Then
            then(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End time must be after current time");
        }

        @Test
        void flow_speed_smaller_than_one_millisecond() {
            // Given
            TestTime.testInstance().setClock(FIXED_CLOCK);
            var step = Duration.of(1, ChronoUnit.MINUTES);
            var endTime = Time.instance().now().plus(10, ChronoUnit.MINUTES);
            var invalidFlowSpeedMillis = 0;

            // When
            var throwable = catchThrowable(() -> TestTime.testInstance().timeFlow(step, endTime, invalidFlowSpeedMillis));

            // Then
            then(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Flow speed must be positive");
        }
    }
}
