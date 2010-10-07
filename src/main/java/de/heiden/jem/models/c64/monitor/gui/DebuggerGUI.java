package de.heiden.jem.models.c64.monitor.gui;

import de.heiden.jem.models.c64.C64;
import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;
import de.heiden.jem.models.c64.components.cpu.DebuggerExit;
import org.apache.log4j.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Main Monitor frame.
 */
public class DebuggerGUI extends JPanel
{
  private final Logger _logger = Logger.getLogger(getClass());

  private Thread _thread;
  private C64 _c64;
  private CPU6510Debugger _cpu;
  private BusDevice _bus;

  private StateGUI _state;
  private DisassemblerGUI _disassembler;
  private final MemDumpGUI _memDump;

  public DebuggerGUI()
  {
    setLayout(new BorderLayout());

    JButton runButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/run_exc.gif"), "Run"));
    JButton resumeButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/resume_co.gif"), "Run"));
    JButton suspendButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/suspend_co.gif"), "Run"));
    JButton stopButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/progress_stop.gif"), "Run"));

    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);
    add(toolBar, BorderLayout.NORTH);
    toolBar.add(runButton);
    toolBar.add(resumeButton);
    toolBar.add(suspendButton);
    toolBar.add(stopButton);

    JSplitPane topBottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    JSplitPane top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    topBottom.setTopComponent(top);

    JPanel stack = new JPanel();
    top.setLeftComponent(stack);
    stack.add(new JLabel("TOP/LEFT"));
    _state = new StateGUI();
    top.setRightComponent(_state);

    JSplitPane bottom = new JSplitPane();
    topBottom.setBottomComponent(bottom);

    _disassembler = new DisassemblerGUI();
    _disassembler.setAddress(0xE000);
    bottom.setLeftComponent(_disassembler);
    _memDump = new MemDumpGUI();
    _memDump.setAddress(0xE000);
    bottom.setRightComponent(_memDump);

    add(topBottom);

    runButton.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (_thread == null)
        {
          run();
        }
      }
    });

    suspendButton.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (_thread != null)
        {
          suspend();
        }
      }
    });
    resumeButton.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (_thread != null)
        {
          _cpu.resume();
        }
      }
    });

    stopButton.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (_thread != null)
        {
          stop();
        }
      }
    });
  }

  //
  // Toolbar actions
  //

  private void run()
  {
    try
    {
      _c64 = new C64(true);
      _cpu = (CPU6510Debugger) _c64.getCpu();
      _bus = _c64.getCpuBus();

      _state.setCpu(_cpu);
      _disassembler.setBus(_bus);
      _memDump.setBus(_bus);

      _thread = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            _logger.info("C64 has been started");
            _c64.start();
          }
          catch (DebuggerExit e)
          {
            _logger.info(e.getMessage());
          }
          catch (Exception e)
          {
            _logger.error("C64 terminated abnormally", e);
          }
          finally
          {
            reset();
          }
        }
      }, "C64 (debug)");
      _thread.start();
    }
    catch (Exception e)
    {
      _logger.error("Failed to start C64", e);
    }
  }

  private synchronized void reset()
  {
    _c64.stop();

    _thread = null;
    _c64 = null;
    _cpu = null;
    _bus = null;
  }

  private void suspend()
  {
    new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          _cpu.suspendAndWait();
        }
        catch (DebuggerExit e)
        {
          // ignore
        }
        finally
        {
          updateComponents();
        }
      }
    }, "C64 suspend thread").start();
  }

  private void stop()
  {
    new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          _cpu.stop();
          _thread.join();
        }
        catch (InterruptedException e)
        {
          // ignore
        }
        finally
        {
          updateComponents();
        }
      }
    }, "C64 stop thread").start();
  }

  private void updateComponents()
  {
    _state.stateChanged();
    _disassembler.setAddress(_cpu.getState().PC);
    _disassembler.codeChanged();
    _memDump.memoryChanged();
  }
}
