package de.heiden.jem.models.c64.components.cpu;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.models.c64.components.bus.C64Bus;
import org.serialthreads.Interruptible;

import java.util.HashSet;
import java.util.Set;

/**
 * CPU variant which support debugging.
 */
public class CPU6510Debugger extends CPU6510
{
  private boolean _suspend;
  private boolean _suspended;
  private boolean _stop;
  private final Object _suspendLock = new Object();

  private final Set<Integer> _breakpoints;

  private final CPU6510State _state;

  /**
   * Constructor.
   *
   * @param clock system clock
   * @param bus cpu bus
   * @require clock != null
   * @require bus != null
   */
  public CPU6510Debugger(Clock clock, C64Bus bus)
  {
    super(clock, bus);

    _suspend = false;
    _suspended = false;
    _stop = false;
    _breakpoints = new HashSet<Integer>();

    _state = getState();
  }

  @Override
  @Interruptible
  public void reset()
  {
    _suspend = false;
    _suspended = false;
    _stop = false;

    super.reset();
  }

  @Override
  protected void preExecute()
  {
    synchronized (_suspendLock)
    {
      if (_stop)
      {
        throw new DebuggerExit("C64 has been stopped");
      }
      if (_suspend || _breakpoints.contains(_state.PC))
      {
        waitForResume();
      }
    }
  }

  private void waitForResume()
  {
    try
    {
      // wait for a resume
      synchronized (_suspendLock)
      {
        _suspended = true;
        _suspendLock.notifyAll();
        while (_suspend)
        {
          _suspendLock.wait();
        }
        _suspended = false;
      }
    }
    catch (InterruptedException e)
    {
      throw new DebuggerExit("Thread has been stopped: " + e.getMessage());
    }
  }

  /**
   * Suspend cpu execution.
   */
  public void suspendAndWait()
  {
    try
    {
      synchronized (_suspendLock)
      {
        _suspend = true;
        while (!_suspended)
        {
          _suspendLock.wait();
        }
      }
    }
    catch (InterruptedException e)
    {
      throw new DebuggerExit("Thread has been stopped: " + e.getMessage());
    }
  }

  /**
   * Resume cpu execution.
   */
  public void resume()
  {
    synchronized (_suspendLock)
    {
      _suspend = false;
      _suspendLock.notifyAll();
    }
  }

  /**
   * Resume cpu execution for execution of 1 opcode.
   */
  public void resume1()
  {
    synchronized (_suspendLock)
    {
      _suspend = true;
      _suspendLock.notifyAll();
    }
  }

  public void stop()
  {
    synchronized (_suspendLock)
    {
      _stop = true;
      resume();
    }
  }

  public void addBreakpoint(int addr)
  {
    synchronized (_suspendLock)
    {
      _breakpoints.add(addr);
    }
  }
}
