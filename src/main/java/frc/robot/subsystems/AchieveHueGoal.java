// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

/*
 * Example of a subsystem that is a (PID) controller to achieve a goal (setpoint).
 * 
 * WHAT THIS IS NOT:
 * Some teams use the structure of a (perpetually, possibly) running system (class) that is
 * self-contained and periodically updated within itself. No commands are used to calculate or
 * schedule calculations. It responds to commands only to accept the goal (setpoint).
 * 
 * That scheme might not be fully in the spirit of command-based so this example is not encouraging
 * that usage.
 * 
 * A suggestion by CD @Amicus1 for those against using commands except to set the goal:
 * If this is a verbosity of code issue, I suggest writing the logic as a private subsystem method
 * and exposing it as a command factory.
 */

import static edu.wpi.first.units.Units.Seconds;
import static edu.wpi.first.wpilibj2.command.Commands.sequence;

import frc.robot.Color;
import frc.robot.LEDPattern;
import frc.robot.subsystems.RobotSignals.LEDView;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj2.command.Command;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

import java.util.function.DoubleSupplier;

/**
 * PID controller to achieve (slowly by over-damped kP gain) a color hue goal set by joystick right
 * trigger axis and display progress toward goal on the LEDs.
 */
public class AchieveHueGoal extends SubsystemBase {

  private final PIDController m_hueController;
  double m_hueSetpoint;
  double m_currentStateHue;
  LEDPattern m_currentStateSignal; // what to display
  private final LEDView m_robotSignals; // where the output is displayed

  /**
   * Constructor
   *
   * @param robotSignals
   */
  public AchieveHueGoal(LEDView robotSignals) {
    this.m_robotSignals = robotSignals;
    /**
     *  PID initialization.
     *  The PID controller is ready but not running initially until a command is issued
     *  with a setpoint.
     */
    m_currentStateHue = 0.0; // also considered the initial and previous state
    final double kP = 0.025;
    final double kI = 0.0;
    final double kD = 0.0;
    m_hueController = new PIDController(kP, kI, kD);
    final double tolerance = 2.0; // hue range is 0 to 180
    m_hueController.setTolerance(tolerance);
  }

  // Methods that change the subsystem should be private.
  // Methods that inquire about the system must be public.
  // Triggers bound within should be private.

  /**
   * Set the Goal and Move Toward The Goal.
   *
   * <p>Runs until the goal has been achieved within the tolerance at which time the end is
   * indicated and the controller/command stops.
   * 
   * @param hueSetpoint the goal is dynamically supplied hue 0 to 180 (computer version of a color wheel)
   * @return command used to set and achieve the goal
   */
  public Command displayHue(DoubleSupplier hueSetpoint) {
    final double minimumHue = 0.0;
    final double maximumHue = 180.0;
    return
      sequence(

        runOnce(this::reset),

        run(() -> { // run to the setpoint displaying state progress as it runs
                m_currentStateHue = // compute the current state
                    MathUtil.clamp(
                        m_currentStateHue
                         + m_hueController.calculate(m_currentStateHue, hueSetpoint.getAsDouble()),
                        minimumHue,
                        maximumHue);
                m_currentStateSignal = // color for the current state
                    LEDPattern.solid(Color.fromHSV((int) m_currentStateHue, 200, 200));
              }
            )
          
          .until(m_hueController::atSetpoint) // controller stops; momentum stable or not

          // parallel display the color of the current state as it's continually recalculated above
          .deadlineWith(m_robotSignals.setSignal(()->m_currentStateSignal)),

        // blink for a while to show at setpoint
        m_robotSignals.setSignal(()->m_currentStateSignal.blink(Seconds.of(0.1))).withTimeout(2.0)
      )
      .finallyDo(this::reset); // cleanup the controller but it was stopped or interrupted above
  }

  /**
   * Reset or stop the controller
   * 
   * <p>Normally used in finallyDo() at the end of a controller command to assure stopping
   */
  public void reset() {
    // also stop other devices as needed but not needed in this example
    m_hueController.reset();
    m_currentStateHue = 0; // also considered the initial and previous state
    // signal pattern needed initially because of the race to show it first; then last for off
    m_currentStateSignal = LEDPattern.solid(Color.kGray);
    m_robotSignals.setSignalOnceDirect(m_currentStateSignal); // display off signal
  }

  /**
   * Immediately interrupt the controller command.
   * 
   * <p>Null command works merely by its existence; assuming the underlying controller command allows interrupts
   * and cleanly handles interrupts with "finallyDo()".
   * 
   * @return Command normally used to interrupt and stop the controller while it's running as a command
   */
  public Command interrupt() {
    return
      runOnce(() -> {});
  }

  /**
   * Run before commands and triggers
   */
  public void runBeforeCommands() {}

  /**
   * Run after commands and triggers
   */
  public void runAfterCommands() {}

  /**
   * Example of how to disallow default command
   *
   * @param def default command
   */
  @Override
  public void setDefaultCommand(Command def) {
    throw new IllegalArgumentException("Default Command not allowed");
  }
}
