// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

// import edu.wpi.first.wpilibj.AddressableLED;
// import edu.wpi.first.wpilibj.AddressableLEDBuffer;
// import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.AddressableLED;
import frc.robot.AddressableLEDBuffer;
import frc.robot.Color;
import frc.robot.PeriodicTask;

/*
 * All Commands factories (except the default command) are "public."
 * 
 * All other methods are "private" to prevent other classes from forgetting to
 * add requirements of these resources if creating commands from these methods.
 */

public class RobotSignals  {

// Can't overload methods using generic interfaces parameters like Supplier
// so make our own interfaces to use in overloads

/**
 * Represents a supplier of Color.
 */
@FunctionalInterface
public interface ColorSupplier {

    /**
     * Gets a result.
     *
     * @return a result
     */
    Color get();
}

/**
 * Represents a supplier of AddressableLEDBuffer.
 */
@FunctionalInterface
public interface AddressableLEDBufferSupplier {

    /**
     * Gets a result.
     *
     * @return a result
     */
    AddressableLEDBuffer get();
}

  private final AddressableLED strip;

  // layout by LED number of the single physical buffer
  // into two logical views (segments; subsystems) called Main and Top
  // Assume views don't overlaps
  // Asume a view is not discontinuous.
  // Views do not have to be adjacent to another (gaps are allowed)
  // Assume only one view starts at 0.
  // Buffer is not cleared.
  // It's just an example.
  // (New anticipated functions for LED views may make this all easy and better.)
  private final AddressableLEDBuffer bufferLED;
  private final int firstMainLED = 11; // inclusive
  private final int lastMainLED = 32; // inclusive
  private final int firstTopLED = 0; // inclusive
  private final int lastTopLED = 10; // inclusive
  private final Color TopDefault = new Color(0., 0., 1.);
  private final Color MainDefault = new Color(0., 1., 1.);
  public LEDView Top;
  public LEDView Main;

  public RobotSignals(int port, PeriodicTask periodicTask) {
  
    // start updating the physical LED strip  
    int length = Math.max(lastTopLED, lastMainLED) + 1; // simplistic view of 2 segments - one starts at 0
    strip = new AddressableLED(port);
    strip.setLength(length);
    strip.start();

    bufferLED = new AddressableLEDBuffer(length); // buffer for the all LEDs

    // createthe two resources (subsystems) as views of the primary buffer
    Top = new LEDView(lastTopLED - firstTopLED + 1, TopDefault);
    Main = new LEDView(lastMainLED - firstMainLED + 1, MainDefault);

    // start the periodic process to send all views to the buffer and then to the LED
    periodicTask.register(this::mergeViewsAndUpdate, 0.24, 0.019);
  }

  private void mergeViewsAndUpdate() {
    int indexSource; // index of the source buffer

    // deep copy Top view to LED buffer
    indexSource = 0;
    for(int indexDestination = firstTopLED; indexDestination <= lastTopLED; indexDestination++)
    {
      bufferLED.setLED(indexDestination, Top.view.getLED(indexSource++));
    }

    // deep copy Main view to LED buffer
    indexSource = 0;
    for(int indexDestination = firstMainLED; indexDestination <= lastMainLED; indexDestination++)
    {
      bufferLED.setLED(indexDestination, Main.view.getLED(indexSource++));
    }

    // update LEDs
    strip.setData(bufferLED);
  }

  /**
   * LED view resource (subsystem)
   */
  public class LEDView extends SubsystemBase {

    private final int length;
    private final AddressableLEDBuffer view;

    private LEDView(int length, Color defaultColor) {
      this.length = length;
      view = new AddressableLEDBuffer(length);

      setDefaultCommand(Default(defaultColor));
    }

    /**
     * Sets all LEDs in the view to the same parameter color
     * @param color
     */
    private void setSignalView(Color color)
    {
      for(int index = 0; index < length; index++) {
        view.setLED(index, color);    
      }
    }

    /**
     * Sets the LEDS in a view to the parameter buffer
     * @param buffer
     */
    private void setSignalView(AddressableLEDBuffer buffer)
    {
      // deep copy parameter buffer to view
      for(int index = 0; index < length; index++)
      {
        view.setLED(index, buffer.getLED(index));
      }
    }

    private Command Default(Color color) {
      return
        run(()->setSignalView(color))
          .ignoringDisable(true)
          .withName("LedDefault");
    }
/*
 * Public Commands
 */
    public Command setSignal(Color color) {
      return
        run(()->setSignalView(color))
          .ignoringDisable(true)
          .withName("LedSetC");
    }

