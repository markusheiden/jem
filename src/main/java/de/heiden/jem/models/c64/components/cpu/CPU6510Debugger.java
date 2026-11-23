package de.heiden.jem.models.c64.components.cpu;

import org.serialthreads.Interruptible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * CPU variant which support debugging.
 */
public class CPU6510Debugger extends CPU6510 {
    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * PC to start tracing at.
     */
    private int tracePoint = -1;
    /**
     * Is tracing enabled?
     */
    private boolean trace = false;
    /**
     * How many opcodes to trace before java breakpoint.
     */
    private int count = 100;

    private boolean suspend;
    private boolean suspended;
    private boolean stop;
    private final Object suspendLock = new Object();

    private int currentTrace;
    private final Trace[] traces;

    private final Set<Integer> breakpoints;

    private final CPU6510State state;

    /**
     * Constructor.
     */
    public CPU6510Debugger() {
        suspend = false;
        suspended = false;
        stop = false;

        currentTrace = 0;
        traces = new Trace[1000000];
        for (var i = 0; i < traces.length; i++) {
            traces[i] = new Trace();
        }

        breakpoints = new HashSet<>();

        state = getState();
    }

    @Override
    @Interruptible
    public void reset() {
        suspend = false;
        suspended = false;
        stop = false;

        super.reset();
    }

    @Override
    @Interruptible
    protected final void execute() {
        //
        // Support for manual tracing per java breakpoint.
        //

        if (state.PC == tracePoint) {
            trace = true;
        }

        if (trace && logger.isDebugEnabled()) {
            if (state.NMI || state.IRQ && !state.I) {
                logger.debug(Monitor.state(state));
            } else {
                logger.debug(Monitor.state(state) + "  " + Monitor.disassemble(state.PC, bus));
            }
            if (count-- == 0) {
                // Dummy statement to set java breakpoint at.
                System.out.println("STOP");
            }
        }

        //
        // Automatic tracing of the last executed opcodes.
        //

        var trace = traces[currentTrace++];
        if (currentTrace >= traces.length) {
            currentTrace = 0;
        }
        trace.read(state.PC, bus);

        //
        // CPU Breakpoints.
        //

        synchronized (suspendLock) {
            if (stop) {
                throw new DebuggerExit("C64 has been stopped");
            }
            if (breakpoints.contains(state.PC)) {
                suspend = true;
            }
            if (suspend) {
                waitForResume();
            }
        }

        super.execute();
    }

    private void waitForResume() {
        try {
            // Wait for a resume.
            synchronized (suspendLock) {
                suspended = true;
                suspendLock.notifyAll();
                while (suspend) {
                    suspendLock.wait();
                }
                suspended = false;
            }
        } catch (InterruptedException e) {
            throw new DebuggerExit("Thread has been stopped: " + e.getMessage());
        }
    }

    /**
     * Is the cpu currently suspended?
     */
    public boolean isSuspended() {
        synchronized (suspendLock) {
            return suspend;
        }
    }

    /**
     * Suspend cpu execution.
     */
    public void suspendAndWait() throws DebuggerExit {
        try {
            synchronized (suspendLock) {
                suspend = true;
                while (!suspended) {
                    suspendLock.wait();
                }
            }
        } catch (InterruptedException e) {
            throw new DebuggerExit("Thread has been stopped: " + e.getMessage());
        }
    }

    /**
     * Resume cpu execution.
     */
    public void resume() {
        synchronized (suspendLock) {
            suspend = false;
            suspendLock.notifyAll();
        }
    }

    /**
     * Resume cpu execution for execution of 1 opcode.
     */
    public void resume1() {
        synchronized (suspendLock) {
            suspend = true;
            suspendLock.notifyAll();
        }
    }

    public void stop() {
        synchronized (suspendLock) {
            stop = true;
            resume();
        }
    }

    public void addBreakpoint(int addr) {
        synchronized (suspendLock) {
            breakpoints.add(addr);
        }
    }

    public int getCurrentTrace() {
        return currentTrace;
    }

    public Trace[] getTraces() {
        return traces;
    }

    @Override
    public String toString() {
        var result = new StringBuilder();
        result.append(getClass().getSimpleName());
        result.append(":\n");
        for (var i = 20; i > 0; i--) {
            var t = currentTrace - i;
            if (t < 0) {
                t += traces.length;
            }
            result.append(traces[t]);
        }

        return result.toString();
    }
}
