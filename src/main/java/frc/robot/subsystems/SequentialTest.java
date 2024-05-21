package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class SequentialTest extends SubsystemBase {

/*
 * All Commands factories are "public."
 * 
 * All other methods are "private" to prevent other classes from forgetting to
 * add requirements of these resources if creating commands from these methods.
 */

    public SequentialTest () {

      setDefaultCommand(defaultCommand);
    }

    // variables used to produce the periodic output
    private String output = "";
    private String outputPrevious = "";
    private int repeatedOutputCount = 0;
    private final int repeatedOutputLimit = 250; // 5 seconds worth at 50 Hz loop frequency

    public void beforeCommands() {}

    public void afterCommands() {
      { // process "output" variable

    // Note that using this periodic output scheme - as implemented - causes the last output
    // value to persist through iterative periods if it hasn't changed. This behavior could be
    // changed with a check for stale data. It is problem dependent on what should be done -
    // persist output or no output.

      boolean newOutput = ! output.equals(outputPrevious);

      if ( ! newOutput) {
        repeatedOutputCount++;
      }

      if (newOutput || repeatedOutputCount >= repeatedOutputLimit) {
        if (repeatedOutputCount > 1) {
          System.out.println(" --- " + repeatedOutputCount + " times");
          repeatedOutputCount = 0;
        }
        else {
          System.out.println();
        }
        System.out.print(output);
      }

      outputPrevious = output;
      } // end process "output" variable

    }
/* output of sequential group test commands

Note that the default command is allowed to run between commands (during the waitSeconds) if using the separatedSequence.
Note that using the sequnce (not separatedSequence) the default command does not run between commands.

Warning at edu.wpi.first.wpilibj.DriverStation.reportJoystickUnpluggedWarning(DriverStation.java:1364): Joystick Button 2 on port 0 not available, check if controller is plugged in
 --- 250 times
Warning at edu.wpi.first.wpilibj.DriverStation.reportJoystickUnpluggedWarning(DriverStation.java:1364): Joystick Button 2 on port 0 not available, check if controller is plugged in
 --- 29 times
default command --- 250 times
default command --- 247 times
testing 1
default command --- 4 times
testing 2
default command --- 6 times
testing 3
default command --- 84 times
testing 4 --- 4 times
testing 5 --- 5 times
testing 6
default command --- 250 times
default command --- 250 times
default command --- 250 times
default command
*/

    /*
     * Public Commands
     */

    /**
     * Recommendation is don't use the setDefaultCommand because default commands are not
     * run inside composed commands.
     * If you insist then recommendation is don't use more than one default command
     * because it may not be obvious which default command is active (last one set is active)
     * Allow no more than one call to this set of the view (resource, subsystem) default command
     * You're on your own to remember if there is a default command set or not.
     */
    @Override
    public void setDefaultCommand(Command def) {

      if (getDefaultCommand() != null) {
        throw new IllegalArgumentException("Default Command already set");
      }

      if(def != null) {
        super.setDefaultCommand(def);
      }
    }

    private final Command defaultCommand = run(()->{
      output = "default command";
      });
    // private final Command defaultCommand = run(()->System.out.println("default command"));

    public final Command setTest(int testNumber) {

        return runOnce(()->{
          output = "testing " + testNumber;
        });
    }

    // note that the Commands.print("testing " + testNumber) does not require a subsystem which
    // is needed for this test so System.out.print() was used more directly.
    // And related note that Command factories cannot be "static" since they require the subsystem
    // instance ("this"; implied in this example by the runOnce() command).
}