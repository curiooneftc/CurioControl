# Commands vs. State Machines

> Status: the command system and `StateMachine` landed in v0.3.0; see
> [the phases document](../../PHASES.md).

CurioControl offers two ways to compose robot behaviour, and neither is required. Commands
describe actions as a tree; state machines describe a routine as places with transitions.
Most autonomous routines want exactly one of the two, and the wrong choice announces itself
early: a command tree full of conditional branches wants to be states, and a state machine
full of pass-through states wants to be a sequence.

## The command system in one page

A `CommandScheduler` runs from the OpMode loop. Commands declare the subsystems they need;
the scheduler never runs two commands that need the same subsystem.

```java
public class MainTeleOp extends CurioOpMode {
    private final CommandScheduler scheduler = new CommandScheduler();

    @Override
    public void initRobot() {
        robot = new CurioRobot(hardwareMap);
        Arm arm = new Arm(hardwareMap);
        robot.registerSubsystem(arm);
        // The arm holds position whenever no command owns it.
        scheduler.setDefaultCommand(arm, Commands.instant("Hold", () -> {}, arm));
    }

    @Override
    public void runRobot() {
        scheduler.run();
        scheduler.publishTelemetry(robot.telemetry());
    }
}
```

Ownership rules, all enforced by the scheduler:

1. **Declare what you touch.** A command that drives a subsystem without requiring it is
   invisible to the scheduler, and invisibility is how two writers end up on one motor.
2. **Scheduling preempts.** A new command needing a busy subsystem ends the running one
   with `end(true)`. Preemption is silent but reported — `publishTelemetry` lists what is
   running under the `DEBUG` category, so turn on `CurioConfig.DEBUG` when a routine
   keeps losing its mechanism.
3. **Cancellation always ends cleanly.** `cancel`, preemption, and OpMode stop all funnel
   through `end(true)`. Stop the mechanism in `end`, not in `execute`, and a cancelled
   autonomous leaves the robot still.
4. **Schedule from hooks freely.** A command scheduled from inside another command's hook
   joins on the next scheduler pass, never mid-iteration. There is one ordering, and it
   does not depend on who called what from where.

## Subsystem factories

Mechanism-specific factories live on the subsystem, next to the mechanism logic — this is
team code, so the framework's package layering does not apply:

```java
public class Arm extends Subsystem {
    private final Motor motor;

    public Command moveTo(int ticks) {
        return Commands.sequence(
                Commands.instant("Arm to " + ticks, () -> motor.setTargetPosition(ticks), this),
                Commands.waitUntil("Arm arrived",
                        () -> Math.abs(motor.getPosition() - ticks) < TOLERANCE, this));
    }
}
```

Returning the command rather than running the motion keeps ownership visible: the caller
schedules it, the scheduler guards the subsystem, and the method itself never touches the
scheduler. Generic building blocks — `instant`, `waitSeconds`, `waitUntil`, `sequence`,
`parallel` — live in `Commands`.

## The state machine in one page

```java
enum Auto { DRIVE_TO_SCORE, SCORE, RETURN, PARK }

StateMachine<Auto> auto = new StateMachine<>(Auto.DRIVE_TO_SCORE);
auto.onEnter(Auto.SCORE, () -> arm.moveTo(HIGH));
auto.addTransition(Auto.SCORE, Auto.RETURN, () -> arm.atTarget());
auto.setTimeout(Auto.SCORE, 3.0, Auto.RETURN);   // watchdog, not the plan
```

`update()` runs once per loop: the update action, then at most one transition — an
explicit `requestTransition` first, then the timeout, then guards in registration order.
The two mix freely: a state's update action schedules commands, and a command's end
action requests a transition. Neither owns the other.

## The same autonomous, both ways

SPEC §24's routine — drive to scoring position, score, return, park:

```java
// As a command tree: the routine IS the composition.
Command blueAuto = Commands.sequence("BlueAuto",
        new DriveDistanceCommand(drive, 24.0),
        arm.moveTo(HIGH),
        Commands.sequence("Score",
                Commands.instant("Release", intake::release, intake),
                Commands.waitSeconds(0.5)),
        arm.moveTo(HOME),
        new DriveDistanceCommand(drive, -24.0));
```

```java
// As a state machine: the routine is places, the transitions carry the logic.
auto.onEnter(Auto.DRIVE_TO_SCORE, () -> scheduler.schedule(new DriveDistanceCommand(drive, 24.0)));
auto.addTransition(Auto.DRIVE_TO_SCORE, Auto.SCORE, () -> drive.atTarget());
auto.onEnter(Auto.SCORE, () -> scheduler.schedule(arm.moveTo(HIGH)));
auto.addTransition(Auto.SCORE, Auto.RETURN, () -> arm.atTarget());
auto.setTimeout(Auto.SCORE, 5.0, Auto.RETURN);
auto.onEnter(Auto.RETURN, () -> scheduler.schedule(new DriveDistanceCommand(drive, -24.0)));
auto.addTransition(Auto.RETURN, Auto.PARK, () -> drive.atTarget());
```

| | Command tree | State machine |
|---|---|---|
| Reads as | what happens, in order | where the robot is, and what moves it on |
| Branching | awkward (nested compositions) | natural (guards) |
| Timeouts/watchdogs | manual (`waitUntil` + timeouts) | built in (`setTimeout`) |
| Re-entry (retry a step) | rebuild the tree | `requestTransition` back |
| Debugging | scheduler dump shows the running stack | `getState()` shows the place |

Rule of thumb: a routine that always does the same steps in the same order is a tree. A
routine whose next step depends on what the robot sees — a missed sample, a blocked path,
a sensor that disagrees — is states. When a tree grows conditional branches, or a machine
grows pass-through states with no guards, switch.

## Next

- [Motion profiles](motion-profiles.md) — smooth references for the commands to track
- [Telemetry and logging](telemetry-and-logging.md) — scheduler introspection and CSV logs
