package frc.robot.subsystems;

import static edu.wpi.first.wpilibj2.command.Commands.sequence;
import static edu.wpi.first.wpilibj2.command.Commands.waitSeconds;

import frc.robot.subsystems.RobotSignals.LEDView;
import frc.robot.Color;
import frc.robot.LEDPattern;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;

/**
 * Demonstration of a Moore-Like FSM example that is similar to composing sequential and parallel
 * command groups. Triggers are used to help control state selection instead of other commands and
 * decorators.
 * 
 * This FSM example sequentially displays eight red LEDs first to last then back last to first
 *   1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8 -> 7 -> 6 -> 5 -> 4 -> 3 -> 2 -> 1 -> 2 ...
 * 
 * The triggers are 1/10 second clock divided into 14 bins for 14 triggers needed for this example
 * of the Knight Rider Kitt Scanner.
 * 
 * The scanner runs Disabled and Enabled so the FSM is started immediately with startFSM().
 * 
 * There is a stopFSM() function that is tested 10 seconds after starting and then 4 seconds later
 * the FSM is restarted and then has no end or off state; it keeps on flashing.
 * 
 * This example is a bit of a cheat. There are several complex states but they are all identical
 * except for a sequence number. That allows severe compression of code.  Normally each state would
 * have three separate Commands for the Entry, Exit, and Steady-state.
 *
 * 
 * This Moore-Like FSM is initially inactive and defines an Initial State when the FSM is activated.
 * 
 * Each state is composed of a State Entry Action, "Steady-State" Action and State Exit Action.
 * 
 * Each of these three actions is a command which can be a complex composed command.
 * 
 * Each steady state waits for a transition requiring the state to exit and transition to the next state.
 * (The trigger to transition can happen at any time as defined in the trigger and the entry action or
 * the steady state action could be running and be interrupted by the transition trigger. The exit action
 * will then run.)
 * 
 * A Transition from State to State is defined as the Current State + Trigger Condition yields current
 * state exit command, next state entry command, and then next state steady-state command.
 * 
 * This FSM does not demonstrate an End State. That is available by defining a State (and trigger
 * condition to get to that state) that ends the FSM in some manner. (The FSM can be activated and
 * deactivated with included start/stop methods but an end state is not defined; it just stops.)
 */

public class MooreLikeFSMMultiCommand extends SubsystemBase {

  private final LEDView m_robotSignals; // LED view where the output is displayed
  private double m_periodFactor; // changeable speed of the scanner
  private final Color m_color; // changeable color of the scanner
  private final double m_numberPeriods = 14.0; // number of periods or time bins to generate time-based triggers

  /**
   * Eight state FSM for the eight lights in the Knight Rider Kitt Scanner
   */ 
  private enum State
    {Light1, Light2, Light3, Light4, Light5, Light6, Light7, Light8, Inactive};

  private State m_initialState = State.Light1; // when the FSM is turned on - state starts here
  private State m_currentState = State.Inactive; // FSM isn't running initially

  /**
   * A Moore-Like FSM to display lights similar to the Knight Rider Kitt Scanner
   * 
   * @param the LED View for the Scanner
   * @param periodFactor Specify the speed of the Scanner (suggest about 10.0)
   * @param color Specify the color of the Scanner (suggest Color.kRed)
   */
  public MooreLikeFSMMultiCommand(LEDView robotSignals, double periodFactor, Color color) {
    m_robotSignals = robotSignals;
    m_periodFactor = periodFactor;
    m_color = color;
    createTransitions();
    startFSM(); // This FSM runs also disabled so start it immediately.
    // If the FSM doesn't run disabled, then start it in auto or periodic init or as a command anywhere.

    waitSeconds(10.) // test stopFSM function and then restart
      .andThen(this::stopFSM)
      .andThen(waitSeconds(4.))
      .andThen(this::startFSM)
        .ignoringDisable(true)
        .schedule();
  }

  /**
   * Activate all Transitions for this FSM through the use of triggers.
   * 
   * Trigger stores the current state, triggering event (clock value), and next state (Command) - that's a transition.
   * 
   * The transition is defined as current_state + event => next_state.
   * 
   * Generally Triggers can be "public" but these are dedicated to this FSM and there is no
   * intention of allowing outside use of them as that can disrupt the proper function of the FSM.
   * 
   * There may be potential optimizations.
   * 1. The triggers for this FSM could be in their own EventLoop and polled only when the subsystem is activated.
   * 2. If an event is unique to a transition, the check for the current state would not be necessary. In this
   * example none of the checks for the current state are needed because each event is unique. If the events were
   * not unique such as an event was merely every 14th of a period without further identity, then the current state
   * (as it is coded herein) would be needed.
   */
  private void createTransitions()
  {
    // Each transition is the current state to exit AND a timed event period that together
    // trigger a command to attain the next state.

    /*Light1Period0ToLight2*/ new Trigger(() -> m_currentState == State.Light1)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 0)
      .onTrue(transition(State.Light2, activateLightExit(State.Light1), activateLightEntry(State.Light2), activateLightSteadystate(State.Light2)));
    
