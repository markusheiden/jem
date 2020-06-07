# C64 Emulator

C64 emulator implemented using serial threads.

# Build

````shell script
./gradlew build
````

## Vice emu testprogs

Checkout svn://svn.code.sf.net/p/vice-emu/code/testprogs into src/test/vice-emu-testprogs to make integration test work.

    $ svn checkout svn://svn.code.sf.net/p/vice-emu/code/testprogs src/test/resources/vice-emu-testprogs
    
### TODOs

   * Defaults for output ports, if masks is not $FF
   * "Next tick" events for clock.
   * Fix timing of writes -> Execute at start of next tick.
