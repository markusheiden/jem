package de.heiden.jem.components.clock;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link AbstractClock}.
 */
@Tag("clock")
class AbstractClockTest {
    /**
     * List of executed events.
     */
    private final List<ClockEvent> executed = new ArrayList<>();

    @Test
    void constructor() {
        try (var clock = new TestClock()) {
            assertThat(clock.getNextEventTick()).isEqualTo(Long.MAX_VALUE);
        }
    }

    @Test
    void addClockEvent() {
        try (var clock = new TestClock()) {
            var event1 = new TestClockEvent();
            var event2 = new TestClockEvent();
            var event3 = new TestClockEvent();

            // Add the first event -> Next tick should be set to tick of this event.
            clock.addClockEvent(2, event2);
            assertThat(event2.previous).isNull();
            assertThat(event2.next).isNotNull();
            assertThat(clock.getNextEvent()).isSameAs(event2);
            assertThat(clock.getNextEventTick()).isEqualTo(2);

            // Add event before first event -> Next tick should be set to tick of this event.
            clock.addClockEvent(1, event1);
            assertThat(event1.previous).isNull();
            assertThat(event1.next).isSameAs(event2);
            assertThat(event2.previous).isSameAs(event1);
            assertThat(clock.getNextEvent()).isSameAs(event1);
            assertThat(clock.getNextEventTick()).isEqualTo(1);

            // Add event after first event -> Next tick should not change.
            clock.addClockEvent(3, event3);
            assertThat(event2.next).isSameAs(event3);
            assertThat(event3.previous).isSameAs(event2);
            assertThat(event3.next).isNotNull();
            assertThat(clock.getNextEvent()).isSameAs(event1);
            assertThat(clock.getNextEventTick()).isEqualTo(1);
        }
    }

    @Test
    void addClockEvent_registered() {
        try (var clock = new TestClock()) {
            var event1 = new TestClockEvent();
            var event2 = new TestClockEvent();

            clock.addClockEvent(1, event1);
            clock.addClockEvent(2, event2);

            // Add an already registered event -> It gets moved.
            clock.addClockEvent(3, event1);
            assertThat(clock.getNextEvent()).isSameAs(event2);
            assertThat(event2.previous).isNull();
            assertThat(event2.next).isSameAs(event1);
            assertThat(event1.previous).isSameAs(event2);
            assertThat(event1.next).isNotNull();
            assertThat(clock.getNextEventTick()).isEqualTo(2);
        }
    }

    @Test
    void updateClockEvent() {
        try (var clock = new TestClock()) {
            var event10 = new TestClockEvent();
            var event20 = new TestClockEvent();
            var event30 = new TestClockEvent();

            clock.addClockEvent(10, event10);
            clock.addClockEvent(20, event20);
            clock.addClockEvent(30, event30);

            // Nothing to update.
            clock.updateClockEvent(10, event10);
            assertThat(event10.next).isSameAs(event20);
            assertThat(clock.getNextEvent()).isSameAs(event10);
            assertThat(clock.getNextEventTick()).isEqualTo(10);
            // event10 is still the first, but the next event tick needs to be updated.
            clock.updateClockEvent(11, event10);
            assertThat(event10.next).isSameAs(event20);
            assertThat(clock.getNextEvent()).isSameAs(event10);
            assertThat(clock.getNextEventTick()).isEqualTo(11);
            // event10 is no longer the first event -> the next event (event20) takes its place.
            clock.updateClockEvent(21, event10);
            assertThat(event20.next).isSameAs(event10);
            assertThat(event10.next).isSameAs(event30);
            assertThat(clock.getNextEvent()).isSameAs(event20);
            assertThat(clock.getNextEventTick()).isEqualTo(20);
            // event10 is now the last event.
            clock.updateClockEvent(31, event10);
            assertThat(event30.next).isSameAs(event10);
            assertThat(clock.getNextEvent()).isSameAs(event20);
            assertThat(clock.getNextEventTick()).isEqualTo(20);
        }
    }

    @Test
    void removeClockEvent_notAdded() {
        try (var clock = new TestClock()) {
            var event2 = new TestClockEvent();
            var event1 = new TestClockEvent();

            clock.addClockEvent(2, event2);

            // Remove not registered event -> No change.
            clock.removeClockEvent(event1);
            assertThat(event1.previous).isNull();
            assertThat(event1.next).isNull();
            assertThat(clock.getNextEvent()).isSameAs(event2);
            assertThat(clock.getNextEventTick()).isEqualTo(2);
        }
    }

    @Test
    void removeClockEvent_only() {
        try (var clock = new TestClock()) {
            var event1 = new TestClockEvent();

            clock.addClockEvent(1, event1);

            // Remove only registered event -> Next tick should be reset.
            clock.removeClockEvent(event1);
            assertThat(event1.previous).isNull();
            assertThat(clock.getNextEventTick()).isEqualTo(Long.MAX_VALUE);
        }
    }

    @Test
    void removeClockEvent_first() {
        try (var clock = new TestClock()) {
            var event1 = new TestClockEvent();
            var event2 = new TestClockEvent();

            clock.addClockEvent(1, event1);
            clock.addClockEvent(2, event2);

            // Remove the first registered event -> Next tick should be reset.
            clock.removeClockEvent(event1);
            assertThat(event1.previous).isNull();
            assertThat(event2.previous).isNull();
            assertThat(event2.next).isNotNull();
            assertThat(clock.getNextEvent()).isSameAs(event2);
            assertThat(clock.getNextEventTick()).isEqualTo(2);
        }
    }

