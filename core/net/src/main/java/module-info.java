module net {
  requires lib.core;
  requires lib.sql;
  requires lib.util;
  requires java.sql;
  requires java.xml.bind;
  requires java.logging;

  exports se.elnama.core.net.logger;
  exports se.elnama.core.net.msg;
}