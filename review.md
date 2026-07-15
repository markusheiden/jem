# Review: serialthreads bytecode transformation — runtime efficiency

**Scope:** In-depth review of the bytecode manipulation code in `../serialthreads`, focused on the efficiency of the *generated* bytecode, i.e. reducing the runtime of an enhanced application. jem (C64 emulator) is used as the reference workload. All file references are relative to `../serialthreads/src/main/java/org/serialthreads/` unless stated otherwise.

**Reviewed:** `transformer/strategies/*` (esp. `frequent4`), `transformer/code/*`, `transformer/analyzer/*`, `context/Stack`, `context/StackFrame`, `context/SerialThreadExecutor`, plus jem's usage (`SerialClock`, `CPU6510`, `Tick`, VIC components).

---

## 1. Cost model: how the generated code runs in jem

jem uses `Strategies.DEFAULT` = **FREQUENT4**. jem's `SerialClock` re-enters every component's `run()` on **every clock tick**, and `Tick.waitForTick()` is an `@Interrupt`, which *always* yields. So per tick and per component the enhanced code:

1. **Unwinds:** at the interrupt site, the innermost method captures its live locals into its `StackFrame` and returns `true`; every caller sees `true` (`IFEQ` check after the call), captures *its* live state, sets `frame.method`, and returns `true` — all the way up to `run()`.
2. **Rewinds:** on the next tick, `run()` re-dispatches via `TABLESWITCH` on `frame.method`, invokes the `$$…$$` copy method of the callee, which re-dispatches, restores locals, … down to the innermost frame, which continues after the interrupt.

For a typical CPU tick the chain is ~5 levels deep (`run → execute → Opcode.execute → readBytePC → read`). Estimated **~150–200 instructions of capture/restore/dispatch machinery per tick vs. a few dozen instructions of actual emulation logic** — the machinery dominates the emulator's runtime, so every constant shaved off this path is visible wall-clock time.

The design is already strong:

- Liveness-based capture (`ExtendedAnalyzer` backflow analysis / `ExtendedFrame.neededLocals`) — dead locals are neither captured nor restored.
- Constant and duplicate-local elision in `CompactingStackCode` (constants are re-pushed, aliased locals copied instead of restored).
- Fast fields for the first 8 slots per type (`StackFrame.stackInt0…`, `localObject0…`).
- Tail-call elision (`TAG_TAIL_CALL` skips capture/restore entirely).
- `setMethod` skipped for methods with ≤ 1 interruptible call; single-restore dispatch degenerates to a `GOTO`.

The findings below are the remaining gaps, ranked by expected impact for jem.

---

## 2. High-impact findings

### 2.1 Tail-call detection is defeated by javac's `ISTORE; ILOAD; IRETURN` pattern

`AbstractMethodTransformer.analyze()` (`strategies/AbstractMethodTransformer.java:151`) tags a call `TAG_TAIL_CALL` only when the *very next* real instruction is a return. The idiomatic Java

```java
int result = read(pc);
return result;          // jem: CPU6510.readBytePC(), line 3590
```

compiles to `ISTORE 2; ILOAD 2; IRETURN`, so the tail call is missed. Consequences for such a method:

- `needsFrame()` (`strategies/frequent4/MethodTransformer.java:351`) returns `true` → every entry pays `previousFrame.next` (plus null-check/`addFrame` branch) instead of reusing the previous frame.
- Capture/restore code is generated although a genuine tail call would skip both (`captureFrame`/`restoreFrame` emit nothing for `TAG_TAIL_CALL`, `code/CompactingStackCode.java:34,104`).

**Recommended fix (transformer):** recognize `store x; load x; return` (with only pseudo-nodes in between) as a tail call.

**Better, semantic fix:** make `needsFrame()` liveness-based instead of syntactic — the javadoc itself calls the third condition "somewhat suboptimal". The liveness data already exists: if a method has exactly one interruptible call *and* `captureFrame` would emit nothing (no live locals, no saved stack), no frame is needed regardless of call shape. This also covers `void` methods ending in a tail *interrupt* (jem: `CPU6510.write()` ending in `tick.waitForTick()`; `MemAccessUnit.waitForTick()`), because interrupts never receive the tail tag: the `if/else if` in `analyze()` (`AbstractMethodTransformer.java:149-154`) makes `TAG_INTERRUPT` and `TAG_TAIL_CALL` mutually exclusive even when the interrupt is in tail position and its capture set is empty.

### 2.2 `previousFrame.owner = this` is written on every entry, not on capture

`OriginalMethodTransformer.createRestoreHandlerMethod()` (`strategies/frequent4/OriginalMethodTransformer.java:64`) stores the owner eagerly in the prologue of every non-static interruptible method. During normal (non-yielding) execution this is a dead store — the owner is only read by the caller's restore code (`pushOwner`, used in `callCopyMethod`). jem's opcode implementations call several CPU helper methods per tick, so this is multiple wasted `PUTFIELD`s per tick. (Field stores are observable side effects; the JIT cannot eliminate them.)

