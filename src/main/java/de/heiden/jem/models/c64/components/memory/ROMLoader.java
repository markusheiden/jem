package de.heiden.jem.models.c64.components.memory;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Loader for ROM contents.
 */
public class ROMLoader {
    /**
     * BASIC ROM.
     *
     * @param filename
     *         Filename of ROM image.
     */
    public static ROM basic(String filename) throws Exception {
        var content = load(0x2000, filename);
        return new ROM(content);
    }

    /**
     * Kernel ROM.
     *
     * @param filename
     *         Filename of ROM image.
     */
    public static ROM kernel(String filename) throws Exception {
        var content = load(0x2000, filename);
        return new ROM(content);
    }

    /**
     * Charset ROM.
     *
     * @param filename
     *         Filename of ROM image.
     */
    public static ROM character(String filename) throws Exception {
        var content = load(0x1000, filename);
        return new ROM(content); // TODO correct?
    }

    /**
     * Charset ROM.
     *
     * @param filename
     *         Filename of ROM image.
     */
    public static ROM pla(String filename) throws Exception {
        var content = load(0x1000, filename);
        return new ROM(content);
    }

    /**
     * Load ROM content from file.
     *
     * @param length
     *         Of expected content.
     * @param filename
     *         Filename of content.
     * @throws Exception
     */
    protected static byte[] load(int length, String filename) throws Exception {
        try (var stream = ROMLoader.class.getResourceAsStream(filename)) {
            var result = new byte[length];
            var size = stream.read(result);

            if (size != length) {
                throw new Exception("ROM image '%s' is too short".formatted(filename));
            }

            return result;
        } catch (FileNotFoundException e) {
            throw new Exception("ROM image '%s' not found".formatted(filename), e);
        } catch (IOException e) {
            throw new Exception("Unable to read ROM image %s'".formatted(filename), e);
        }
    }

    //
    // Public constants.
    //

    public static final String DEFAULT_BASIC = "/roms/basic/901226-01.bin";
    public static final String DEFAULT_KERNEL = "/roms/kernel/901227-03.bin";
    public static final String DEFAULT_CHARACTER = "/roms/character/901225-01.bin";
    public static final String DEFAULT_PLA = "/roms/pla/pla.bin";
}
