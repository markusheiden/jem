package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.serialthreads.SerialClock;
import org.serialthreads.Interruptible;
import org.serialthreads.context.IRunnable;
import org.serialthreads.context.ITransformedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.heiden.jem.models.c64.gui.javafx.emulator.C64Application.start;

/**
 * C64 startup with {@link SerialClock} applying byte code transformation via an agent.
 * <p>
 * The JVM has to be started with the serial threads agent, e.g. via {@code ./gradlew runAgent}:
 * <pre>{@code java -javaagent:serialthreads.jar ... de.heiden.jem.models.c64.C64SerialAgent}</pre>
 */
public class C64SerialAgent {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(C64SerialAgent.class);

    /**
     * Start the application.
     */
    public static void main(String[] args) {
        if (!isTransformationActive()) {
            logger.error("Byte code transformation is not active. " +
                    "Start the JVM with the serial threads agent (-javaagent:serialthreads.jar), e.g. via ./gradlew runAgent.");
            System.exit(1);
        }

        try {
            start("--clock=" + SerialClock.class.getName());
        } catch (Exception e) {
            logger.error("Unable to startup", e);
        }
    }

    /**
     * Check that the serial threads agent is active:
     * If so, it has made the {@link TransformationCheck} class implement {@link ITransformedRunnable}.
     */
    private static boolean isTransformationActive() {
        return ITransformedRunnable.class.isAssignableFrom(TransformationCheck.class);
    }

    /**
     * Class which is never executed, just loaded to check whether byte code transformation is active.
     */
    private static final class TransformationCheck implements IRunnable {
        @Override
        @Interruptible
        public void run() {}
    }
}