    /*Light2Period1ToLight3*/ new Trigger(() -> m_currentState == State.Light2)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 1)
      .onTrue(transition(State.Light3, activateLightExit(State.Light2), activateLightEntry(State.Light3), activateLightSteadystate(State.Light3)));
    
    /*Light3Period2ToLight4*/ new Trigger(() -> m_currentState == State.Light3)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 2)
      .onTrue(transition(State.Light4, activateLightExit(State.Light3), activateLightEntry(State.Light4), activateLightSteadystate(State.Light4)));
    
    /*Light4Period3ToLight5*/ new Trigger(() -> m_currentState == State.Light4)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 3)
      .onTrue(transition(State.Light5, activateLightExit(State.Light4), activateLightEntry(State.Light5), activateLightSteadystate(State.Light5)));
    
    /*Light5Period4ToLight6*/ new Trigger(() -> m_currentState == State.Light5)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 4)
      .onTrue(transition(State.Light6, activateLightExit(State.Light5), activateLightEntry(State.Light6), activateLightSteadystate(State.Light6)));
    
    /*Light6Period5ToLight7*/ new Trigger(() -> m_currentState == State.Light6)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 5)
      .onTrue(transition(State.Light7, activateLightExit(State.Light6), activateLightEntry(State.Light7), activateLightSteadystate(State.Light7)));
    
    /*Light7Period6ToLight8*/ new Trigger(() -> m_currentState == State.Light7)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 6)
      .onTrue(transition(State.Light8, activateLightExit(State.Light7), activateLightEntry(State.Light8), activateLightSteadystate(State.Light8)));
    
    /*Light8Period7ToLight7*/ new Trigger(() -> m_currentState == State.Light8)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 7)
      .onTrue(transition(State.Light7, activateLightExit(State.Light8), activateLightEntry(State.Light7), activateLightSteadystate(State.Light7)));
    
    /*Light7Period8ToLight6*/ new Trigger(() -> m_currentState == State.Light7)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 8)
      .onTrue(transition(State.Light6, activateLightExit(State.Light7), activateLightEntry(State.Light6), activateLightSteadystate(State.Light6)));
    
    /*Light6Period9ToLight5*/ new Trigger(() -> m_currentState == State.Light6)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 9)
      .onTrue(transition(State.Light5, activateLightExit(State.Light6), activateLightEntry(State.Light5), activateLightSteadystate(State.Light5)));
    
    /*Light5Period10ToLight4*/ new Trigger(() -> m_currentState == State.Light5)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 10)
      .onTrue(transition(State.Light4, activateLightExit(State.Light5), activateLightEntry(State.Light4), activateLightSteadystate(State.Light4)));
    
    /*Light4Period11ToLight3*/ new Trigger(() -> m_currentState == State.Light4)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 11)
      .onTrue(transition(State.Light3, activateLightExit(State.Light4), activateLightEntry(State.Light3), activateLightSteadystate(State.Light3)));
    
    /*Light3Period12ToLight2*/ new Trigger(() -> m_currentState == State.Light3)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 12)
      .onTrue(transition(State.Light2, activateLightExit(State.Light3), activateLightEntry(State.Light2), activateLightSteadystate(State.Light2)));
    
    /*Light2Period13ToLight1*/ new Trigger(() -> m_currentState == State.Light2)
      .and(() -> (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 13)
      .onTrue(transition(State.Light1, activateLightExit(State.Light2), activateLightEntry(State.Light1), activateLightSteadystate(State.Light1)));

    // There is no final, end, or off State defined so no trigger to it.
    // Keep scanning until the FSM is deactivated.
  }

  /**
   * Start FSM at the initial state if it isn't running
   */
  public void startFSM()
  {
    // if the FSM has its own event loop, the loop could be started here
    if(m_currentState == State.Inactive)
    {
      transition(m_initialState, none(), activateLightEntry(m_initialState), activateLightSteadystate(m_initialState)).schedule();
    }
  }

  /**
   * Stop FSM immediately regardless of the state
   */
  public void stopFSM()
  {
    // if the FSM has its own event loop, the loop could be stopped here
    m_currentState = State.Inactive;
  }

  private Command none()
  {
      return Commands.runOnce(()->{});
  }
  
  /**
   * Turn on one bright LED in the string view.
   * Turn on its neighbors dimly. It appears smeared.
   * 
   * A simple cheat of the real Knight Rider Kitt Scanner which has a slowly
   * diminishing comet tail.  https://www.youtube.com/watch?v=usui7ECHPNQ
   * 
   * @param index which LED to turn on
   * @param colorForeground color of the on LED
   * @param colorBackground color of the off LEDs
   * @return Pattern to apply to the LED view
   */
  private static final LEDPattern oneLEDSmeared(int light, Color colorForeground, Color colorBackground) {
    int index = light;
    final int slightlyDim = 180;
    final int dim = 120;

    return (reader, writer) -> {
      int bufLen = reader.getLength();

      for (int led = 0; led < bufLen; led++) {
        if(led == index) {
          writer.setLED(led, colorForeground);              
        } else if((led == index-2 && index-2 >= 0) || (led == index+2 && index+2 < bufLen)) {
          writer.setRGB(led,
           (int) (colorForeground.red * dim),
           (int) (colorForeground.green * dim),
           (int) (colorForeground.blue * dim));
        } else if((led == index-1 && index-1 >= 0) || (led == index+1 && index+1 < bufLen)) {
          writer.setRGB(led,
           (int) (colorForeground.red * slightlyDim),
           (int) (colorForeground.green * slightlyDim),
           (int) (colorForeground.blue * slightlyDim));
        } else {
          writer.setLED(led, colorBackground);              
        }
      }
    };
  }

  /**
   * Default Command could cause havoc with the FSM - it depends; be careful
   * 
   * @param def Command will be ignored
   * @throws IllegalArgumentException immediately to prevent attempt to use
   */
  @Override
  public void setDefaultCommand(Command def) {
    throw new IllegalArgumentException("Default Command not allowed");
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
   * Factories for commands that turn on the correct LED every iteration until interrupted by a new time period.
   * 
   * <p>Commands can't be put into the State enum because
   * enums are static and these commands in general are non-static especially with the
   * "this" subsystem requirement.
   * 
   * <p>Generally Command factories can be "public" but this is dedicated to this FSM and there is
   * no intention of allowing outside use of it as that can disrupt the proper function of the FSM.
   */

  /**
   * Factory for the Entry Commands
   * All the Commands are the same except for the State identifier
   *  
   * @param state the associated state
   * @return the command to run that defines the state entry
   */
  private final Command activateLightEntry(State state)
  {
    return
      runOnce(
        // entry action
        () ->
          {
            // m_currentState = state; // The state has to record its "currentState" for use in the
                                    // transition since there is no other good way to get
                                    // automatically the current state for the Trigger.
            SmartDashboard.putString("FSM entry action "+this, m_currentState.name());
          })
          .withName(this.getClass().getSimpleName() + " " + m_color + " entry " + state)
          .ignoringDisable(true);
  }

  /**
   * Factory for the Steady-State Commands - turn on the LEDs
   * All the Commands are the same except for the State identifier
   * 
   * @param state the associated state
   * @return the command to run that defines the steady-state to wait for the next transition
   */
  private final Command activateLightSteadystate(State state)
  {
    return
      run(
        // steady-state action
        () ->
          {
            LEDPattern currentStateSignal = oneLEDSmeared(state.ordinal(), m_color, Color.kBlack);
            m_robotSignals.setSignal(currentStateSignal).schedule();
            SmartDashboard.putString("FSM steady-state action "+this, state.name());
          })
          .withName(this.getClass().getSimpleName() + " " + m_color + " steady-state " + state)
          .ignoringDisable(true);
  }

  /**
   * Factory for the Exit Commands
   * All the Commands are the same except for the State identifier
   * 
   * @param state the associated state
   * @return the command to run that defines the state exit
   */
  private final Command activateLightExit(State state)
  {
    return
      runOnce(
        // exit action
        () ->
          {
            Commands.print("exited active state");
            SmartDashboard.putString("FSM exit action "+this, state.name());
          })
          .withName(this.getClass().getSimpleName() + " " + m_color + " exit " + state)
          .ignoringDisable(true);
  }

  /**
   * Run all the actions of a state
   * @return
   */
  Command transition(State nextState, Command currentStateExit, Command nextStateEntry, Command nextSteadystate)
  {
    // couldn't figure out how to get the m_currentState into activateLightExit
    // so had to pass it in to transition to pass it to Exit
    return
      sequence(
        currentStateExit, // exit actions of previous state
        runOnce(() -> m_currentState = nextState).ignoringDisable(true), // record new state activated
        nextStateEntry, // entry actions of new state
        nextSteadystate) // steady state actions of new state
      .withName(this.getClass().getSimpleName() + " " + m_color + " transition to " + nextState)
      .ignoringDisable(true);
  }
}
