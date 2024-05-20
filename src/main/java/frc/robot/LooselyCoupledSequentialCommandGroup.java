package frc.robot;

// donation by CD @bovlb

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.WrapperCommand;
import edu.wpi.first.wpilibj2.command.button.InternalButton;
import edu.wpi.first.wpilibj2.command.button.Trigger;

/**
 * A command group that runs a list of commands in sequence.
 *
 * <p>Because each component command is individually composed, some rules for command
 *  compositions apply:
 *  commands that are passed to it cannot be added to any other composition or
 *  scheduled individually unless first released by
 *  CommandScheduler.getInstance().removeComposedCommand(test1).
 * 
 * <p>The difference with regular group compositions is this sequential group does not
 *  require at all time all of the subsystems its components require.
 */

class LooselyCoupledSequentialCommandGroup extends WrapperCommand {
    private final InternalButton m_trigger;
    private LooselyCoupledSequentialCommandGroup(Command command) {
        super(command);
        m_trigger = new InternalButton();
    }

    @Override
    public void initialize() {
        m_command.initialize();
        // the trigger could be reset here [m_trigger.setPressed(false);]
        // but since these triggers are well hidden they won't be reused
    }

    @Override
    public void end(boolean interrupted) {
        m_command.end(interrupted);
        m_trigger.setPressed(true); // indicate command ended and the next command is to be triggered
    }

    private Trigger getTrigger() {
        return m_trigger;
    }

    /**
     * 
     * @param commands - list of commands to run sequentially
     * 
     * <p>Each command is added to an individual composition group and is thus restricted but
     * the requirements of each component command are not required for the entire group process.
     * 
     * @return the first command to run and the remainder are automatically triggered
     */
    public static Command sequence(Command... commands) {

        if (commands.length == 0) return null;

        if (commands.length == 1) return commands[0];

        // all but last command get the new trigger command (augmented) that triggers the next command
        // all but first command triggered by the previous command
        // first doesn't have a previous and last doesn't have a next
        Command first = null;
        Trigger previousTrigger = null;
        int i = 0;

        for (Command command : commands) {

            int firstCommandIndex = 0;
            int lastCommandIndex = commands.length - 1;
            boolean atFirstcommand = i == firstCommandIndex;
            boolean atLastCommand = i == lastCommandIndex;
            LooselyCoupledSequentialCommandGroup augmented = null;

            if( ! atLastCommand) {
                augmented = new LooselyCoupledSequentialCommandGroup(command); // augment it with a trigger
            }

            if (atFirstcommand) {
                first = augmented; // first command is triggered externally by the user
                                   // thus has no previous trigger to set
            }
            else if (atLastCommand) {
                previousTrigger.onTrue(command); // the last command is triggered by the previous
                                                 // and won't be triggering the next command so no augmentation
            }
            else {
                previousTrigger.onTrue(augmented); // not the first command and not the last command
                // the middle commands triggered by their previous command and augmented to trigger the next command
            }

            if( ! atLastCommand) { // now there is a previous command and it will trigger this command
                previousTrigger = augmented.getTrigger();
            }

            ++i;
        }

        return first;

        }
    }
