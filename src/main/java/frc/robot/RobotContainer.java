// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.units.Units.Seconds;
import static edu.wpi.first.wpilibj2.command.Commands.deadline;
import static edu.wpi.first.wpilibj2.command.Commands.parallel;
import static edu.wpi.first.wpilibj2.command.Commands.print;
import static edu.wpi.first.wpilibj2.command.Commands.race;
import static edu.wpi.first.wpilibj2.command.Commands.sequence;
import static edu.wpi.first.wpilibj2.command.Commands.waitSeconds;

import java.util.function.DoubleSupplier;

import edu.wpi.first.math.filter.Debouncer.DebounceType;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.ParallelDeadlineGroup;
import edu.wpi.first.wpilibj2.command.ParallelRaceGroup;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.subsystems.AchieveHueGoal;
import frc.robot.subsystems.GroupDisjointTest;
import frc.robot.subsystems.HistoryFSM;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.RobotSignals;
import frc.robot.subsystems.RobotSignals.LEDPatternSupplier;
import frc.robot.subsystems.TargetVisionSubsystem;

public class RobotContainer {

  private boolean logCommands = false; // switch command logging on/off; a lot of output for the command execute methods
  private int operatorControllerPort = 0; 
  private final CommandXboxController operatorController = new CommandXboxController(operatorControllerPort);
  private DoubleSupplier hueGoal = ()->operatorController.getRightTriggerAxis() * 180.; // scale joystick 0 to 1 to computer color wheel hue 0 to 180
  
  // define all the subsystems
  private final IntakeSubsystem intake;
  private final TargetVisionSubsystem vision;
  private final HistoryFSM historyFSM;
  private final AchieveHueGoal achieveHueGoal;
  private final GroupDisjointTest[] groupDisjointTest = {new GroupDisjointTest("A"), new GroupDisjointTest("B"), new GroupDisjointTest("C")};
  private final RobotSignals robotSignals; // container for all the LEDView subsystems

  public RobotContainer() {

    robotSignals = new RobotSignals(1);
    intake = new IntakeSubsystem(robotSignals.Main, operatorController);
    vision = new TargetVisionSubsystem(robotSignals.Top, operatorController);
    historyFSM = new HistoryFSM(robotSignals.HistoryDemo, operatorController);
    achieveHueGoal = new AchieveHueGoal(robotSignals.AchieveHueGoal/*, hueGoal*/);
 
    configureBindings();

    configureDefaultCommands();

    if(logCommands) configureLogging();

    var cmd1 = waitSeconds(1.);
    var req1 = cmd1.getRequirements();
    if (!req1.isEmpty()) cmd1 = cmd1.asProxy();

    var cmd2 = groupDisjointTest[0].setTest(1);
    var req2 = cmd2.getRequirements();
    if (!req2.isEmpty()) cmd2 = cmd2.asProxy();

    System.out.println(req1 + " " + req2);
    System.out.println(cmd1.getRequirements() + " " + cmd2.getRequirements());


  }
   
  /**
   * configure driver and operator controllers' buttons
   * (if they haven't been defined)
   */
    private void configureBindings() {

    operatorController.x().debounce(0.03, DebounceType.kBoth)
      .onTrue(robotSignals.Top.setSignal(colorWheel()));

    final Trigger startAcceptingSetpoints = new Trigger(operatorController.rightTrigger(0.05))
      .onTrue(achieveHueGoal.hueGoal.setHueGoal(hueGoal));
  }

  /**
   * "color wheel" supplier runs when commanded
   * @return
   */
  private RobotSignals.LEDPatternSupplier colorWheel() {

    // produce a color based on the timer current seconds of the minute
    return ()->LEDPattern.solid(
      Color.fromHSV((int)(Timer.getFPGATimestamp()%60./*seconds of the minute*/)*3/*scale seconds to 180 hues per color wheel*/,
       200, 200));
  }

