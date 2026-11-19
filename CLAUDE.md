# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```shell
# Build
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "de.heiden.jem.models.c64.vice.LorenzTest"

# Run just some groups of tests (lorenz, interrupts, c64doc, cia, vic, cpu, clock; none for the tests without a group)
./gradlew test -Pgroups=cpu,cia

# Run the emulator (byte code transformation via TransformingClassLoader)
./gradlew run

# Run the emulator as NTSC C64 (pal is the default, ntsc_old is the VIC 6567R56A)
./gradlew run -Djem.video=ntsc

# Run the emulator (byte code transformation via the serial threads agent)
./gradlew runAgent

# Coverage report (HTML + XML)
./gradlew jacocoTestReport

# Check for dependency updates
./gradlew dependencyUpdates

# Benchmark the clocks (each clock in its own JVM)
./gradlew clockBenchmark

# Test the sequential clocks on a single carrier thread (part of check)
./gradlew singleCarrierTest
```

The shadow JAR is the primary artifact; the plain JAR is disabled. Test failures fail the build (`ignoreFailures = false`).

## Architecture

This is a Commodore 64 emulator written in Java, built around a **serial threads** concurrency model.

### Serial Threads

The key architectural concept is that each hardware component (CPU, VIC, CIA) runs as a coroutine-style "serial thread". The `serialthreads` library (included build at `../serialthreads`) transforms bytecode at load time so that components can `yield` control after each clock tick without using OS threads. This is done via:

- `TransformingClassLoader` — loads and transforms classes at runtime (used in `C64Serial`, the main entry point)
- `@Transform` annotation — triggers transformation in tests

All emulated component logic that participates in the clock must be within packages matching `de.heiden.jem`.

### Clock Implementations

Multiple clock strategies exist under `de.heiden.jem.components.clock`:

| Package | Strategy |
|---|---|
| `serialthreads/` | Serial coroutines via bytecode transformation (default) |
| `threads/` | One thread per component: Sequential, Parallel variants (Spin, Yield, Notify, Barrier) |
| `loom/` | Java Virtual Threads (Project Loom): Fiber variants (Yield, Park, Latch, Queue) and `ContinuationClock` |

The emulated components are not thread-safe. The Parallel clocks and `SequentialSpinClock` execute the components with multiple threads, so they are just for reference. The Sequential and Fiber clocks execute all components and events with the same thread, if their entry point calls `FiberScheduler.useSingleCarrier()` (JVM-wide single carrier thread for virtual threads). `ContinuationClock` executes everything with the thread running the clock, it uses the JDK-internal continuations (`--add-exports java.base/jdk.internal.vm=ALL-UNNAMED`, configured in `build.gradle.kts`).

Each `C64*.java` in `de.heiden.jem.models.c64` is an entry point that wires up the C64 hardware with a specific clock.

### C64 Hardware Components (`de.heiden.jem.models.c64.components`)

- `C64` — top-level wiring: instantiates CPU, VIC, CIA×2, RAM, ROM, keyboard and connects them; configured via `C64.builder(clock)` (video system, old/new CIAs, power line frequency, debugger CPU; default PAL, 50 Hz, old CIAs)
- `cpu/CPU6510` — MOS 6510 CPU; `CPU6510Debugger` adds trace/monitor support
- `cpu/C64Bus` — memory map / PLA logic routing reads/writes to the right chip
- `VideoSystem` — PAL, NTSC or old NTSC: selects the VIC and the frequencies of the CPU clock and the power line
- `vic/VIC` — VIC-II video chip: `VIC6569PAL`, `VIC6567R8NTSC` and `VIC6567R56ANTSC` define its timing
- `cia/CIA6526` — CIA timer/IO chips
- `memory/` — RAM, ROM, ColorRAM
- `keyboard/Keyboard` — keyboard matrix
- `patch/` — ROM patches that intercept KERNAL calls (e.g. load from host filesystem, detect program end)

### GUIs

- `gui/javafx/` — JavaFX frontend (default for `C64Serial`)
- `gui/swing/` — Swing frontend with integrated debugger/monitor/trace views

### Integration Tests

Tests in `de.heiden.jem.models.c64.vice` run real C64 test programs (`.prg` files) and detect pass/fail by reading the VIC border color (green = pass, red/light-red = fail). They depend on the VICE emu test programs in the git submodule `src/test/resources/vice-emu-testprogs`, a git mirror of the VICE SVN testprogs (each commit names its SVN revision in its `git-svn-id`). The submodule is pinned, because the test exclusions depend on the versions of the test programs:

```
# Fetch after cloning (or clone with --recurse-submodules)
git submodule update --init

# Update to the latest test programs, re-run the tests, then commit the new submodule commit
git submodule update --remote src/test/resources/vice-emu-testprogs
```

`AbstractTest` is the base class for all C64 integration tests and provides helpers for loading/running programs, waiting for conditions, and reading the screen/console.

### Dependencies

Managed via `gradle/libs.versions.toml`. External dependencies not in Spring Boot BOM:
- `serialthreads` — serial threads library (composite build at `../serialthreads`)
- `c64dt` — C64 tools (assembler, disassembler, charset, disk image; composite build at `../c64dt`)
- JavaFX via `org.openjfx.javafxplugin`
