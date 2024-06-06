// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

/**
 * Display a random color signal periodically and don't reuse the same color too soon (if reasonably
 * possible).
 *
 * This is a FSM that depends on the current state, the transition event trigger, and the historical
 * previous states.
 * 
 * This demonstrates a command that calls another command upon completion by setting up a trigger.
 * This demonstrates accessing historical data to determine the next state. This demonstrates using
 * persistent data to periodically refresh outputs past the completion of this command. This
 * demonstrates running a command periodically based on the past time history.
 * 
 * Caution - this is a simple, contrived example. This command essentially runs periodically the
 * hard way using memory of a time and a trigger on the time. There may be better ways to do that
 * simple scheduling structure, for example, the use of the "beforeCommands()" or "afterCommands()"
 * periodic methods as used for a sub-purpose here is a good way.
 */

import static edu.wpi.first.units.Units.Milliseconds;
import static edu.wpi.first.units.Units.Seconds;

import frc.robot.Color;
import frc.robot.LEDPattern;
import frc.robot.subsystems.RobotSignals.LEDView;

import edu.wpi.first.units.Measure;
import edu.wpi.first.units.Time;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class HistoryFSM extends SubsystemBase {

    private final LEDView m_RobotSignals;
    private Random m_Rand = new Random();

    // Periodic output variable used for each run of "afterCommands()"
    // this is used before command to set it is run so start with LEDs off
    private LEDPattern m_PersistentPatternDemo = LEDPattern.solid(Color.kBlack);

    // Add a color [hue number as subscript] and last time used to the history
    // so that color isn't used again during a lockout period.

    // Make the history in as narrow of scope as possible. For this simple example the scope is
    // perfectly narrow (this instance scope) since the history doesn't depend on any values
    // from other subsystems.

    // Also saved from historical values are the "current" color so it persists through multiple
    // iterations.

    // Time data is saved for how long a color is to persist in the display.

    private static final int m_ComputerColorWheel = 180; // max count of hues numbered 0 to 179
    // list of the last times of all the colors so try not to repeat for a long time so repeats are
    // rare
    private List<Measure<Time>> lastTimeHistoryOfColors =
            new ArrayList<>(m_ComputerColorWheel);

    private static final double m_BeginningOfTime = 0.0;
    private static final double m_EndOfTime = Double.MAX_VALUE;

    // time the current color display should end and a new color selected
    // initialize so the time doesn't trigger anything until the "Y" button is pressed
    private Measure<Time> m_NextTime = Milliseconds.of(m_EndOfTime);
    Measure<Time> m_ChangeColorPeriod = Seconds.of(2.0); // display color for this long
    Measure<Time> m_ColorLockoutPeriod = Seconds.of(20.0); // try not to reuse a color for this long
    // elapsed timer
    private Trigger m_TimeOfNewColor = new Trigger(this::timesUp);
    private static final double m_DebounceTime = 0.04;

    public HistoryFSM(LEDView robotSignals,
            CommandXboxController operatorController) {

        this.m_RobotSignals = robotSignals;

        fillInitialTimes();// initialize last time used for all the hues of the color wheel

        // Trigger if it's time for a new color or the operator pressed their "Y" button
        m_TimeOfNewColor.or(operatorController.y().debounce(m_DebounceTime))
                .onTrue(runOnce(this::getHSV) // new color
                        .andThen(runOnce(this::setNextTime)) // next time for new color
                );
    }

    /**
     * Create an initialized list of hues
     */
    private void fillInitialTimes() {

        // initially indicate hue hasn't been used in a long time ago so available immediately
        for (int i = 0; i < m_ComputerColorWheel; i++) {
            lastTimeHistoryOfColors.add(Seconds.of(m_BeginningOfTime));
        }
    }

    /**
     * Sets the time for the trigger of its next periodic run
     */
    private void setNextTime() {

        Measure<Time> currentTime = Milliseconds.of(System.currentTimeMillis());
        m_NextTime = currentTime.plus(m_ChangeColorPeriod);
    }

    /**
     * Elapsed Timer determines if in the color change lockout period or not. Resets automatically.
     * 
     * @return has time elapsed
     */
    private boolean timesUp() {

        if (m_NextTime.lt(Milliseconds.of(System.currentTimeMillis()))) {
            // reset; a command may run that will set the correct periodic "nextTime".
            // Otherwise wait for other triggering to restart.
            // This locks-out automatic restarting on disable-to-enable change; other trigger
            // required to get it started again.
            m_NextTime = Milliseconds.of(m_EndOfTime);
            return true;
        }
        return false; // not time to trigger yet
    }

    /**
     * Sets a color and quits immediately assuming the color persists somehow (in
     * "persistentPatternDemo") until the next color is later requested.
     * 
     * <p>
     * Set a random color that hasn't been used in the last "colorLockoutPeriod"
     */
    public void getHSV() {

        Measure<Time> currentTime = Milliseconds.of(System.currentTimeMillis());
        int randomHue; // to be the next color
        int loopCounter = 0; // count attempts to find a different hue
        int loopCounterLimit = 20; // limit attempts to find a different hue
        // reasonable limit related to:
        // number of colors, how often colors change, how long to lockout a color.

        do {
            // Generate random numbers for hues in range of the computer color wheel
            randomHue = m_Rand.nextInt(m_ComputerColorWheel);
            // if hue hasn't been used recently, then use it now and update its history
            var colorTime = lastTimeHistoryOfColors.get(randomHue); // get the associated time
            if (colorTime.lt(currentTime.minus(m_ColorLockoutPeriod))) {
                lastTimeHistoryOfColors.set(randomHue, currentTime);
                break;
            }
            // hue used recently so loop to get another hue
            // limit attempts - no infinite loops allowed
        } while (++loopCounter < loopCounterLimit);

        m_PersistentPatternDemo =
                LEDPattern.solid(Color.fromHSV(randomHue, 200, 200));
    }

    /**
     * Example of how to disallow default command
     */
    @Override
    public void setDefaultCommand(Command def) {

        throw new IllegalArgumentException("Default Command not allowed");
    }

    /**
     * Run before commands and triggers
     * 
     * beforeCommands() and afterCommands() run from the Robot.periodic() via RobotContainer and are
     * run before and after Commands and Triggers are run.
     */
    public void beforeCommands() {}

    int m_DebugPrintCounter; // 0; initialize limit testing prints counter

    /**
     * Run after commands and triggers
     */
    public void afterCommands() {

        // testing prints
        boolean debugPrint = true;
        if (debugPrint) {
            m_DebugPrintCounter++;
            if (m_DebugPrintCounter % 600 == 0) {
                System.out
                        .println("current time " + System.currentTimeMillis());
                for (int i = 0; i < lastTimeHistoryOfColors.size(); i++) {
                    System.out.println(i + " "
                            + lastTimeHistoryOfColors.get(i).toLongString());
                }
            }
        }

        // Set and refresh the color could be done many ways:
        // here which is called periodically through Robot.periodic(),
        // where the data were created,
        // the default command,
        // or the device that receives the data may keep the last value alive.
        // Being done here for illustrative purposes.

        m_RobotSignals.setSignal(m_PersistentPatternDemo).schedule(); // access to the LEDS is only by
                                                                  // command so do it that way.
        // Note that because this method runs in disabled mode, the color persists in Disabled mode
        // even if the command was
        // not to run in disabled mode.
        // Could check here for that and black out if necessary. Or do something in disabledInit().

        // Thus, we end up demonstrating how to run a command periodically (as a minor part of the
        // bigger picture).
        // This usage within a subsystem is NOT (maybe) the same as a command directly scheduling
        // another command;
        // that technique should be avoided.
    }
}