  /**
   * Configure some of the Default Commands
   * 
   * DANGER DANGER DANGER
   * 
   * Default commands are not run in composed commands.
   * 
   * Suggest not using default commands to prevent assuming they run.
   * (Example included on how to disable the setDefaultCommand)
   * 
   * Alternatively use the "disjointSequence()" method that allows commands to
   * release their resources upon completion instead of waiting for completion
   * of the entire command composition then the default command runs upon
   * completion of each individual command. (Example included)
   * 
   * If using the default command, suggest not setting it more than once
   * to prevent confusion on which one is set.
   * (Example included on how to prevent more than one setting of the default command)
   */
  private void configureDefaultCommands() {

    final LEDPattern TopDefaultSignal = LEDPattern.solid(new Color(0., 0., 1.));
    final LEDPattern MainDefaultSignal = LEDPattern.solid(new Color(0., 1., 1.));
    final LEDPattern disabled = LEDPattern.solid(Color.kRed).breathe(Seconds.of(2));
    final LEDPattern enabled = LEDPattern.solid(Color.kGreen).breathe(Seconds.of(2));
    final LEDPatternSupplier EnableDisableDefaultSignal = () -> (DriverStation.isDisabled() ? disabled : enabled);

    final Command TopDefault = robotSignals.Top.setSignal(TopDefaultSignal)
                                .ignoringDisable(true).withName("TopDefault");
    final Command MainDefault = robotSignals.Main.setSignal(MainDefaultSignal)
                                .ignoringDisable(true).withName("MainDefault");
    final Command EnableDisableDefault = robotSignals.EnableDisable.setSignal(EnableDisableDefaultSignal)
                                .ignoringDisable(true).withName("EnableDisableDefault");

    robotSignals.Top.setDefaultCommand(TopDefault);
    robotSignals.Main.setDefaultCommand(MainDefault);
    robotSignals.EnableDisable.setDefaultCommand(EnableDisableDefault);
  }

  /**
   * Create a command to run in Autonomous
   * @return
   */
  public Command getAutonomousCommand() {

    LEDPattern autoTopSignal = LEDPattern.solid(new Color(0.2, 0.6, 0.2));
    LEDPattern autoMainSignal = LEDPattern.solid(new Color(0.4, 0.9, 0.4));
    // statements before the return are run early at intialization time
    return
    // statements returned are run later when the command is scheduled
      parallel // interrupting either of the two parallel commands with an external command interupts the group
      (
      /*parallel cmd*/
        robotSignals.Top.setSignal(autoTopSignal)
          .withTimeout(6.) // example this ends but the group continues and the default command is not activated here with or without the andThen command
          .andThen(robotSignals.Top.setSignal(autoTopSignal)),
      /*parallel cmd*/
        robotSignals.Main.setSignal(autoMainSignal)
      )
      /*composite*/
        .withName("AutoSignal");
      // command ends here so default command runs if no subsequant command runs for the subsystem
    }

  /**
   * Configure Command logging
   */
  private void configureLogging() {
    
    //_________________________________________________________________________________
    CommandScheduler.getInstance()
        .onCommandInitialize(
            command ->
            {
              System.out.println(/*command.getClass() + " " +*/ command.getName() + " initialized " + command.getRequirements());
            }
        );
    //_________________________________________________________________________________
    CommandScheduler.getInstance()
        .onCommandInterrupt(
            command ->
            {
              System.out.println(/*command.getClass() + " " +*/ command.getName() + " interrupted " + command.getRequirements());
            }
        );
    //_________________________________________________________________________________
    CommandScheduler.getInstance()
        .onCommandFinish(
            command ->
            {
              System.out.println(/*command.getClass() + " " +*/ command.getName() + " finished " + command.getRequirements());
            }
        );
    //_________________________________________________________________________________
    CommandScheduler.getInstance()
        .onCommandExecute( // this can generate a lot of events
            command ->
            {
              System.out.println(/*command.getClass() + " " +*/ command.getName() + " executed " + command.getRequirements());
            }
        );
    //_________________________________________________________________________________
  }
  
  // Demonstration of loosely connected commands in a sequential group such that the subsystem
  // default commands run if the subsystem is not active.
  // Standard behavior is all subsystems are locked for the duration of the group execution and
  // no default commands even if the subsystem isn't continuous active.

  private static final int A = 0; // subsystem id is subscript on the array of subsystems
  private static final int B = 1;
  private static final int C = 2;

  public final Command testSequence =
    sequence(
      groupDisjointTest[A].setTest(1), waitSeconds(0.1), groupDisjointTest[A].setTest(2), print("\nEND testSequence"));

  public final Command testDisjointSequence =
    disjointSequence(
      groupDisjointTest[A].setTest(1), waitSeconds(0.1), groupDisjointTest[A].setTest(2), print("\nEND testDisjointSequence test"));

  // Demonstration of loosely connected commands in a parallel group such that the subsystem
  // default commands run if the subsystem is not active.
  // Standard behavior is all subsystems are locked for the duration of the group execution and
  // no default commands even if the subsystem isn't continuous active.