**Recommended fix:** emit the owner store inside the capture blocks instead — both the interrupt block (`createCaptureAndRestoreCodeForInterrupt`) and the serializing branch after each method call (`createCaptureAndRestoreCodeForMethod`). Those blocks execute at most once per tick per level, and both have `ALOAD 0` and `localPreviousFrame` available. Same instruction count, strictly fewer executions (captures ≤ entries).

### 2.3 Return values funnel through `Stack.returnXXX` with a `frame.stack` indirection at every site

Because FREQUENT4 methods return the serializing flag, real return values travel via `thread.returnInt` etc. Every non-void interruptible call — *also on the never-interrupted fast path* — pays:

- Callee return (`replaceReturns`, `strategies/frequent4/MethodTransformer.java:155`):
  `ALOAD frame; GETFIELD stack; SWAP; PUTFIELD returnInt; ICONST_0; IRETURN`
- Caller (`createCaptureAndRestoreCodeForMethod`, line 264):
  `IFEQ; ALOAD frame; GETFIELD stack; GETFIELD returnInt`

The `GETFIELD stack` pairs are real memory traffic: the opcode-level `INVOKEINTERFACE Opcode.execute$$…$$` is megamorphic (256 opcode classes in `CPU6510`), so the JIT cannot inline across it and cannot forward these stores to loads.

Note that **FREQUENT3 already passes the `Stack` as a parameter** (`strategies/frequent3/MethodTransformer.java:110`, `popReturnValue(localThread)`) — one fewer `GETFIELD` per site, at the price of one extra call argument (which lives in a register). FREQUENT4 traded that away.

**Options, cheapest first:**

1. In methods with ≥ 2 return-value sites, hoist `frame.stack` into a synthetic local once in the prologue.
2. Reintroduce the thread parameter. This can be A/B-tested *today* in jem by benchmarking `Strategies.FREQUENT3` vs `FREQUENT4` in `C64Serial`.
3. Best but invasive (a "FREQUENT5"): **hybrid return convention** — void methods keep returning the flag (optimal, zero extra cost); non-void methods return their *real* value normally and the caller checks `thread.serializing` (one predictable `GETFIELD` + branch, with the thread cached in a local/parameter). This removes the entire `returnXXX` dance from the fast path of value-returning methods (`read()`, `readBytePC()`), which dominate a CPU emulator.

### 2.4 Null-clearing of object slots on every restore

`clear == true` for references roughly doubles the restore cost of every object local/stack slot:

- `popLocalFast` (`code/AbstractValueCode.java:285`): `ALOAD frame; GETFIELD localObjectN; CHECKCAST; ASTORE` **plus** `ALOAD frame; ACONST_NULL; PUTFIELD localObjectN`.
- `popStackFast`/`popStackSlow` and `popReturnValue` carry analogous extra sequences (with `DUP`/`SWAP` juggling in the slow path).

The `StackFrame` TODOs (`context/StackFrame.java:266,330`) show this was already suspected. Since frames are pooled and slots are overwritten by the next capture, the only benefit is slightly earlier GC eligibility of the referenced object — and in jem the captured objects (`state`, components, opcode receivers) are immortal anyway.

**Recommended fix:** drop the clearing, or gate it behind a transformer option (default off). Easiest pure win in this list: jem's `run()` loops capture/restore object locals like `state` every single tick.

### 2.5 `StackFrame` is enormous

Each frame eagerly allocates **ten arrays of 64 elements** (~2.5 KB) that the generated fast path never touches (fast fields cover the first 8 slots per type), on top of 80 scalar "fast" fields — the object spans many cache lines, and capture/restore is exactly the code that walks it every tick.

**Recommended fixes:**

- Allocate the overflow arrays lazily — the slow-path push/pop methods and `resize()` are the only consumers.
- Shrink `DEFAULT_FRAME_SIZE` (64) drastically; methods with > 8 live locals *of one type* across a yield are rare.
- Reconsider `FAST_FRAME_SIZE = 8` for all five types × stack+locals (= 80 fields, 500+ bytes per frame); 4 may be the cache-friendlier sweet spot. Worth measuring — footprint effects don't show in instruction counts.

### 2.6 Minor / likely JIT-absorbed (for completeness)

- **Dead receiver push at interrupt sites.** `replace(methodCall, …)` in `createCaptureAndRestoreCodeForInterrupt` removes only the call instruction; the receiver push (`ALOAD 0; GETFIELD tick`) survives and executes every tick, its result silently discarded by the following `IRETURN` (legal: the operand stack need not be empty at return). C2 usually dead-code-eliminates it after inlining, but it inflates bytecode.
- **`getNextFrame(..., addIfNotPresent=true)`** (`code/AbstractStackCode.java:139`) re-checks `next == null` with a `DUP/IFNONNULL/POP` dance on every entry. Maintaining an "always one spare frame" invariant in `Stack`/`StackFrame.addFrame()` would remove the branch; it is well-predicted, so low priority.
- **Bytecode size feeds JIT inlining thresholds** (`FreqInlineSize` ≈ 325 bytes for hot methods). All injected code inflates transformed methods; keeping injected sequences minimal (findings 2.1–2.4) compounds, because inlining is what lets C2 elide flag checks and forward field traffic.
- `fixMaxs()` overestimates `maxStack`; harmless at runtime (`COMPUTE_FRAMES` recomputes maxs), only relevant for the `CheckClassAdapter` path.
- `AbstractTransformer` enables `CheckClassAdapter` + double disassembly whenever debug logging is on (`check = logger.isDebugEnabled()`, `strategies/AbstractTransformer.java:67`) — a startup-time trap worth documenting: never benchmark with debug logging enabled.

