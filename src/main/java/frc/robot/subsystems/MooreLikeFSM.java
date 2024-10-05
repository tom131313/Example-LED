package frc.robot.subsystems;

import frc.robot.subsystems.RobotSignals.LEDView;
import frc.robot.Color;
import frc.robot.LEDPattern;

import edu.wpi.first.wpilibj.Timer;
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
 */
public class MooreLikeFSM extends SubsystemBase {

  private final LEDView m_robotSignals; // LED view where the output is displayed
  private double m_periodFactor = 10.0; // changeable speed of the scanner; Trigger lambda throws uninitialized error if final and not set here.
  private final Color m_color; // changeable color of the scanner
  private final double m_numberPeriods = 14.0; // number of periods or time bins to generate time-based triggers

  /**
   * Eight state FSM for the eight lights in the Knight Rider Kitt Scanner
   */ 
  private enum State
    {Light1, Light2, Light3, Light4, Light5, Light6, Light7, Light8};

  // Since triggering is based on the clock the initial state is not fully relevant
  // because we have to wait for the clock to roll around to the right time and then it
  // triggers the state following the initial state. Too simple to work exactly as you might
  // expect but it's set so fast you might not notice it's not "right".
  private State m_initialState = State.Light1;
  private State m_currentState = m_initialState;

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
   * There may be potential optimizations. The triggers for this FSM could be in their own
   * EventLoop and polled only when the subsystem is activated. If an event is unique to a
   * transition the check for the current state would not be necessary.
   */
  private void createTransitions()
  {
    /*Light1Period0ToLight2*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 0).and(()-> m_currentState == State.Light1).onTrue(activateLight2());
    /*Light2Period1ToLight3*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 1).and(()-> m_currentState == State.Light2).onTrue(activateLight3());
    /*Light3Period2ToLight4*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 2).and(()-> m_currentState == State.Light3).onTrue(activateLight4());
    /*Light4Period3ToLight5*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 3).and(()-> m_currentState == State.Light4).onTrue(activateLight5());
    /*Light5Period4ToLight6*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 4).and(()-> m_currentState == State.Light5).onTrue(activateLight6());
    /*Light6Period5ToLight7*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 5).and(()-> m_currentState == State.Light6).onTrue(activateLight7());
    /*Light7Period6ToLight8*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 6).and(()-> m_currentState == State.Light7).onTrue(activateLight8());
    /*Light8Period7ToLight7*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 7).and(()-> m_currentState == State.Light8).onTrue(activateLight7());
    /*Light7Period8ToLight6*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 8).and(()-> m_currentState == State.Light7).onTrue(activateLight6());
    /*Light6Period9ToLight5*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 9).and(()-> m_currentState == State.Light6).onTrue(activateLight5());
    /*Light5Period10ToLight4*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 10).and(()-> m_currentState == State.Light5).onTrue(activateLight4());
    /*Light4Period11ToLight3*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 11).and(()-> m_currentState == State.Light4).onTrue(activateLight3());
    /*Light3Period12ToLight2*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 12).and(()-> m_currentState == State.Light3).onTrue(activateLight2());
    /*Light2Period13ToLight1*/new Trigger(()->(int) (Timer.getFPGATimestamp()*m_periodFactor % m_numberPeriods) == 13).and(()-> m_currentState == State.Light2).onTrue(activateLight1());
  }

  /**
   * These commands (factories) define the states.
   * 
   * <p>They can't be put into the State enum because
   * enums are static and these commands in general are non-static especially with the automatic
   * "this" subsystem requirement.
   * 
   * <p>Generally Command factories can be "public" but these are dedicated to this FSM and there is
   * no intention of allowing outside use of them as that can disrupt the proper function of the
   * FSM.
   * 
   * <p>The states have to record their "currentState" for use in the transition since there is no
   * other good way to get automatically the current state.
   * 
   * @return Command that records its state name and turns on the correct LED
   */

  private final Command activateLight1() {
    return
      defineState(State.Light1,
        ()->{
            LEDPattern currentStateSignal = oneLEDSmeared(0, m_color, Color.kBlack);
            m_robotSignals.setSignal(currentStateSignal).schedule();
      });
  }

  private final Command activateLight2() {
    return
      defineState(State.Light2,
        ()->{
            LEDPattern currentStateSignal = oneLEDSmeared(1, m_color, Color.kBlack);
            m_robotSignals.setSignal(currentStateSignal).schedule();
      });
  }

  private final Command activateLight3() {
    return
      defineState(State.Light3,      
        ()->{
            LEDPattern currentStateSignal = oneLEDSmeared(2, m_color, Color.kBlack);
            m_robotSignals.setSignal(currentStateSignal).schedule();
      });
  }

  private final Command activateLight4() {
    return
      defineState(State.Light4,
        ()->{
            LEDPattern currentStateSignal = oneLEDSmeared(3, m_color, Color.kBlack);
            m_robotSignals.setSignal(currentStateSignal).schedule();
      });
  }

  private final Command activateLight5() {
    return
      defineState(State.Light5,
        ()->{
            LEDPattern currentStateSignal = oneLEDSmeared(4, m_color, Color.kBlack);
            m_robotSignals.setSignal(currentStateSignal).schedule();
      });
  }

  private final Command activateLight6() {
    return
      defineState(State.Light6,
        ()->{
            LEDPattern currentStateSignal = oneLEDSmeared(5, m_color, Color.kBlack);
            m_robotSignals.setSignal(currentStateSignal).schedule();
      });
    }

  private final Command activateLight7() {
    return
      defineState(State.Light7,
        ()->{
            LEDPattern currentStateSignal = oneLEDSmeared(6, m_color, Color.kBlack);
            m_robotSignals.setSignal(currentStateSignal).schedule();
      });
  }

  private final Command activateLight8() {
    return
      defineState(State.Light8,
        ()->{
            LEDPattern currentStateSignal = oneLEDSmeared(7, m_color, Color.kBlack);
            m_robotSignals.setSignal(currentStateSignal).schedule();
      });
  }

  /**
   * Constructs a command that sets the current state and runs an action every iteration until interrupted.
   *
   * @param state State associated with the command as the current state
   * @param run the action to run every iteration
   * @return the command to run that defines the state
   */
  private final Command defineState(State state, Runnable run) {
    return new FunctionalCommand(
        () -> m_currentState = state, run, interrupted -> {}, () -> false, this)
        .withName("Moore-Like " + m_color + " " + state);
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
  private static final LEDPattern oneLEDSmeared(int index, Color colorForeground, Color colorBackground) {
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
