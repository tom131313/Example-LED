// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

// import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Color;

public class IntakeSubsystem extends SubsystemBase {
  private final RobotSignals robotSignals;
  private final CommandXboxController operatorController;
  public final Trigger gamePieceAcquired = new Trigger(this::hasGamePieceAcquired);

  public IntakeSubsystem(RobotSignals robotSignals, CommandXboxController operatorController) {
    this.robotSignals = robotSignals;
    this.operatorController = operatorController;
    gamePieceAcquired.whileTrue(gamePieceAcquired());
  }

  public Command gamePieceAcquired() {
    Color gamePieceAcquiredSignal = new Color(1., 1., 1.);
    return
      robotSignals.Main.setSignal(gamePieceAcquiredSignal) // this command locks the robotSignals.Main subsystem only
        .ignoringDisable(true)
        .withName("MainGamePieceAcquiredSignal");
  }

  private boolean hasGamePieceAcquired() {
    return operatorController.getHID().getBButton(); // fake event source for game piece acquired
  }
}
