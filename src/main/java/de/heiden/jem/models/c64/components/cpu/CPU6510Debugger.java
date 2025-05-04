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
    private int _tracePoint = -1;
    /**
     * Is tracing enabled?
     */
    private boolean _trace = false;
    /**
     * How many opcodes to trace before java breakpoint.
     */
    private int _count = 100;

    private boolean _suspend;
    private boolean _suspended;
    private boolean _stop;
    private final Object _suspendLock = new Object();

    private int _currentTrace;
    private final Trace[] _traces;

    private final Set<Integer> _breakpoints;

    private final CPU6510State _state;

    /**
     * Constructor.
     */
    public CPU6510Debugger() {
        _suspend = false;
        _suspended = false;
        _stop = false;

        _currentTrace = 0;
        _traces = new Trace[1000000];
        for (int i = 0; i < _traces.length; i++) {
            _traces[i] = new Trace();
        }

        _breakpoints = new HashSet<>();

        _state = getState();
    }

    @Override
    @Interruptible
    public void reset() {
        _suspend = false;
        _suspended = false;
        _stop = false;

        super.reset();
    }

    @Override
    @Interruptible
    protected final void execute() {
        //
        // Support for manual tracing per java breakpoint
        //

        if (_state.PC == _tracePoint) {
            _trace = true;
        }

        if (_trace && logger.isDebugEnabled()) {
            if (_state.NMI || _state.IRQ && !_state.I) {
                logger.debug(Monitor.state(_state));
            } else {
                logger.debug(Monitor.state(_state) + "  " + Monitor.disassemble(_state.PC, bus));
            }
            if (_count-- == 0) {
                // dummy statement to set java breakpoint at
                System.out.println("STOP");
            }
        }

        //
        // Automatic tracing of the last executed opcodes
        //

        Trace trace = _traces[_currentTrace++];
        if (_currentTrace >= _traces.length) {
            _currentTrace = 0;
        }
        trace.read(_state.PC, bus);

        //
        // CPU Breakpoints
        //

        synchronized (_suspendLock) {
            if (_stop) {
                throw new DebuggerExit("C64 has been stopped");
            }
            if (_breakpoints.contains(_state.PC)) {
                _suspend = true;
            }
            if (_suspend) {
                waitForResume();
            }
        }

        super.execute();
    }

    private void waitForResume() {
        try {
            // wait for a resume
            synchronized (_suspendLock) {
                _suspended = true;
                _suspendLock.notifyAll();
                while (_suspend) {
                    _suspendLock.wait();
                }
                _suspended = false;
            }
        } catch (InterruptedException e) {
            throw new DebuggerExit("Thread has been stopped: " + e.getMessage());
        }
    }

    /**
     * Is the cpu currently suspended?
     */
    public boolean isSuspended() {
        synchronized (_suspendLock) {
            return _suspend;
        }
    }

    /**
     * Suspend cpu execution.
     */
    public void suspendAndWait() throws DebuggerExit {
        try {
            synchronized (_suspendLock) {
                _suspend = true;
                while (!_suspended) {
                    _suspendLock.wait();
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
        synchronized (_suspendLock) {
            _suspend = false;
            _suspendLock.notifyAll();
        }
    }

    /**
     * Resume cpu execution for execution of 1 opcode.
     */
    public void resume1() {
        synchronized (_suspendLock) {
            _suspend = true;
            _suspendLock.notifyAll();
        }
    }

    public void stop() {
        synchronized (_suspendLock) {
            _stop = true;
            resume();
        }
    }

    public void addBreakpoint(int addr) {
        synchronized (_suspendLock) {
            _breakpoints.add(addr);
        }
    }

    public int getCurrentTrace() {
        return _currentTrace;
    }

    public Trace[] getTraces() {
        return _traces;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(getClass().getSimpleName());
        result.append(":\n");
        for (int i = 20; i > 0; i--) {
            int t = _currentTrace - i;
            if (t < 0) {
                t += _traces.length;
            }
            result.append(_traces[t]);
        }

        return result.toString();
    }
}
