package de.heiden.jem.tmpshx;

import de.heiden.jem.components.clock.ClockEvent;
import de.heiden.jem.models.c64.components.TestC64;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Temporary: Start shx-test at exact ticks.
 */
class PhaseTest {
    @Test
    void phases() throws Exception {
        var program = Files.readAllBytes(Path.of(System.getenv("PRG")));
        var from = Long.parseLong(System.getenv().getOrDefault("FROM", "3000000"));
        var step = Long.parseLong(System.getenv().getOrDefault("STEP", "1"));
        var count = Integer.parseInt(System.getenv().getOrDefault("COUNT", "8"));
        var secs = Long.parseLong(System.getenv().getOrDefault("SECS", "20"));
        for (var i = 0; i < count; i++) {
            var start = from + i * step;
            var c64 = new TestC64();
            var bus = c64.getBus();
            c64.getClock().addClockEvent(start, new ClockEvent("Start") {
                @Override
                public void execute(long tick) {
                    var end = 0x0801 + program.length - 2;
                    for (var j = 2; j < program.length; j++) {
                        bus.write(program[j] & 0xFF, 0x07FF + j);
                    }
                    for (var addr : new int[]{0x2D, 0x2F, 0x31, 0xAE}) {
                        bus.write(end & 0xFF, addr);
                        bus.write(end >> 8, addr + 1);
                    }
                    var run = "RUN\r";
                    for (var j = 0; j < run.length(); j++) {
                        bus.write(run.charAt(j), 0x0277 + j);
                    }
                    bus.write(run.length(), 0xC6);
                }
            });
            var thread = new Thread(c64::start, "phase" + start);
            thread.setDaemon(true);
            thread.start();
            while (c64.getClock().getTick() < start + secs * 1000000) {
                Thread.sleep(20);
            }
            var border = bus.read(0xD020) & 0x0F;
            var sb = new StringBuilder();
            for (var j = 0; j < 40; j++) {
                sb.append(String.format("%02x", bus.read(0x0450 + j)));
            }
            System.out.println("PHASE " + start + " border " + border + " " + sb);
            c64.stop();
            thread.join(2000);
        }
    }
}
