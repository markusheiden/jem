package de.heiden.jem.models.c64.gui.swing.monitor;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.components.C64;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;
import de.heiden.jem.models.c64.components.cpu.DebuggerExit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Main Monitor frame.
 */
public class DebuggerGUI extends JPanel {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private Thread thread;
  private C64 c64;
  private CPU6510Debugger cpu;
  private BusDevice bus;

  private final JButton runButton;
  private final JButton resumeButton;
  private final JButton suspendButton;
  private final JButton stopButton;
  private final JButton refreshButton;

  private StateGUI state;
  private TraceGUI trace;
  private DisassemblerGUI disassembler;
  private final MemDumpGUI memDump;

  /**
   * Constructor.
   */
  public DebuggerGUI() {
    setLayout(new BorderLayout());

    runButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/run_exc.gif"), "Run"));
    resumeButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/resume_co.gif"), "Resume"));
    suspendButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/suspend_co.gif"), "Suspend"));
    stopButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/progress_stop.gif"), "Stop"));
    refreshButton = new JButton(new ImageIcon(getClass().getResource("/icons/enabled/refresh.gif"), "Refresh"));

    var toolBar = new JToolBar();
    toolBar.setFloatable(false);
    add(toolBar, BorderLayout.NORTH);
    toolBar.add(runButton);
    toolBar.add(resumeButton);
    toolBar.add(suspendButton);
    toolBar.add(stopButton);
    toolBar.add(new JLabel(" "));
    toolBar.add(refreshButton);

    var topBottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    var top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    topBottom.setTopComponent(top);

    trace = new TraceGUI();
    top.setLeftComponent(trace);

    state = new StateGUI();
    top.setRightComponent(state);

    var bottom = new JSplitPane();
    topBottom.setBottomComponent(bottom);

    disassembler = new DisassemblerGUI();
//    disassembler.setAddress(0xE000);
    bottom.setLeftComponent(disassembler);

    memDump = new MemDumpGUI();
//    memDump.setAddress(0xE000);
    bottom.setRightComponent(memDump);

    add(topBottom);

    runButton.addActionListener(e -> {
      if (thread == null) {
        run();
      }
    });

    suspendButton.addActionListener(e -> {
      if (thread != null) {
        suspend();
      }
    });
    resumeButton.addActionListener(e -> {
      if (thread != null) {
        resume();
      }
    });

    stopButton.addActionListener(e -> {
      if (thread != null) {
        stop();
      }
    });

    refreshButton.addActionListener(e -> {
      if (thread != null) {
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
      c64 = new C64(new SerialClock(), true);
      cpu = (CPU6510Debugger) c64.getCpu();
      bus = c64.getCpuBus();

      trace.setCpu(cpu);
      state.setCpu(cpu);
      disassembler.setBus(bus);
      memDump.setBus(bus);

      thread = new Thread(() -> {
        try {
          logger.info("C64 has been started");
          c64.start();
        } catch (DebuggerExit e) {
          logger.info(e.getMessage());
        } catch (Exception e) {
          logger.error("C64 terminated abnormally", e);
        } finally {
          c64 = null;
          cpu = null;
          bus = null;
        }
      }, "C64 (debug)");
      thread.start();

      updateComponents();
    } catch (Exception e) {
      logger.error("Failed to start C64", e);
    }
  }

  /**
   * Suspend C64.
   */
  private void suspend() {
    new SwingWorker<>() {
      @Override
      protected Object doInBackground() {
        try {
          cpu.suspendAndWait();
        } catch (DebuggerExit e) {
          thread.interrupt();
          thread = null;
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
    new SwingWorker<>() {
      @Override
      protected Object doInBackground() {
        try {
          cpu.resume();
        } catch (DebuggerExit e) {
          thread.interrupt();
          thread = null;
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
    new SwingWorker<>() {
      @Override
      protected Object doInBackground() {
        try {
          cpu.stop();
          thread.join();
        } catch (InterruptedException e) {
          // ignore
        } finally {
          thread = null;
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
   * Update the state of the buttons.
   */
  private void updateButtons() {
    runButton.setEnabled(thread == null);
    suspendButton.setEnabled(thread != null && !cpu.isSuspended());
    resumeButton.setEnabled(thread != null && cpu.isSuspended());
    stopButton.setEnabled(thread != null);
  }


  /**
   * Notify gui to display current values.
   */
  private void updateComponents() {
    updateButtons();

    trace.stateChanged();
    state.stateChanged();
    disassembler.setAddress(cpu.getState().PC);
    disassembler.codeChanged();
    memDump.memoryChanged();
  }
}