    @Test
    void removeClockEvent_many() {
        try (var clock = new TestClock()) {
            var event1 = new TestClockEvent();
            var event2 = new TestClockEvent();
            var event3 = new TestClockEvent();

            clock.addClockEvent(1, event1);
            clock.addClockEvent(2, event2);
            clock.addClockEvent(3, event3);
            assertThat(clock.getNextEventTick()).isEqualTo(1);

            // Remove the second registered event -> next tick should still be the tick of the first event.
            clock.removeClockEvent(event2);
            assertThat(event1.next).isSameAs(event3);
            assertThat(event3.previous).isSameAs(event1);
            assertThat(clock.getNextEvent()).isSameAs(event1);
            assertThat(clock.getNextEventTick()).isEqualTo(1);
        }
    }

    @Test
    void removeClockEvent_twice() {
        try (var clock = new TestClock()) {
            var event1 = new TestClockEvent();
            var event2 = new TestClockEvent();
            var event3 = new TestClockEvent();

            clock.addClockEvent(1, event1);
            clock.addClockEvent(2, event2);
            clock.removeClockEvent(event2);
            clock.addClockEvent(3, event3);

            // Remove the already removed event again -> The other events have to stay registered.
            clock.removeClockEvent(event2);
            assertThat(event2.previous).isNull();
            assertThat(event2.next).isNull();
            assertThat(clock.getNextEvent()).isSameAs(event1);
            assertThat(event1.next).isSameAs(event3);
            assertThat(event3.previous).isSameAs(event1);
            assertThat(event3.next).isNotNull();
        }
    }

    @Test
    void removeClockEvent_executed() {
        try (var clock = new TestClock()) {
            var event1 = new TestClockEvent();
            var event2 = new TestClockEvent();
            var event3 = new TestClockEvent();

            clock.addClockEvent(1, event1);
            clock.addClockEvent(2, event2);
            clock.addClockEvent(3, event3);
            clock.executeEvents(1);
            assertThat(event1.next).isNull();
            assertThat(event2.previous).isNull();

            // Remove the already executed event -> The other events have to stay registered.
            clock.removeClockEvent(event1);
            assertThat(clock.getNextEvent()).isSameAs(event2);
            assertThat(event2.previous).isNull();
            assertThat(event2.next).isSameAs(event3);
            assertThat(event3.previous).isSameAs(event2);
        }
    }

    @Test
    void updateClockEvent_executed() {
        try (var clock = new TestClock()) {
            var event1 = new TestClockEvent();
            var event2 = new TestClockEvent();
            var event3 = new TestClockEvent();

            clock.addClockEvent(1, event1);
            clock.addClockEvent(2, event2);
            clock.addClockEvent(4, event3);
            clock.executeEvents(1);

            // Update the already executed event -> It gets registered again, the other events stay registered.
            clock.updateClockEvent(3, event1);
            assertThat(clock.getNextEvent()).isSameAs(event2);
            assertThat(event2.next).isSameAs(event1);
            assertThat(event1.previous).isSameAs(event2);
            assertThat(event1.next).isSameAs(event3);
            assertThat(event3.previous).isSameAs(event1);
        }
    }

    @Test
    void execute_reregister() {
        try (var clock = new TestClock()) {
            var event2 = new TestClockEvent();
            var event1 = new ClockEvent("Re-registering event") {
                @Override
                public void execute(long tick) {
                    clock.addClockEvent(3, this);
                }
            };

            clock.addClockEvent(1, event1);
            clock.addClockEvent(2, event2);
            clock.executeEvents(1);

            assertThat(clock.getNextEvent()).isSameAs(event2);
            assertThat(event2.previous).isNull();
            assertThat(event2.next).isSameAs(event1);
            assertThat(event1.previous).isSameAs(event2);
            assertThat(event1.next).isNotNull();
        }
    }

    @Test
    void execute() {
        try (var clock = new TestClock()) {
            var event2a = new TestClockEvent();
            clock.addClockEvent(2, event2a);
            var event2b = new TestClockEvent();
            clock.addClockEvent(2, event2b);
            var event3 = new TestClockEvent();
            clock.addClockEvent(3, event3);
            assertThat(executed).isEmpty();

            // Execute tick 1, which has no registered events -> nothing should happen.
            clock.executeEvents(1);
            assertThat(executed).isEmpty();

            // Execute tick 2, which has two registered events -> both should be executed.
            clock.executeEvents(2);
            assertThat(executed).hasSize(2);
            assertThat(executed).contains(event2a);
            assertThat(executed).contains(event2b);
        }
    }

    /**
     * Clock implementation for testing.
     */
    private static final class TestClock extends AbstractSimpleClock {
        @Override
        protected void doRun() {
            throw new UnsupportedOperationException("Not implemented for test clock");
        }

        @Override
        protected void doRun(int ticks) {
            throw new UnsupportedOperationException("Not implemented for test clock");
        }

        /**
         * Tick, when the next event gets executed.
         */
        long getNextEventTick() {
            return getNextEvent().tick;
        }
    }

    /**
     * Clock event implementation for testing.
     */
    private final class TestClockEvent extends ClockEvent {
        /**
         * Constructor.
         */
        TestClockEvent() {
            super("Test event");
        }

        @Override
        public void execute(long tick) {
            // Record executions.
            executed.add(this);
        }
    }
}
