module serial {
  requires lib.core;
  requires lib.util;
  requires com.fazecast.jSerialComm;
  requires java.prefs;
  requires java.logging;

  exports se.elnama.core.serial.msg;
  exports se.elnama.core.serial.logger;
}