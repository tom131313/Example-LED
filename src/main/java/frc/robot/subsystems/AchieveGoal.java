package frc.robot.subsystems;

import java.util.function.DoubleConsumer;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Color;
import frc.robot.LEDPattern;
import frc.robot.subsystems.RobotSignals.LEDView;

public class AchieveGoal {
    
    //for PID
    private final double kP = 0.75;
    private final double kI = 0.0;
    private final double kD = 0.0;
    private PIDController PIDcontroller = new PIDController(kP, kI, kD);
    private double setpointGoal;
    private double defaultHue = 90;
    
    private LEDView robotSignals;

    Goal goal = new Goal();

    public AchieveGoal(LEDView robotSignals) {
        this.robotSignals = robotSignals;
        PIDcontroller.setTolerance(2.);
        PIDcontroller.enableContinuousInput(0., 180.);
    }

    public boolean isAtHue() {

        return PIDcontroller.atSetpoint();
    }
    
    public void afterCommands() {
        double currentStateHue = PIDcontroller.calculate(kI, setpointGoal);
        LEDPattern persistentPatternDemo = LEDPattern.solid(Color.fromHSV((int)currentStateHue, 200, 200));
        robotSignals.setSignal(persistentPatternDemo).schedule(); // access to the LEDS is only by command so do it that way.
    }

    public class Goal extends SubsystemBase {

        private Goal() {
        
            Command defaultCommand = Commands.run(()->setpointGoal = defaultHue, this);
            goal.setDefaultCommand(defaultCommand); 
        }

        Command setSetpoint(Double setpoint) {
            // DoubleConsumer dc = (setSetpoint)-> this.setpointGoal = setSetpoint;
            return run(()-> setpointGoal = setpoint);
        }

    }
}
