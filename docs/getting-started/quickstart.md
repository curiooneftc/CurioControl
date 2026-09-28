# Quickstart

> Status: this page describes the target v1.0.0 API. The v0.1.0 release implements a subset of it;
> see [the phases document](../PHASES.md) for exactly what exists in which release.

The goal: a mecanum robot that drives, in about ten minutes.

## Before you start

- A configured robot with four drive motors, named consistently in the Robot Controller app
  (`frontLeft`, `frontRight`, `backLeft`, `backRight`).
- [CurioControl installed](installation.md).

## 1. The OpMode

```java
package org.curioone.robot.opmode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.curioone.control.core.CurioOpMode;
import org.curioone.control.core.CurioRobot;

@TeleOp(name = "Main TeleOp")
public class MainTeleOp extends CurioOpMode {

    @Override
    public void initRobot() {
        robot = new CurioRobot(hardwareMap);
    }

    @Override
    public void runRobot() {
        robot.drive()
                .mecanum(
                        gamepad1.left_stick_x,   // strafe
                        gamepad1.left_stick_y,   // forward
                        gamepad1.right_stick_x); // rotation
    }
}
```

That is the whole TeleOp. The framework knows how to turn three gamepad axes into four motor
powers; the OpMode only says what the driver is asking for.

## 2. Deploy and run

1. Build and deploy the TeamCode module from Android Studio.
2. On the Robot Controller, select **Main TeleOp** and press **Start**.
3. Drive.

If a motor is missing or misnamed, the framework says so explicitly rather than silently
substituting a device — see [competition safety](../concepts/architecture.md#fail-predictably).

## 3. Add a mechanism

Mechanisms belong in subsystems. A subsystem owns its hardware and exposes an intent, not a
sequence of motor calls.

```java
package org.curioone.robot.subsystem;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.curioone.control.hardware.Motor;
import org.curioone.control.core.Subsystem;

public class Arm extends Subsystem {

    private final Motor motor;

    public Arm(HardwareMap hardwareMap) {
        this.motor = new Motor(hardwareMap, "arm");
    }

    public void moveTo(int ticks) {
        motor.setTargetPosition(ticks);
    }
}
```

Register it once, then use it everywhere:

```java
robot.registerSubsystem(new Arm(hardwareMap));

// in runRobot()
if (gamepad1.a) {
    robot.arm().moveTo(RobotConfig.Arm.HIGH);
}
```

Notice there is no motor lookup in the OpMode and no tick math. The subsystem owns both.

## 4. Keep configuration out of the code

Wheel diameters, gear ratios, and joint positions belong to *your* robot, not to the framework.

```java
public final class RobotConfig {
    private RobotConfig() {}

    public static final class Drive {
        public static final double WHEEL_DIAMETER_MM = 96.0;
        public static final double GEAR_RATIO = 1.0;
        public static final double TICKS_PER_REV = 537.7;
    }

    public static final class Arm {
        public static final int HOME = 0;
        public static final int LOW = 300;
        public static final int HIGH = 850;
    }
}
```

This is what lets CurioControl stay reusable across robots and seasons.

## Next

- [Hardware abstraction](../guides/hardware-abstraction.md) — motors, servos, encoders, IMU
- [Drivetrains](../guides/drivetrains.md) — mecanum, tank, field-centric
- [Writing your own subsystem](../guides/robot-config.md)
