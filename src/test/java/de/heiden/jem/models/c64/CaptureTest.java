package de.heiden.jem.models.c64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Run a program, e.g. a demo, and capture frames as PNG and VIC/CIA/CPU state for visual inspection and debugging.
 * Enabled only if the program is configured, e.g.
 * {@code ./gradlew capture -Dcapture.program=demos/lft-nine.prg -Dcapture.seconds=200 -Dcapture.step=4}.
 * <ul>
 *     <li>{@code capture.program}: The program (.prg) to run.</li>
 *     <li>{@code capture.dir}: The directory to write the frames to. Defaults to build/capture/[program name].</li>
 *     <li>{@code capture.seconds}: The number of seconds to run the program. Defaults to 20.</li>
 *     <li>{@code capture.step}: The number of seconds between two frames. Defaults to 2.</li>
 * </ul>
 */
@EnabledIfSystemProperty(named = "capture.program", matches = ".+")
class CaptureTest extends AbstractTest {
    /**
     * Pepto PAL palette.
     */
    private static final int[] PALETTE = {
            0x000000, 0xFFFFFF, 0x68372B, 0x70A4B2, 0x6F3D86, 0x588D43, 0x352879, 0xB8C76F,
            0x6F4F25, 0x433900, 0x9A6759, 0x444444, 0x6C6C6C, 0x9AD284, 0x6C5EB5, 0x959595
    };

    @Test
    @Timeout(3600)
    void capture() throws Exception {
        var program = Paths.get(System.getProperty("capture.program")).toAbsolutePath();
        var name = program.getFileName().toString().replaceFirst("\\.prg$", "");
        var out = Paths.get(System.getProperty("capture.dir", "build/capture/" + name));
        Files.createDirectories(out);
        var seconds = Integer.getInteger("capture.seconds", 20);
        var step = Integer.getInteger("capture.step", 2);

        loadAndRun(program);
        for (var s = step; s <= seconds; s += step) {
            waitSeconds(step);
            save(out.resolve("frame-%03d.png".formatted(s)));
            dumpState(s);
        }
    }

    private void dumpState(int s) {
        var sb = new StringBuilder("t=" + s + " VIC:");
        for (var r = 0; r < 0x2F; r++) {
            sb.append(r % 16 == 0 ? "\n" : " ").append("%02X".formatted(c64.getVic().read(0xD000 + r)));
        }
        sb.append("\nDD00=%02X DD02=%02X DC0E=%02X 01=%02X".formatted(c64.getBus().read(0xDD00), c64.getBus().read(0xDD02), c64.getBus().read(0xDC0E), c64.getBus().read(0x01)));
        sb.append("\nCPU ").append(c64.getCpu().getState());
        System.out.println(sb);
    }

    private void save(Path file) throws Exception {
        var display = c64.getDisplayUnit();
        var screen = display.display();
        var width = display.getWidth();
        var height = display.getHeight();
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                image.setRGB(x, y, PALETTE[screen[display.getOffset() + y * display.getLineLength() + x] & 0x0F]);
            }
        }
        ImageIO.write(image, "png", file.toFile());
    }
}
