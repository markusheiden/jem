package de.heiden.jem.models.c64.monitor.gui;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.C64;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;
import de.heiden.jem.models.c64.components.cpu.DebuggerExit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Main Monitor frame.
 */
public class DebuggerGUI extends JPanel {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private Thread _thread;
  private C64 _c64;
  private CPU6510Debugger _cpu;
  private BusDevice _bus;

  private final JButton _runButton;
  private final JButton _resumeButton;
  private final JButton _suspendButton;
  private final JButton _stopButton;
  private final JButton _refreshButton;

  private StateGUI _state;
  private TraceGUI _trace;
  private DisassemblerGUI _disassembler;
  private final MemDumpGUI _memDump;

  /**
   * Constructor.
   */
  public DebuggerGUI() {
    setLayout(new BorderLayout());

    _runButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/run_exc.gif"), "Run"));
    _resumeButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/resume_co.gif"), "Resume"));
    _suspendButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/suspend_co.gif"), "Suspend"));
    _stopButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/progress_stop.gif"), "Stop"));
    _refreshButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/refresh.gif"), "Refresh"));

    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);
    add(toolBar, BorderLayout.NORTH);
    toolBar.add(_runButton);
    toolBar.add(_resumeButton);
    toolBar.add(_suspendButton);
    toolBar.add(_stopButton);
    toolBar.add(new JLabel(" "));
    toolBar.add(_refreshButton);

    JSplitPane topBottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    JSplitPane top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    topBottom.setTopComponent(top);

    _trace = new TraceGUI();
    top.setLeftComponent(_trace);

    _state = new StateGUI();
    top.setRightComponent(_state);

    JSplitPane bottom = new JSplitPane();
    topBottom.setBottomComponent(bottom);

    _disassembler = new DisassemblerGUI();
//    _disassembler.setAddress(0xE000);
    bottom.setLeftComponent(_disassembler);

    _memDump = new MemDumpGUI();
//    _memDump.setAddress(0xE000);
    bottom.setRightComponent(_memDump);

    add(topBottom);

    _runButton.addActionListener(e -> {
      if (_thread == null) {
        run();
      }
    });

    _suspendButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (_thread != null) {
          suspend();
        }
      }
    });
    _resumeButton.addActionListener(e -> {
      if (_thread != null) {
        resume();
      }
    });

    _stopButton.addActionListener(e -> {
      if (_thread != null) {
        stop();
      }
    });

    _refreshButton.addActionListener(e -> {
      if (_thread != null) {
        updateComponents();
      }
    });

    updateButtons();
  }

  //
  // Toolbar actions
  //

  /**
   * Run C64.
   */
  private void run() {
    try {
      _c64 = new C64(true);
      _cpu = (CPU6510Debugger) _c64.getCpu();
      _bus = _c64.getCpuBus();

      _trace.setCpu(_cpu);
      _state.setCpu(_cpu);
      _disassembler.setBus(_bus);
      _memDump.setBus(_bus);

      _thread = new Thread(() -> {
        try {
          logger.info("C64 has been started");
          _c64.start();
        } catch (DebuggerExit e) {
          logger.info(e.getMessage());
        } catch (Exception e) {
          logger.error("C64 terminated abnormally", e);
        } finally {
          _c64 = null;
          _cpu = null;
          _bus = null;
        }
      }, "C64 (debug)");
      _thread.start();

      updateComponents();
    } catch (Exception e) {
      logger.error("Failed to start C64", e);
    }
  }

  /**
   * Suspend C64.
   */
  private void suspend() {
    new SwingWorker<Object, Object>() {
      @Override
      protected Object doInBackground() throws Exception {
        try {
          _cpu.suspendAndWait();
        } catch (DebuggerExit e) {
          _thread.interrupt();
          _thread = null;
        }

        return null;
      }

      @Override
      protected void done() {
        updateComponents();
      }
    }.execute();
  }

  /**
   * Resume C64.
   */
  private void resume() {
    new SwingWorker<Object, Object>() {
      @Override
      protected Object doInBackground() throws Exception {
        try {
          _cpu.resume();
        } catch (DebuggerExit e) {
          _thread.interrupt();
          _thread = null;
        }

        return null;
      }

      @Override
      protected void done() {
        updateButtons();
      }
    }.execute();
  }

  /**
   * Stop C64.
   */
  private void stop() {
    new SwingWorker<Object, Object>() {
      @Override
      protected Object doInBackground() throws Exception {
        try {
          _cpu.stop();
          _thread.join();
        } catch (InterruptedException e) {
          // ignore
        } finally {
          _thread = null;
        }

        return null;
      }

      @Override
      protected void done() {
        updateButtons();
      }
    }.execute();
  }

  /**
   * Update state of buttons.
   */
  private void updateButtons() {
    _runButton.setEnabled(_thread == null);
    _suspendButton.setEnabled(_thread != null && !_cpu.isSuspended());
    _resumeButton.setEnabled(_thread != null && _cpu.isSuspended());
    _stopButton.setEnabled(_thread != null);
  }


  /**
   * Notify gui to display current values.
   */
  private void updateComponents() {
    updateButtons();

    _trace.stateChanged();
    _state.stateChanged();
    _disassembler.setAddress(_cpu.getState().PC);
    _disassembler.codeChanged();
    _memDump.memoryChanged();
  }
}
