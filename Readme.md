# C64 Emulator

C64 emulator implemented using serial threads.

# Build

````shell script
./gradlew build
````

## Vice emu testprogs

The integration tests need the VICE testprogs (svn://svn.code.sf.net/p/vice-emu/code/testprogs).
They are included as git submodule src/test/resources/vice-emu-testprogs from a git mirror of them.
Fetch them after cloning (or clone with --recurse-submodules):

    $ git submodule update --init
    
### TODOs

   * Defaults for output ports, if masks is not $FF
   * "Next tick" events for clock.
   * Fix timing of writes -> Execute at start of next tick.
