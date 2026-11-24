package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.ports.OutputPort;

/**
 * VIC bus.
 */
public class VICBus implements BusDevice {
    /**
     * 64kB RAM.
     */
    private final BusDevice ram;

    /**
     * Character ROM.
     */
    private final BusDevice character;

    /**
     * VIC base address in CPU address space.
     */
    private int vicBase;

    /**
     * Byte read last.
     */
    private int data = 0xFF;

    /**
     * Constructor.
     *
     * @param ram
     *         RAM.
     * @param character
     *         Char ROM.
     * @param cia2PortA
     *         Port A of CIA 2 for VIC base address.
     */
    public VICBus(BusDevice ram, BusDevice character, OutputPort cia2PortA) {
        this.ram = ram;
        this.character = character;

        // Bits not driven by the CIA (input) are pulled up, e.g. demos switch the bank via the data direction register.
        cia2PortA.addOutputPortListener((value, mask) -> vicBase = (~(value | ~mask) & 0x03) << 14);
    }

    /**
     * Read byte from bus device.
     *
     * @param address
     *         Address to read byte from.
     * @require address >= 0x0000 && address < 0x4000
     * @ensure result >= 0x00 && result < 0x100
     */
    @Override
    public final int read(int address) {
        assert address >= 0x0000 && address < 0x4000 : "address >= 0x0000 && address < 0x4000";

        var cpuAddress = vicBase + address;
        data = ((cpuAddress & 0x7000) == 0x1000 ? character : ram).read(cpuAddress);
        return data;
    }

    /**
     * Open bus: At addresses of the CPU bus without a device, the CPU reads the byte the VIC has read last.
     */
    public BusDevice openBus() {
        return new BusDevice() {
            @Override
            public int read(int address) {
                return data;
            }

            @Override
            public void write(int value, int address) {
                // Ignored.
            }
        };
    }

    /**
     * Write byte to bus device.
     *
     * @param value
     *         Byte to write.
     * @param address
     *         Address to write byte to.
     * @require value >= 0x00 && value < 0x100
     * @require address >= 0x0000 && address < 0x10000
     */
    @Override
    public final void write(int value, int address) {
        assert false : "VIC does not write anything";
    }
}
