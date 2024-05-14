// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;

public class TargetVisionSubsystem extends SubsystemBase {
  private final RobotSignals robotSignals;
  private final Joystick joystick;
  public final Trigger targetAcquired = new Trigger(this::canSeeTarget);

  public TargetVisionSubsystem(RobotSignals robotSignals, Joystick joystick) {
    this.robotSignals = robotSignals;
    this.joystick = joystick;
    targetAcquired.whileTrue(targetAcquired());
  }
  
  public Command targetAcquired() {
    return
      robotSignals.Top.setSignal(new Color(0., 1., 0.)) // this command locks the Top subsystem only
        .ignoringDisable(true)
        .withName("TopTargetAcquiredSignal")
      .andThen(Commands.idle(this).withTimeout(0.)); // add if a lock on this subsystem is also required
  }

  private boolean canSeeTarget() {
    return joystick.getRawButton(1); // fake source for can see target
  }
}
