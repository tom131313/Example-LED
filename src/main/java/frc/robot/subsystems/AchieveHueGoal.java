// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

/*
 * Example of a subsystem composed only of a goal wrapped in its
 * implementer class to achieve that goal.
 * 
 * Note that this is a simple contrived example based on a PID controller.
 * There may be better ways to do PID controller entirely within a subsystem.
 * 
 * This example uses the Xbox right trigger (injected from RobotContainer)
 * to set a goal of a position on the color wheel displayed on the LEDs.
 * 
 * The PID is tuned to slowly converge on the requested color.
 */

import java.util.function.DoubleSupplier;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Color;
import frc.robot.LEDPattern;
import frc.robot.subsystems.RobotSignals.LEDView;

public class AchieveHueGoal {
    
    // PID initialization.

    // It starts running immediately controlling with the initial
    // "currentStateHue" and "hueSetpoint" - choose carefully!

    // There is no running command to make new setpoints until
    // the Xbox right trigger axis is pressed. This is handled in
    // RobotContainer.java and this class shouldn't even know that
    // to be able to comment here on how the setpoint is determined.

    private final double kP = 0.025;
    private final double kI = 0.0;
    private final double kD = 0.0;
    private PIDController HueController = new PIDController(kP, kI, kD);
    private double minimumHue = 0.;
    private double maximumHue = 180.;
    private double hueSetpoint = 0.;
    private double currentStateHue = 0.;
    
    private LEDView robotSignals; // where the output is displayed

    public HueGoal hueGoal = new HueGoal(); // subsystem protected goal

    public AchieveHueGoal(LEDView robotSignals) {
        this.robotSignals = robotSignals;
    }

    // Example of methods and triggers that the system will require are put here.

    // Methods that change the system should be "private".
    // Methods and triggers that inquire about the system should be "public".

    // This particular state inquiry is an example only and isn't used for the demonstration.
    public final Trigger atHueGoal = new Trigger(this::isAtHueGoal);

    private boolean isAtHueGoal() {

        return HueController.atSetpoint();
    }

    public void beforeCommands() {}

    public void afterCommands() {
        currentStateHue = MathUtil.clamp(currentStateHue + HueController.calculate(currentStateHue, hueSetpoint), minimumHue, maximumHue);
        LEDPattern persistentPatternDemo = LEDPattern.solid(Color.fromHSV((int)currentStateHue, 200, 200));// display state;
        robotSignals.setSignal(persistentPatternDemo).schedule(); // access to the LEDS is only by command in this example so do it that way.
    }

    /**
     * subsystem to lock the resource if a command is running and provide a default command
     * Note that the technique of restricting use of the default command is not implemented
     * in this example. That is insecure and anybody can change it that has access to the
     * instance. The default command is disabled in this example.
     * 
     * A decision must be made on how to set the goal. The controller is always running.
     * Does the goal supplier have to always be running or is it set and forget until a new
     * goal is needed? The default command could be activated to provide the goal if no
     * other goal supplier is running. If set once and forget is used then it is inappropriate
     * to use a default command as that would immediately reset to the default after setting
     * a goal.
     */
    public class HueGoal extends SubsystemBase {

        private HueGoal() {

        // This subsystem could run "perpetually" with a pre-determined hue goal but it's disabled in
        // this example in favor of no default command to prevent assuming there is one always running.
        // That means the last setpoint is running as no default takes over. For the Xbox trigger, that
        // goes to 0 when released but again we're not supposed to know that from RobotContainer.java.
        // Command defaultCommand = 
        //         Commands.run( ()-> hueSetpoint = defaultHueGoal , this);
        // setDefaultCommand(defaultCommand);
        }

        /**
         * Example of how to disallow default command
         */
        @Override
        public void setDefaultCommand(Command def) {

            throw new IllegalArgumentException("Default Command not allowed");
        }

        /**
         * Set the goal.
         * 
         * @param goal hue 0 to 180
         * @return command that can be used to set the goal
         */
        public Command setHueGoal(double goal) {

            return run(()-> hueSetpoint = goal);
        }

        public Command setHueGoal(DoubleSupplier goal) {

            return run(()-> hueSetpoint = goal.getAsDouble());
        }   
    }
}
