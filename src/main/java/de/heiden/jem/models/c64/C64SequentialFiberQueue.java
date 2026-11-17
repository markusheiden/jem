package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.loom.FiberScheduler;
import de.heiden.jem.components.clock.loom.SequentialFiberQueueClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.heiden.jem.models.c64.gui.javafx.emulator.C64Application.start;

/**
 * C64 startup with {@link SequentialFiberQueueClock}.
 */
public class C64SequentialFiberQueue {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(C64SequentialFiberQueue.class);

    /**
     * Start the application.
     */
    public static void main(String[] args) {
        try {
            // The fibers execute strictly sequentially, so one carrier thread is sufficient and much faster.
            FiberScheduler.useSingleCarrier();
            start("--clock=" + SequentialFiberQueueClock.class.getName());
        } catch (Exception e) {
            logger.error("Unable to startup", e);
        }
    }
}
