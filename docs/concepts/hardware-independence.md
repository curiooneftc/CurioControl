# Hardware Independence

The most important structural decision in CurioControl: **the packages that hold the robot's
reasoning contain no robot hardware at all.**

## What that means concretely

`math`, `control`, and `util` import **zero** FTC or Android types. Not "few" — zero. That means:

```java
// This compiles without the FTC SDK anywhere in sight.
public final class PIDController {
    public double calculate(double target, double current) { ... }
}
```

A PID controller takes two doubles. A motion profile takes a time. A `Pose2d` takes three numbers.
None of them knows what a motor is, and none of them needs to.

## Why it matters

**Testing becomes possible.** A PID controller with a closed loop, anti-windup under saturation,
and a `dt` that varies loop to loop is genuinely hard to reason about. It is trivial to unit-test,
and the tests run in milliseconds with no hardware, no Robot Controller, and no flakiness. That
density of testing is only reachable if the logic does not need a robot to run.

**The design improves.** Because a controller cannot reach for a motor, it has to be told what it
is controlling and what the current value is. That constraint pushes toward APIs that are easier
to use correctly.

**Consumers stay unencumbered.** A desktop tool that wants to simulate your autonomous routine can
depend on `math` and `control` without dragging in the Android world.

## It is enforced, not promised

The rule is an ArchUnit test:

```java
@Test
void controlPackageIsPure() {
    noClasses()
            .that().resideInAPackage("org.curioone.control.control..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.qualcomm..", "android..", "androidx..")
            .check(classes);
}
```

Add one `import com.qualcomm.robotcore.hardware.DcMotor;` to a control class and the build fails.
This is deliberate: an unenforced purity rule erodes one convenient import at a time.

## Where the hardware boundary is

The boundary is the `hardware` package — thin wrappers that hold an SDK object and delegate to
it. Those wrappers are the only place SDK types are allowed to appear, and each one exposes the
underlying object so direct SDK access stays available when you need it.

That is also exactly where the test suite substitutes mocks, so the wrapper's own logic
(power clamping, mode changes, unit conversion) is testable too.

## Working with IMU-backed heading

A subtler case: a mecanum drive in field-centric mode needs a heading, but `drive` must not import
the SDK `IMU`. The solution is a plain heading source interface that the hardware layer
implements. `drive` asks for a number; the `hardware` layer knows where it came from.

## The result

You can develop and test the entire decision-making half of a robot — the geometry, the control
loops, the motion profiles, the state machines — on a laptop, in milliseconds, with full coverage.
Only the thin layer that actually touches a wire needs a robot.
