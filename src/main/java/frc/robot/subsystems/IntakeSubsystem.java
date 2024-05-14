// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;

public class IntakeSubsystem extends SubsystemBase {
  private final RobotSignals robotSignals;
  private final Joystick joystick;
  /**
   * Indicates that intake has successfully acquired a game piece.
   */
  public final Trigger gamePieceAcquired = new Trigger(this::hasGamePieceAcquired);

  public IntakeSubsystem(RobotSignals robotSignals, Joystick joystick) {
    this.robotSignals = robotSignals;
    this.joystick = joystick;
    gamePieceAcquired.whileTrue(gamePieceAcquired());
  }

  public Command gamePieceAcquired() {
    return
      robotSignals.Main.setSignal(new Color(1., 1., 1.)) // this command locks the Main subsystem only
        .ignoringDisable(true)
        .withName("MainGamePieceAcquiredSignal");
  }

  private boolean hasGamePieceAcquired() {
    return joystick.getRawButton(2); // fake source for game piece acquired
  }
}