  // public final Command testParallel =
  //   parallel(
  //     sequence(parallel(groupDisjointTest[A].setTest(1).asProxy(), groupDisjointTest[A].setTest(2).asProxy())),// no error message but erroneious results
  //     sequence(groupDisjointTest[B].setTest(1).asProxy(), waitSeconds(0.1), print("\nEND testParallel-B")),
  //     sequence(groupDisjointTest[C].setTest(1).asProxy().andThen(waitSeconds(0.1).asProxy()).asProxy().andThen(print("\nEND testParallel-C").asProxy()).asProxy()).asProxy() // no default
  //     // sequence(groupDisjointTest[C].setTest(1).asProxy().andThen(waitSeconds(0.1)).asProxy().andThen(print("\nEND testParallel-C")).asProxy()) // no default
  //   );


    public final Command testParallel =
      parallel(
        groupDisjointTest[A].testDuration(2, 2.).asProxy(),
        sequence(
          groupDisjointTest[B].testDuration(2, .74).asProxy(),
          parallel(
            groupDisjointTest[A].testDuration(3, .84).asProxy(),
            groupDisjointTest[B].testDuration(3, 1.).asProxy())),
      groupDisjointTest[C].testDuration(1, .6).asProxy()
    );

// can't use decorators on commands of subsystems needing default within the group
// reform as parallel() or sequence()
// add .asProxy() to commands of subsystems needing the default to run in the group
// can proxyAll() but really only need on subsystems needing default to run in group
// test with sequences in parallel and other nested compliactions

  public final Command testDisjointParallel =
      disjointParallel(
      disjointSequence(groupDisjointTest[A].setTest(1), waitSeconds(0.5), print("\nEND testParallel-A")),
      disjointSequence(groupDisjointTest[B].setTest(1), waitSeconds(0.1), print("\nEND testParallel-B"))
    );
/*
AdBdCd
START testParallel
A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1
A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1A2B2C1
A2B2CdA2B2CdA2B2CdA2B2CdA2B2Cd
A2BdCd
A3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3Cd
A3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3CdA3B3Cd
AdB3CdAdB3CdAdB3CdAdB3CdAdB3CdAdB3CdAdB3CdAdB3Cd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd

*/

  public final Command testDeadline =
    deadline(
      disjointSequence(groupDisjointTest[A].setTest(1), waitSeconds(0.2), print("\nEND testDeadline")),
      disjointSequence(groupDisjointTest[B].setTest(1), waitSeconds(10.)),
      disjointSequence(groupDisjointTest[C].setTest(1), waitSeconds(10.))
    );

  public final Command testDisjointDeadline =
    disjointDeadline(
      disjointSequence(groupDisjointTest[A].setTest(1), waitSeconds(0.2), print("\nEND testDisjointDeadline")),
      disjointSequence(groupDisjointTest[B].setTest(1), waitSeconds(10.)),
      disjointSequence(groupDisjointTest[C].setTest(1), waitSeconds(10.))
    );

  public final Command testRace =
    race(
      disjointSequence(groupDisjointTest[A].setTest(1), waitSeconds(0.2), print("\nEND testRace")),
      disjointSequence(groupDisjointTest[B].setTest(1), waitSeconds(10.)),
      disjointSequence(groupDisjointTest[C].setTest(1), waitSeconds(10.))
    );

  public final Command testDisjointRace =
    disjointRace(
      disjointSequence(groupDisjointTest[A].setTest(1), waitSeconds(0.2), print("\nEND testDisjointRace")),
      disjointSequence(groupDisjointTest[B].setTest(1), waitSeconds(10.)),
      disjointSequence(groupDisjointTest[C].setTest(1), waitSeconds(10.))
    );

  // illegal - Multiple commands in a parallel composition cannot require the same subsystems
  // public final Command testParallel =
  //   parallel(
  //     groupedDisjointTest.setTest(1).repeatedly(),
  //     waitSeconds(0.1).andThen(groupedDisjointTest.setTest(2)));

