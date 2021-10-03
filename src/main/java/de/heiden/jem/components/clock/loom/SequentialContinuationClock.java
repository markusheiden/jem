package de.heiden.jem.components.clock.loom;

/**
 * Clock using {@link Continuation}s from project loom.
 */
public final class SequentialContinuationClock { // extends AbstractSimpleClock {
//    @Override
//    protected final void doRun() {
//        final var continuations = createContinuations();
//        //noinspection InfiniteLoopStatement
//        while (true) {
//            startTick();
//            for (final Continuation continuation : continuations) {
//                continuation.run();
//            }
//        }
//    }
//
//    @Override
//    protected final void doRun(int ticks) {
//        assert ticks >= 0 : "Precondition: ticks >= 0";
//
//        final var continuations = createContinuations();
//        for (final long stop = getTick() + ticks; getTick() < stop;) {
//            startTick();
//            for (final Continuation continuation : continuations) {
//                continuation.run();
//            }
//        }
//    }
//
//    /**
//     * Create continuations.
//     */
//    private Continuation[] createContinuations() {
//        ClockedComponent[] components = _componentMap.values().toArray(new ClockedComponent[0]);
//        Continuation[] continuations = new Continuation[components.length];
//        for (int i = 0; i < components.length; i++) {
//            final var component = components[i];
//            final var scope = new ContinuationScope("Component " + i + ": " + component.getName());
//            component.setTick(() -> Continuation.yield(scope));
//            continuations[i] = new Continuation(scope, component::run);
//        }
//        return continuations;
//    }
}