---

## 3. Latent correctness bugs found along the way

These do not affect jem today (its interruptible methods return only `void`/`int`), but produce broken bytecode when hit:

1. **`AbstractValueCode.popReturnValue()` is wrong for reference types** (`code/AbstractValueCode.java:364`). With `clear` set, the emitted sequence is `DUP; GETFIELD returnObject; CHECKCAST; ACONST_NULL; PUTFIELD`. At the `PUTFIELD`, the objectref on the stack is the *returned value*, not the thread — any interruptible method with a reference return type called in non-tail position yields a `VerifyError` (or `COMPUTE_FRAMES` failure) at transform/load time. Fix: `SWAP` after the `GETFIELD` (and reorder the cast). `pushReturnValue()` handles the analogous case correctly with `DUP_X2/POP`.
2. **`pushStackFast`/`pushStackSlow` use `SWAP` unconditionally** (`code/AbstractValueCode.java:158,167`). `SWAP` is illegal on category-2 values, so capturing a `long`/`double` that is live *on the operand stack* at a yield point produces invalid bytecode. Fix: use the `DUP_X2; POP` idiom (as in `pushReturnValue`) when `size == 2`.
3. **Interrupt replacement assumes an empty expression stack.** If an `@Interrupt` call ever sits above a non-empty evaluation stack, `saveStack` misaligns by one slot because the dangling receiver is still on the real stack while `frameAfter` doesn't contain it. Unreachable from javac-compiled statement-position calls, but worth an assertion on `metaInfo.frameAfter` in `createCaptureAndRestoreCodeForInterrupt`.

---

## 4. Bigger-picture option: resume-innermost / trampolining

Since an interrupt *always* yields, each tick pays O(call depth) invokes + `TABLESWITCH` dispatches to rebuild the Java stack, then O(depth) returns to tear it down. The unused `StackFrame.methodHandle` field suggests this was already considered: store a `MethodHandle` (or generated per-site continuation entry) per frame, have the executor invoke the *innermost* frame directly, and trampoline upward only when a method actually returns. That converts the per-tick rewind from O(depth) to O(1) at the cost of slower returns from interruptible methods. Given jem ticks on every memory access but returns from interruptible methods comparatively rarely, the trade likely pays off — but it is a new strategy, not a tweak.

---

## 5. jem-side quick wins (no library changes)

- Write `return read(pc);` instead of `int result = read(pc); return result;` in `CPU6510.readBytePC()` (and similar) — with the current transformer this literally decides whether the method gets a frame (finding 2.1).
- Remove one-line `@Interruptible` wrappers like `MemAccessUnit.waitForTick()` — each wrapper level costs a full frame's entry/capture/restore/dispatch every tick.
- Avoid caching fields in locals across yields (`var state = this.state;` in `CPU6510.run()`): the cached local must be captured and restored every tick; re-reading the field per use is cheaper under this transformation.
- Benchmark `Strategies.FREQUENT3` vs `FREQUENT4` in `C64Serial` — a one-line change that directly measures the return-value-convention question (finding 2.3).

---

## 6. Suggested measurement approach

1. Baseline: jem Lorenz suite wall-clock (`LorenzTest`) and/or a fixed-tick `doRun(int ticks)` micro-run, plus the existing `../serialthreads/src/test/performance` counter tests.
2. Apply findings in isolation (2.4 null-clearing and 2.2 owner store are lowest-risk), re-measure each.
3. Verify transformed bytecode with the existing `Debugger`/`CheckClassAdapter` path (`transformer.check()`), and keep debug logging off while timing.

## 7. Priority summary

| # | Finding | Type | Effort | Expected win (jem) |
|---|---------|------|--------|--------------------|
| 2.4 | Drop null-clearing on restore | perf | low | medium |
| 2.2 | Owner store: entry → capture | perf | low | medium |
| 2.1 | Tail-call detection / liveness-based `needsFrame()` | perf | medium | high |
| 2.3 | Return-value convention (`frame.stack` hoist / thread param / hybrid) | perf | medium–high | high |
| 2.5 | `StackFrame` footprint (lazy arrays, sizes) | perf | low–medium | small–medium |
| 3.1 | `popReturnValue` reference bug | correctness | low | — |
| 3.2 | `SWAP` on category-2 stack values | correctness | low | — |
| 4 | Trampolining strategy | perf | very high | potentially large |