  public void testDisjoint(int count) {

    if (count == 20) {
      // stop default commands if they had been started
      groupDisjointTest[A].removeDefaultCommand();
      groupDisjointTest[B].removeDefaultCommand();
      groupDisjointTest[C].removeDefaultCommand();
    }

    if (count == 90) {
      groupDisjointTest[A].setDefaultCommand(); // used for Sequence and more testing
    }

    if (count == 100) {
      System.out.println("\nSTART testSequence");
      testSequence.schedule();
    }

    if (count == 200) {
      System.out.println("\nSTART testDisjointSequence");
      testDisjointSequence.schedule();
    }

    if (count == 290) {
      groupDisjointTest[B].setDefaultCommand(); // used for Parallel and more testing
      groupDisjointTest[C].setDefaultCommand(); // used for Parallel and more testing
    }

    if (count == 300) {
      System.out.println("\nSTART testParallel");
      testParallel.schedule();
    }

    if (count == 400) {
      System.out.println("\nSTART testDisjointParallel");
      testDisjointParallel.schedule();
    }

    if (count == 500) {
      System.out.println("\nSTART testDeadline");
      testDeadline.schedule();
    }

    if (count == 600) {
      System.out.println("\nSTART testDisjointDeadline");
      testDisjointDeadline.schedule();
    }

    if (count == 700) {
      System.out.println("\nSTART testRace");
      testRace.schedule();
    }

    if (count == 800) {
      System.out.println("\nSTART testDisjointRace");
      testDisjointRace.schedule();
    }

    if (count == 900) {
      // stop default commands to stop the output
      groupDisjointTest[A].removeDefaultCommand();
      groupDisjointTest[B].removeDefaultCommand();
      groupDisjointTest[C].removeDefaultCommand();
      CommandScheduler.getInstance().cancel(groupDisjointTest[A].getDefaultCommand());
      CommandScheduler.getInstance().cancel(groupDisjointTest[B].getDefaultCommand());
      CommandScheduler.getInstance().cancel(groupDisjointTest[C].getDefaultCommand());
    }
  }

  /*********************************************************************************************************/
  /*********************************************************************************************************/
  /*********************************************************************************************************/
  /*********************************************************************************************************/
  /*********************************************************************************************************/
  /*********************************************************************************************************/
  // to be included in an upcoming WPILib release
  /**
   * Runs individual commands in a series without grouped behavior.
   *
   * <p>Each command is run independently by proxy. The requirements of
   * each command are reserved only for the duration of that command and
   * are not reserved for an entire group process as they are in a
   * grouped sequence.
   * 
   * <p>disjoint...() does not propagate to interior groups. Use additional disjoint...() as needed.
   *
   * @param commands the commands to include in the series
   * @return the command to run the series of commands
   * @see #sequence(Command...) use sequence() to invoke group sequence behavior
   */
  public static Command disjointSequence(Command... commands) {
    return sequence(proxyAll(commands));
  }

  /**
   * Runs individual commands in a series without grouped behavior; once the last command ends, the series is restarted.
   *
   * <p>Each command is run independently by proxy. The requirements of
   * each command are reserved only for the duration of that command and
   * are not reserved for an entire group process as they are in a
   * grouped sequence.
   * 
   * <p>disjoint...() does not propagate to interior groups. Use additional disjoint...() as needed.
   *
   * @param commands the commands to include in the series
   * @return the command to run the series of commands repeatedly
   * @see #sequence(Command...) use sequenceRepeatedly() to invoke repeated group sequence behavior
   * @see #disjointSequence(Command...)
   * @see Command#repeatedly() 
   */
  public static Command repeatingDisjointSequence(Command... commands) {
    return disjointSequence(commands).repeatedly();
  }

  /**
   * Runs individual commands at the same time without grouped behavior; when the deadline command ends the otherCommands are cancelled.
   *
   * <p>Each otherCommand is run independently by proxy. The requirements of
   * each command are reserved only for the duration of that command and are
   * not reserved for an entire group process as they are in a grouped deadline.
   *
   * <p>disjoint...() does not propagate to interior groups. Use additional disjoint...() as needed.
   *
   * @param deadline the deadline command
   * @param otherCommands the other commands to include and will be cancelled when the deadline ends
   * @return the command to run the deadline command and otherCommands
   * @see #deadline(Command, Command...) use deadline() to invoke group parallel deadline behavior
   * @throws IllegalArgumentException if the deadline command is also in the otherCommands argument
   */
  public static Command disjointDeadline(Command deadline, Command... otherCommands) {
    new ParallelDeadlineGroup(deadline, otherCommands); // check parallel deadline constraints
    CommandScheduler.getInstance().removeComposedCommand(deadline);
    for (Command cmd : otherCommands) CommandScheduler.getInstance().removeComposedCommand(cmd);
    return deadline(deadline.asProxy(), proxyAll(otherCommands));
  }

  /**
   * Runs a group of commands at the same time. Ends once any command in the group finishes, and
   * cancels the others.
   *
   * @param commands the commands to include
   * @return the command group
   * @see ParallelRaceGroup
   */
  public static Command disjointRace(Command... commands) {
    return race(proxyAll(commands));
  }

