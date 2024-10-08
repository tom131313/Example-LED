package frc.robot.subsystems;

import frc.robot.subsystems.RobotSignals.LEDView;
import frc.robot.Color;
import frc.robot.LEDPattern;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.FunctionalCommand;
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
 * The FSM starts immediately without need of triggering.
 * 
 * The FSM has no end or off state; it keeps on flashing.
 * 
 * 
 * This Moore-Like FSM is initially disabled and defines an Initial State when the FSM is enabled.
 * 
 * Each state is composed of a State Entry Action, "Steady-State" Action and State Exit Action.
 * 
 * Each state waits for a transition requiring the state to exit.
 * 
 * A Transition from State to State is defined as the Current State + Trigger Condition yields Next State.
 * 
 * This FSM does not demonstrate an End State. That is available by defining a State (and trigger
 * condition to get to that state) that ends, in some manner, the FSM.
 */
public class MooreLikeFSM extends SubsystemBase {

  private final LEDView m_robotSignals; // LED view where the output is displayed
  private double m_periodFactor; // changeable speed of the scanner
  private final Color m_color; // changeable color of the scanner
  private final double m_numberPeriods = 14.0; // number of periods or time bins to generate time-based triggers

  /**
   * Eight state FSM for the eight lights in the Knight Rider Kitt Scanner
   */ 
  private enum State
    {Light1, Light2, Light3, Light4, Light5, Light6, Light7, Light8};

  private State m_initialState = State.Light1;
  private State m_currentState;

  /**
   * A Moore-Like FSM to display lights similar to the Knight Rider Kitt Scanner
   * 
   * @param the LED View for the Scanner
   * @param periodFactor Specify the speed of the Scanner (suggest about 10.0)
   * @param color Specify the color of the Scanner (suggest Color.kRed)
   */
  public MooreLikeFSM(LEDView robotSignals, double periodFactor, Color color) {
    m_robotSignals = robotSignals;
    m_periodFactor = periodFactor;
    m_color = color;
    createTransitions();
  }

  /**
   * Activate all Transitions for this FSM through the use of triggers.
   * 
   * Trigger stores the triggering event (clock value), current state and next state (Command) - that's a transition.
   * 
   * The transition is defined as event + current_state => next_state
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
    // Each transition is a timed event period AND current state that triggers a command to attain the next state

    /*NotActiveToInitialState*/ activateLight(m_initialState).schedule(); // activate FSM immediately - don't wait for a trigger

    /*Light1Period0ToLight2*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 0)
      .and(()-> m_currentState == State.Light1)
      .onTrue(activateLight(State.Light2));
    
    /*Light2Period1ToLight3*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 1)
      .and(()-> m_currentState == State.Light2)
      .onTrue(activateLight(State.Light3));
    
    /*Light3Period2ToLight4*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 2)
      .and(()-> m_currentState == State.Light3)
      .onTrue(activateLight(State.Light4));
    
    /*Light4Period3ToLight5*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 3)
      .and(()-> m_currentState == State.Light4)
      .onTrue(activateLight(State.Light5));
    
    /*Light5Period4ToLight6*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 4)
      .and(()-> m_currentState == State.Light5)
      .onTrue(activateLight(State.Light6));
    
    /*Light6Period5ToLight7*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 5)
      .and(()-> m_currentState == State.Light6)
      .onTrue(activateLight(State.Light7));
    
    /*Light7Period6ToLight8*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 6)
      .and(()-> m_currentState == State.Light7)
      .onTrue(activateLight(State.Light8));
    
    /*Light8Period7ToLight7*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 7)
      .and(()-> m_currentState == State.Light8)
      .onTrue(activateLight(State.Light7));
    
    /*Light7Period8ToLight6*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 8)
      .and(()-> m_currentState == State.Light7)
      .onTrue(activateLight(State.Light6));
    
    /*Light6Period9ToLight5*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 9)
      .and(()-> m_currentState == State.Light6)
      .onTrue(activateLight(State.Light5));
    
    /*Light5Period10ToLight4*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 10)
      .and(()-> m_currentState == State.Light5)
      .onTrue(activateLight(State.Light4));
    
    /*Light4Period11ToLight3*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 11)
      .and(()-> m_currentState == State.Light4)
      .onTrue(activateLight(State.Light3));
    
    /*Light3Period12ToLight2*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 12)
      .and(()-> m_currentState == State.Light3)
      .onTrue(activateLight(State.Light2));
    
    /*Light2Period13ToLight1*/ new Trigger(() ->
      (int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 13)
      .and(()-> m_currentState == State.Light2)
      .onTrue(activateLight(State.Light1));

    // There is no final, end, or off state defined so no trigger for it; this just keeps flashing
  }

  /**
   * Factory for commands that turn on the correct LED every iteration until interrupted by a new time period.
   * 
   * <p>Commands can't be put into the State enum because
   * enums are static and these commands in general are non-static especially with the
   * "this" subsystem requirement.
   * 
   * <p>Generally Command factories can be "public" but this is dedicated to this FSM and there is
   * no intention of allowing outside use of it as that can disrupt the proper function of the
   * FSM.
   * 
   * @param light the light number
   * @return the command to run that defines the state - turns on the correct LED
   */
  private final Command activateLight(State state) {
    return new FunctionalCommand(
        () ->
          {
            m_currentState = state; // The state has to record its "currentState" for use in the
                                    // transition since there is no other good way to get
                                    // automatically the current state for the Trigger.
            SmartDashboard.putString("FSM entry action "+this, state.name());
          },
        () ->
          {
            LEDPattern currentStateSignal = oneLEDSmeared(state.ordinal(), m_color, Color.kBlack);
            m_robotSignals.setSignal(currentStateSignal).schedule();
            SmartDashboard.putString("FSM steady-state action "+this, state.name());
          },
        interrupted ->
          {
            SmartDashboard.putString("FSM exit action "+this, state.name());
          },
        () -> false, // finishing can be only by "external" transitions and not by this indicator
        this)
        .ignoringDisable(true)
        .withName("Moore-Like " + m_color + " " + state); // "this" is more precise discriminator
                                                    // but "m_color" is prettier and likely as good
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
}
