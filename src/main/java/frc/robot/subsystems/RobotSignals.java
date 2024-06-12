// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

/**
 * Manage the addressable LEDs as signaling subsystems.
 *
 * <p>This is the creator and container of the LEDView subsystems.
 *
 * <p>Buffer is not cleared.
 *
 * <p>It's just a simple example.
 *
 * <p>Needs a better way of registering LED usage; this is really (too) simple and relies on the
 * user finding it here and remembering to do it.
 */

import frc.robot.AddressableLED;
import frc.robot.AddressableLEDBuffer;
import frc.robot.AddressableLEDBufferView;
import frc.robot.LEDPattern;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/*
 * All Commands factories are "public."
 *
 * All other methods are "private" to prevent other classes from forgetting to add requirements of
 * these resources if creating commands from these methods.
 */

public class RobotSignals {
  /**
   * Represents a supplier of LEDPattern for creating dynamic LEDPatterns.
   *
   * <p>Can't overload methods using generic interfaces parameters like Supplier so make our own
   * interface to use in overloads
   */
  @FunctionalInterface
  public interface LEDPatternSupplier {
    /**
     * Gets a result.
     *
     * @return a result
     */
    LEDPattern get();
  }

  private final AddressableLED m_strip;

  // Layout by LED number of the single physical buffer into multiple logical
  // views or resources/subsystems.

  // simplistic view of segments - assume one starts at 0.
  // first and last LED ids are inclusive of that LED number

  private static final int m_firstTopLED = 0;
  private static final int m_lastTopLED = 7;
  private static final int m_firstMainLED = 8;
  private static final int m_lastMainLED = 15;
  private static final int m_firstEnableDisableLED = 16;
  private static final int m_lastEnableDisableLED = 23;
  private static final int m_firstHistoryDemoLED = 24;
  private static final int m_lastHistoryDemoLED = 31;
  private static final int m_firstAchieveHueGoal = 32;
  private static final int m_lastAchieveHueGoal = 39;
  // CAUTION CAUTION CAUTION -- Update this length for each view defined.
  private static final int length =
      Math.max(
        Math.max(
          Math.max(
            Math.max(
              m_lastTopLED,
            m_lastMainLED),
          m_lastEnableDisableLED),
        m_lastHistoryDemoLED),
      m_lastAchieveHueGoal)
    + 1;

  public final LEDView m_top;
  public final LEDView m_main;
  public final LEDView m_enableDisable;
  public final LEDView m_historyDemo;
  public final LEDView m_achieveHueGoal;

  private final AddressableLEDBuffer bufferLED;

  public RobotSignals() {
    // start updating the physical LED strip
    final int addressableLedPwmPort = 1;
    m_strip = new AddressableLED(addressableLedPwmPort);
    m_strip.setLength(length);
    m_strip.start();

    bufferLED = new AddressableLEDBuffer(length); // buffer for all of the LEDs

    // create the resources (subsystems) as views of the LED buffer
    m_top = new LEDView(bufferLED.createView(m_firstTopLED, m_lastTopLED));
    m_main = new LEDView(bufferLED.createView(m_firstMainLED, m_lastMainLED));
    m_enableDisable =
        new LEDView(bufferLED.createView(m_firstEnableDisableLED, m_lastEnableDisableLED));
    m_historyDemo = new LEDView(bufferLED.createView(m_firstHistoryDemoLED, m_lastHistoryDemoLED));
    m_achieveHueGoal =
        new LEDView(bufferLED.createView(m_firstAchieveHueGoal, m_lastAchieveHueGoal));
  }

  /**
   * Run before commands and triggers
   */
  public void runBeforeCommands() {}

  /**
   * Run after commands and triggers
   */
  public void runAfterCommands() {
    m_strip.setData(bufferLED); // run periodically to send the buffer to the LEDs
  }

  /** LED view resource (subsystem) */
  public class LEDView extends SubsystemBase {
    private final AddressableLEDBufferView m_view;

    private LEDView(AddressableLEDBufferView view) {
      this.m_view = view;
    }

    /*
     * Public Commands
     */

    /**
     * Example of how to allow one (or none) default command to be set.
     *
     * @param def default command
     */
    @Override
    public void setDefaultCommand(Command def) {
      if (getDefaultCommand() != null) {
        throw new IllegalArgumentException("Default Command already set");
      }

      if (def != null) {
        super.setDefaultCommand(def);
      }
    }

    // Some uses of setSignal assume the command keeps running so provide that function.
    // Some uses of setSignal refresh the signal in their own looping so provide a single use, too.

    /**
     * Put an LED Pattern into the view - keep running
     *
     * @param pattern
     * @return Command to apply pattern to LEDs
     */
    public Command setSignal(LEDPattern pattern) {
      return run(() -> pattern.applyTo(m_view)).ignoringDisable(true).withName("LedSet");
    }

    /**
     * Put a dynamic LED Pattern into the view - keep running
     *
     * @param pattern
     * @return Command to apply pattern to LEDs
     */
    public Command setSignal(LEDPatternSupplier pattern) {
      return run(() -> pattern.get().applyTo(m_view)).ignoringDisable(true).withName("LedSetS");
    }

    /**
     * Put an LED Pattern into the view - once
     *
     * @param pattern
     * @return Command to apply pattern to LEDs
     */
    public Command setSignalOnce(LEDPattern pattern) {
      return runOnce(() -> pattern.applyTo(m_view)).ignoringDisable(true).withName("LedSet");
    }

    /**
     * Put a dynamic LED Pattern into the view - once
     *
     * @param pattern
     * @return Command to apply pattern to LEDs
     */
    public Command setSignalOnce(LEDPatternSupplier pattern) {
      return runOnce(() -> pattern.get().applyTo(m_view)).ignoringDisable(true).withName("LedSetS");
    }
  } // End LEDView
}
