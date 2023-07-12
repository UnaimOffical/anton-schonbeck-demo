package se.elnama.core.serial.logger;

import com.fazecast.jSerialComm.SerialPort;
import java.util.concurrent.Callable;

public final class GetAvailablePorts implements Callable<String[]> {

  @Override
  public String[] call() {
    SerialPort[] serialPort = SerialPort.getCommPorts();
    String[] availablePorts = new String[serialPort.length];

    for (int i = 0; i < serialPort.length; i++) {
      availablePorts[i] = serialPort[i].getSystemPortName();
    }
    return availablePorts;
  }
}