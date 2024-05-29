// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

/**
 * Example program that shows a variety of command based and programming
 * "best practices."
 * 
 * Demonstration output is on five sets of ten LEDs to show the program is
 * operating; operator input is Xbox controller. The sixth demonstration
 * output is the console "prints."
 * 
 * 1. Target Vision Acquired subsystem output Top LEDView subsystem (runs disabled, too)
 *     default blue;
 *     autonomous mode command dark green (no requirement for Target Vision Acquired);
 *     target vision acquired orange (simulate target acquired by pressing "A" button);
 *     slowly around the color wheel (initiated by pressing "X" button) (no requirement
 *       for Target Vision Acquired)
 * 2. Game Piece Intake Acquired subsystem output Main LEDView subsystem
 *     default cyan;
 *     autonomous mode command light green (no requirement for Game Piece Intake Acquired);
 *     intake game piece acquired magenta fast blink (simulate game piece intake
 *       acquired by pressing "B" button)
 * 3. EnableDisable LEDView subsystem
 *     enabled mode green slow blink
 *     disabled mode red slow blink
 * 4. HistoryFSM subsystem HistoryDemo LEDView subsystem
 *     random colors that don't repeat for awhile (history) (initiated by pressing "Y"
 *       button then self perpetuating) (runs in enabled mode)
 * 5. AchieveHueGoal subsystem output AchieveHueGoal LEDView subsystem
 *     Subsystem based controller runs continuously and responds to a goal setting
 *       subsystem. Colors on color wheel position showing PID controller subsystem
 *       converging on a color selected by Xbox right trigger axis (press to start)
 * 6. Disjoint Sequential Group Demo console output initiated by teleop enable mode
 *     Show subsystem default command doesn't run within a group command unless the
 *     command with the subsystem requirement is disjointed from the group by using
 *     a Proxy structure. (runs in teleop mode)
 * 
 * All commands are interruptible.
 * Some button presses are debounced.
 */

/**
 * Example program demonstrating:
 * 
 * Splitting a common resource into two separately used resources (LEDs)
 * Configure button trigger
 * Triggers
 * Use of command parameters set at command creation time
 * Use of command parameters set at changable at runtime (Suppliers)
 * Use of method reference
 * Inject TimedRobot.addPeriodic() into other classes
 * Some commentary on composite commands and mode changes
 * Command logging
 * Configuring an autonomous commnad
 * Use of Xbox controller to produce fake events
 * Use of Xbox controller to trigger an event
 * Use of public command factories in subsystems
 * Use of private non-Command methods to prevent other classes from forgetting to lock a subsystem
 * Change TimeRobot loop speed
 * Change LED update rate different from the TimedRobot loop speed
 * Overloading method parameter types
 * No commands with the word Command in the name
 * No triggers with the word Trigger in the name
 * Supplier of dynamic LED pattern
 * Static LED pattern
 * Restrict Subsystem Default Command to none until set once at any time and then unchangeable
 * Goal setting subsystem for a resource
 * Triggers available for other systems to use
 * Default commands can either run or not run within a sequential group depending on how the group is defined using Proxy
 * 
 * This code anticipates extensions to the WPILib addressable LED class which are included here.
 * This example program runs in real or simulated mode of the 2024 WPILib.
 * 
 * This is a refactor and extension of code donated by ChiefDelphi @illinar. It is intended
 * to demonstrate good programming based on @Oblarg's rules.
 * 
 * The use of the InternalTrigger to sequence command runs was donated by ChiefDelphi @bovlb
 * 
 * Any errors or confusions are the fault and responsibility of ChiefDelphi @SLAB-Mr.Thomas; github tom131313.
 * 
 */

 /* Default Commands can be useful but they never run within grouped commands even if
 * their associated subsystem is not active at all times within the group.
 * 
 * There are several possibilites to accomodate that restriction:
 * 1. do without default commands at any time
 * 2. do without the default command only within the group
 * 3. manually code the function of the default command
 * 4. consider using a proxy branching out of the group restriction
 * 
 *    If using triggers to sequence successive commands helps better organize the
 *    command flow and isolate some subsytem requirements so the default command can
 *    run then that’s okay and is preferred to using proxied commands.
 *    Usage of proxies to hide the subsystem requirements from normal checks and
 *    thus allow the default command to activate could be useful but should be used
 *    extremely sparingly by an experienced user.
 * 
 *    The possibility of unintended consequences is very high if bypassing the
 *    normal. Usage should be limited to when you can easily understand what exactly
 *    you’re doing by invoking them.


extremely carefully add asProxy() to as few interior commands as possible to accomplish what you need.
repeatedly() doesn’t repeat correctly - the repeat is different than the original (it’s losing a race condition for coordinating between the repeat and the proxied command).

andThen() doesn’t work but that can be circumvented by using sequence().

Proxies break the sanity check that warn of a subsystem running in parallel commands. There is no warning - just wrong results (a warning might be added to the new combined Proxy library functions but that helps only if you use those functions correctly and nothing says you have to).

Slapping an asProxy() around a composed command isn’t sufficient. You have to proxy the inner commands, also or instead, and any new library commands to ease the use of Proxy aren’t recursive to inner layers.

I don’t have a catalog of what works and what doesn’t and how they fail.

Judicious use of asProxy() on a group’s interior commands can usually allow default commands to run correctly within groups. Slipup - and while coding it’s often not obvious you’ve made a slip - and you have an extremely difficult problem to debug.
*/

    /**
     * Suggestion is don't use the setDefaultCommand because default commands are not
     * run inside composed commands except if using "disjointSequence()".
     * 
     * If you insist, then recommendation is don't use more than one default command
     * because it may not be obvious which default command is active (last one set is
     * active).
     */

    // You're on your own to remember if there is a default command set or not.
  
package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

public class Robot extends TimedRobot {

  private RobotContainer m_robotContainer;
  private Command m_autonomousCommand;

  @Override
  public void robotInit() {

    m_robotContainer = new RobotContainer();
  }

  @Override
  public void robotPeriodic() {

    m_robotContainer.beforeCommands();    // get a consistent set of all inputs
    CommandScheduler.getInstance().run(); // check all the triggers and run all the scheduled commands
    m_robotContainer.afterCommands();     // write outputs such as logging, dashboards and indicators
  }

  @Override
  public void disabledInit() {} // commands running from another mode haven't been cancelled

  @Override
  public void disabledPeriodic() {}

  @Override
  public void disabledExit() {}

  @Override
  public void autonomousInit() {

    // commands running from another mode haven't been cancelled directly but may be interrupted by this command
    m_autonomousCommand = m_robotContainer.getAutonomousCommand();

    if (m_autonomousCommand != null) {
      m_autonomousCommand.schedule();
    }
  }

  @Override
  public void autonomousPeriodic() {}

  @Override
  public void autonomousExit() {

       if (m_autonomousCommand != null) {
      m_autonomousCommand.cancel();
    }
  }

  @Override
  public void teleopInit() { // commands running from another mode haven't been cancelled directly except the one below

    if (m_autonomousCommand != null) {
      m_autonomousCommand.cancel();
    }

    m_robotContainer.disjointedSequenceTestJob.schedule();
  }

  @Override
  public void teleopPeriodic() {
  }

  @Override
  public void teleopExit() {}

  @Override
  public void testInit() {

    CommandScheduler.getInstance().cancelAll();
  }

  @Override
  public void testPeriodic() {}

  @Override
  public void testExit() {}
}
