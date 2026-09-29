package org.curioone.control.support;

import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DigitalChannelController;
import com.qualcomm.robotcore.hardware.HardwareDevice;

/** Recording fakes for the SDK devices CurioControl wraps, for tests. */
public final class FakeSdkDevices {

    private FakeSdkDevices() {
        throw new AssertionError("FakeSdkDevices is a factory and must not be instantiated.");
    }

    /**
     * A {@link DigitalChannel} whose state a test sets directly.
     *
     * <p>Both {@code setMode} overloads are part of the SDK interface; the older one is deprecated
     * upstream, so implementing it is unavoidable and is suppressed locally.
     */
    @SuppressWarnings("deprecation")
    public static final class FakeDigitalChannel implements DigitalChannel {

        private boolean state;

        private Mode mode = Mode.INPUT;

        /** Creates a channel reading low. */
        public FakeDigitalChannel() {
            this(false);
        }

        /**
         * Creates a channel in a known state.
         *
         * @param state the line state
         */
        public FakeDigitalChannel(boolean state) {
            this.state = state;
        }

        @Override
        public boolean getState() {
            return state;
        }

        @Override
        public void setState(boolean state) {
            this.state = state;
        }

        @Override
        public Mode getMode() {
            return mode;
        }

        @Override
        public void setMode(Mode mode) {
            this.mode = mode;
        }

        @Override
        public void setMode(DigitalChannelController.Mode mode) {
            this.mode = mode == DigitalChannelController.Mode.OUTPUT ? Mode.OUTPUT : Mode.INPUT;
        }

        @Override
        public HardwareDevice.Manufacturer getManufacturer() {
            return HardwareDevice.Manufacturer.Unknown;
        }

        @Override
        public String getDeviceName() {
            return "FakeDigitalChannel";
        }

        @Override
        public String getConnectionInfo() {
            return "fake";
        }

        @Override
        public int getVersion() {
            return 1;
        }

        @Override
        public void resetDeviceConfigurationForOpMode() {
            // Nothing to reset.
        }

        @Override
        public void close() {
            // Nothing to close.
        }
    }

    /**
     * An {@link AnalogInput} with a settable voltage and a fixed configured maximum.
     *
     * <p>Subclasses the SDK class rather than implementing its interface, because the SDK declares
     * {@code AnalogInput} as a concrete class. The real constructor needs an {@code
     * AnalogInputController} and a channel number from a live hub, so it is called with a null
     * controller: every method that could touch hardware is overridden below, and the superclass
     * only stores the arguments. Confined to test support, where it is a fair trade for not needing
     * a hub.
     */
    public static final class FakeAnalogInput extends AnalogInput {

        private double voltage;

        private final double maxVoltage;

        /** Creates an input with a 3.3 V range, the FTC default. */
        public FakeAnalogInput() {
            this(3.3);
        }

        /**
         * Creates an input with a specific configured maximum.
         *
         * @param maxVoltage the channel's configured maximum
         */
        public FakeAnalogInput(double maxVoltage) {
            super(null, 0);
            this.maxVoltage = maxVoltage;
        }

        /**
         * Sets the reading.
         *
         * @param voltage the voltage to report
         */
        public void setVoltage(double voltage) {
            this.voltage = voltage;
        }

        @Override
        public double getVoltage() {
            return voltage;
        }

        @Override
        public double getMaxVoltage() {
            return maxVoltage;
        }

        @Override
        public HardwareDevice.Manufacturer getManufacturer() {
            return HardwareDevice.Manufacturer.Unknown;
        }

        @Override
        public String getDeviceName() {
            return "FakeAnalogInput";
        }

        @Override
        public String getConnectionInfo() {
            return "fake";
        }

        @Override
        public int getVersion() {
            return 1;
        }

        @Override
        public void resetDeviceConfigurationForOpMode() {
            // Nothing to reset.
        }

        @Override
        public void close() {
            // Nothing to close.
        }
    }
}