  /**
   * Runs individual commands at the same time without grouped behavior and ends once all commands finish.
   *
   * <p>Each command is run independently by proxy. The requirements of
   * each command are reserved only for the duration of that command and
   * are not reserved for an entire group process as they are in a
   * grouped parallel.
   * 
   * <p>disjoint...() does not propagate to interior groups. Use additional disjoint...() as needed.
   *
   * @param commands the commands to run in parallel
   * @return the command to run the commands in parallel
   * @see #parallel(Command...) use parallel() to invoke group parallel behavior
   */
  public static Command disjointParallel(Command... commands) {
     new ParallelCommandGroup(commands); // check parallel constraints
     for (Command cmd : commands) CommandScheduler.getInstance().removeComposedCommand(cmd);
    return parallel(proxyAll(commands));
  }

  // to be included in an upcoming WPILib release
  /**
   * Maps an array of commands by proxying every element using {@link Command#asProxy()}.
   *
   * <p>This is useful to ensure that default commands of subsystems withing a command group are
   * still triggered despite command groups requiring the union of their members' requirements
   *
   * <p>Example usage for creating an auto for a robot that has a drivetrain and arm:
   *
   * <pre>
   * {@code var auto = sequence(proxyAll(drive.move(), arm.score()));}
   * </pre>
   *
   * @param commands an array of commands
   * @return an array of proxied commands
   */
  public static Command[] proxyAll(Command... commands) {
    Command[] out = new Command[commands.length];
    for (int i = 0; i < commands.length; i++) {
      out[i] = commands[i].asProxy();
    }
    return out;
  }
/*********************************************************************************************************/
/*********************************************************************************************************/
/*********************************************************************************************************/
/*********************************************************************************************************/
/*********************************************************************************************************/
/*********************************************************************************************************/

  /**
   * Run periodically before commands are run - read sensors, etc.
   * Include all classes that have periodic inputs or other need to run periodically.
   *
   * There are clever ways to register classes so they are automatically
   * included in a list but this example is simplistic - remember to type them in.
   */
  public void beforeCommands() {

    intake.beforeCommands();
    vision.beforeCommands();
    robotSignals.beforeCommands();
    historyFSM.beforeCommands();
    achieveHueGoal.beforeCommands();
    groupDisjointTest[0].afterCommands();
    groupDisjointTest[1].afterCommands();
    groupDisjointTest[2].afterCommands();
  }

  /**
   * Run periodically after commands are run - write logs, dashboards, indicators
   * Include all classes that have periodic outputs
   * 
   * There are clever ways to register classes so they are automatically
   * included in a list but this example is simplistic - remember to type them in.
   */
  public void afterCommands() {

    intake.afterCommands();
    vision.afterCommands();
    robotSignals.afterCommands();
    historyFSM.afterCommands();
    achieveHueGoal.afterCommands();
    groupDisjointTest[0].afterCommands();
    groupDisjointTest[1].afterCommands();
    groupDisjointTest[2].afterCommands();
  }
}

/*
AdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBd
START testSequence
A1
BdBdBdBdBdBd
A2
Bd
END testSequence
BdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBd
START testDisjointSequence
A1
BdAdBdAdBdAdBdAdBdAdBdAdBd
A2
BdBd
END testDisjointSequence test
AdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBd
AdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBd
AdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBd
AdBdAdBdAdBdAdBdAdBdAdBdAdBd
START testParallel
A1B1
AdBdAdBdAdBdAdBdAdBdAdBd
END testParallel-B
AdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBd
END testParallel-A
AdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBd
AdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBd
AdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdWarning at edu.wpi.first.wpilibj.IterativeRobotBase.printLoopOverrunMessage(IterativeRobotBase.java:412): Loop time of 0.02s overrun
AdBdAdBdAdBdAdBdAdBd
START testDisjointParallel
A1B1
AdBdAdBdAdBdAdBdAdBdAdBdAdBd
END testParallel-B
AdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBd
END testParallel-A
AdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBd
AdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdBdAdB
dCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
START testDeadline
A1B1C1AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
END testDeadline
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
START testDisjointDeadline
A1B1C1
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
END testDisjointDeadline
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
START testRace
A1B1C1
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
END testRace
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
START testDisjointRace
A1B1C1
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
END testDisjointRace
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
AdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCdAdBdCd
*/