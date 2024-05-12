// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.wpilibj.AddressableLED;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class LEDSusbystem extends SubsystemBase {
  private final AddressableLED strip;
  private final AddressableLEDBuffer buffer;
   
  public LEDSusbystem(int length, int port) {
    // initialize the physical LED strip length
    strip = new AddressableLED(port);
    strip.setLength(length);

    buffer = new AddressableLEDBuffer(length);

    // start the physical LED strip output
    strip.start();
  }

  @Override
  public void periodic() {
    strip.setData(buffer);
  }
}