    public Command setSignal(ColorSupplier color) {
      return
        run(()->setSignalView(color.get()))
          .ignoringDisable(true)
          .withName("LedSetSC");
    }

    public Command setSignal(AddressableLEDBuffer buffer) {
      return
        run(()->setSignalView(buffer))
          .ignoringDisable(true)
          .withName("LedSetB");
    }

    public Command setSignal(AddressableLEDBufferSupplier buffer) {
        return
          run(()->setSignalView(buffer.get()))
            .ignoringDisable(true)
            .withName("LedSetSB");
    }

    public Command runPattern(LEDPattern pattern) {
      return run(() -> pattern.applyTo());
    }
  } // End LEDView
}



/* parking lot

  import static edu.wpi.first.units.Units.Seconds;
  private final AddressableLEDBufferView view;
  view = led.createView(...);
  private final AddressableLEDBufferView view;
  view = led.createView(...);
  private final LEDPattern disabled = LEDPattern.solid(Color.kRed).breathe(Seconds.of(2));
  private final LEDPattern enabled = LEDPattern.solid(Color.kGreen).breathe(Seconds.of(2));
  private final LEDPattern defaultPattern = () -> (DriverStation.isDisabled() ? disabled : enabled).applyTo();
  private final LEDPattern blink = LEDPattern.solid(Color.kMagenta).blink(Seconds.of(0.2));
  private final LEDPattern orange = LEDPattern.solid(Color.kOrange);

  ledMain.setDefaultCommand(ledMain.runPattern(defaultPattern).ignoringDisable(true).withName("LedDefaultMain"));
  ledTop.setDefaultCommand(ledTop.runPattern(defaultPattern).ignoringDisable(true).withName("LedDefaultTop"));

C:\Users\RKT\frc\FRC2024\allwpilib-main\wpilibjExamples\src\main\java\edu\wpi\first\wpilibj\examples\addressableled\Main.java
    private void rainbow() {
    // For every pixel
    for (var i = 0; i < m_ledBuffer.getLength(); i++) {
      // Calculate the hue - hue is easier for rainbows because the color
      // shape is a circle so only one value needs to precess
      final var hue = (m_rainbowFirstPixelHue + (i * 180 / m_ledBuffer.getLength())) % 180;
      // Set the value
      m_ledBuffer.setHSV(i, hue, 255, 128);
    }
    // Increase by to make the rainbow "move"
    m_rainbowFirstPixelHue += 3;
    // Check bounds
    m_rainbowFirstPixelHue %= 180;
  }


  ///// Sam's Robot.java
  // Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package edu.wpi.first.wpilibj.examples.addressableled;

import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.MetersPerSecond;

import edu.wpi.first.units.Distance;
import edu.wpi.first.units.Measure;
import edu.wpi.first.wpilibj.AddressableLED;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.LEDPattern;
import edu.wpi.first.wpilibj.TimedRobot;

public class Robot extends TimedRobot {
  private AddressableLED m_led;
  private AddressableLEDBuffer m_ledBuffer;

  // Create an LED pattern that will display a rainbow across
  // all hues at maximum saturation and half brightness
  private final LEDPattern m_rainbow = LEDPattern.rainbow(255, 128);

  // Our LED strip has a density of 120 LEDs per meter
  private static final Measure<Distance> kLedSpacing = Meters.of(1 / 120.0);

  // Create a new pattern that scrolls the rainbow pattern across the LED strip, moving at a speed
  // of 1 meter per second.
  private final LEDPattern m_scrollingRainbow =
      m_rainbow.scrollAtAbsoluteSpeed(MetersPerSecond.of(1), kLedSpacing);

  @Override
  public void robotInit() {
    // PWM port 9
    // Must be a PWM header, not MXP or DIO
    m_led = new AddressableLED(9);

    // Reuse buffer
    // Default to a length of 60, start empty output
    // Length is expensive to set, so only set it once, then just update data
    m_ledBuffer = new AddressableLEDBuffer(60);
    m_led.setLength(m_ledBuffer.getLength());

    // Set the data
    m_led.setData(m_ledBuffer);
    m_led.start();
  }

  @Override
  public void robotPeriodic() {
    // Update the buffer with the rainbow animation
    m_scrollingRainbow.applyTo(m_ledBuffer);
    // Set the LEDs
    m_led.setData(m_ledBuffer);
  }
}

 */