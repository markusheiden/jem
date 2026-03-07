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

# Run the emulator
./gradlew run

# Coverage report (HTML + XML)
./gradlew jacocoTestReport

# Check for dependency updates
./gradlew dependencyUpdates
```

The shadow JAR is the primary artifact; the plain JAR is disabled. Tests ignore failures (`ignoreFailures = true`) so the build won't fail on test errors.

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
| `threads/` | OS threads: Sequential, Parallel variants (Spin, Yield, Notify, Barrier) |
| `loom/` | Java Virtual Threads (Project Loom): Fiber variants (Yield, Park, Latch, Executor) |

Each `C64*.java` in `de.heiden.jem.models.c64` is an entry point that wires up the C64 hardware with a specific clock.

### C64 Hardware Components (`de.heiden.jem.models.c64.components`)

- `C64` — top-level wiring: instantiates CPU, VIC, CIA×2, RAM, ROM, keyboard and connects them
- `cpu/CPU6510` — MOS 6510 CPU; `CPU6510Debugger` adds trace/monitor support
- `cpu/C64Bus` — memory map / PLA logic routing reads/writes to the right chip
- `vic/VIC6569PAL` — PAL VIC-II video chip
- `cia/CIA6526` — CIA timer/IO chips
- `memory/` — RAM, ROM, ColorRAM
- `keyboard/Keyboard` — keyboard matrix
- `patch/` — ROM patches that intercept KERNAL calls (e.g. load from host filesystem, detect program end)

### GUIs

- `gui/javafx/` — JavaFX frontend (default for `C64Serial`)
- `gui/swing/` — Swing frontend with integrated debugger/monitor/trace views

### Integration Tests

Tests in `de.heiden.jem.models.c64.vice` run real C64 test programs (`.prg` files) and detect pass/fail by reading the VIC border color (green = pass, red/light-red = fail). They depend on VICE emu test programs checked out at:

```
src/test/resources/vice-emu-testprogs
svn checkout svn://svn.code.sf.net/p/vice-emu/code/testprogs src/test/resources/vice-emu-testprogs
```

`AbstractTest` is the base class for all C64 integration tests and provides helpers for loading/running programs, waiting for conditions, and reading the screen/console.

### Dependencies

Managed via `gradle/libs.versions.toml`. External dependencies not in Spring Boot BOM:
- `serialthreads` — serial threads library (composite build at `../serialthreads`)
- `c64dt` — C64 tools (assembler, disassembler, charset, disk image; composite build at `../c64dt`)
- JavaFX via `org.openjfx.javafxplugin`
