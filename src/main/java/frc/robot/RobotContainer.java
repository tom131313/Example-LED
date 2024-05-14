// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.wpilibj2.command.Commands.parallel;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.RobotSignals;
import frc.robot.subsystems.TargetVisionSubsystem;

public class RobotContainer {
  private final Joystick joystick = new Joystick(0);
  private final IntakeSubsystem intake;
  private final TargetVisionSubsystem vision;

  private final RobotSignals robotSignals;

  public RobotContainer(PeriodicTask periodicTask) {
    robotSignals = new RobotSignals(64, 1, periodicTask);
    intake = new IntakeSubsystem(robotSignals, joystick);
    vision = new TargetVisionSubsystem(robotSignals, joystick);

    configureBindings();
    configureLogging();
  }

  private void configureBindings() {}

  private void configureLogging() {
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
  }

  public Command getAutonomousCommand() {
    return
      parallel( // interrupting either command with an external command interupts the group
        robotSignals.Top.setAutoSignal()
          .withTimeout(6.) // this ends but the group continues and the default command is not activated here with or without the andThen command
          .andThen(robotSignals.Top.setSignal(new Color(100., 100., 100.))),
        robotSignals.Main.setAutoSignal());
    }
}



/* Parking Lot
  private final LEDPattern disabled = LEDPattern.solid(Color.kRed).breathe(Seconds.of(2));
  private final LEDPattern enabled = LEDPattern.solid(Color.kGreen).breathe(Seconds.of(2));
  private final LEDPattern defaultPattern = () -> (DriverStation.isDisabled() ? disabled : enabled).applyTo();
  private final LEDPattern blink = LEDPattern.solid(Color.kMagenta).blink(Seconds.of(0.2));
  private final LEDPattern orange = LEDPattern.solid(Color.kOrange);


  ledTop.setDefaultCommand(ledTop.runPattern(defaultPattern).ignoringDisable(true).withName("LedDefaultTop"));
  ledMain.setDefaultCommand(ledMain.runPattern(defaultPattern).ignoringDisable(true).withName("LedDefaultMain"));

  intake.gamePieceAcquired.onTrue(
    ledMain.runPattern(blink).withTimeout(1.0).withName("LedAcquiredGamePiece"));

  vision.targetAcquired.whileTrue(
    ledTop.runPattern(orange).withName("LedVisionTargetInSight"));
*/
