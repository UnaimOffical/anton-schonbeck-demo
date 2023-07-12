package se.elnama.core.net.msg;

import static se.elnama.lib.util.TimeUtils.getDateTimeFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import se.elnama.lib.sql.PrintTabularResult;

public final class NetMessageStoreSql implements PrintTabularResult {

  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  private static final String USERNAME = "";
  private static final String PASSWORD = getPassword();
  private static final String NODE_URLS = "";

  private static String getPassword() {
  }

  public boolean callInsertLog(NetMessage message) {
    try (Connection conn = DriverManager.getConnection(NODE_URLS, USERNAME, PASSWORD)) {
      callableInsertLog(conn, message.getId(), message.getPort(), message.getLength(),
          message.getMessageBlocks(), message.getIpAddress());
      return true;
    } catch (SQLException exc) {
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
      return false;
    }
  }

  public boolean callSelectLog(int id, String time) {
    try (Connection conn = DriverManager.getConnection(NODE_URLS, USERNAME, PASSWORD)) {
      callableSelectLog(conn, id, time);
      return true;
    } catch (SQLException exc) {
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
      return false;
    }
  }

  private void callableInsertLog(Connection conn, int serialNr, int portNr, int length,
      String[] block, String ipAddress) {
    String insertLogCall = "{CALL insert_log(?,?,?,?,?,?,?,?,?,?)}";

    try (CallableStatement insertLogStmt = conn.prepareCall(insertLogCall)) {
      insertLogStmt.setInt(1, serialNr);
      insertLogStmt.setString(2, block[0]);
      insertLogStmt.setString(3, block[1]);
      insertLogStmt.setString(4, block[2]);
      insertLogStmt.setString(5, block[3]);
      insertLogStmt.setString(6, block[4]);
      insertLogStmt.setString(7, block[5]);
      insertLogStmt.setInt(8, length);
      insertLogStmt.setString(9, ipAddress);
      insertLogStmt.setInt(10, portNr);
      insertLogStmt.executeQuery();
    } catch (SQLException exc) {
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
  }

  private void callableSelectLog(Connection conn, int id, String times) {
    String selectLogCall = "{CALL select_log(?,?)}";
    ifNotExistCreateDirectory();
    File file = new File("./log/" + getDateTimeFile() + "-log.txt");

    try (CallableStatement selectLogStmt = conn.prepareCall(selectLogCall,
        ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_UPDATABLE); PrintWriter pw = new PrintWriter(
        new BufferedWriter(new FileWriter(file)))) {

      selectLogStmt.setInt(1, id);
      selectLogStmt.setTimestamp(2, Timestamp.valueOf(times));

      ResultSet rs = selectLogStmt.executeQuery();
      printResultSetDefault(rs, pw);
    } catch (SQLException | IOException exc) {
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
  }

  private void ifNotExistCreateDirectory() {
    File directory = new File("./log/");

    if (!directory.exists()) {
      directory.mkdirs();
    }
  }

  public enum SendRequisite {
    INSTANCE;
    private String ipAddress;
    private int port;
    private byte[] block1;

    SendRequisite() {
    }

    public SendRequisite getInstance() {
      return INSTANCE;
    }

    public void setSendRequisite(int id) {
      try (Connection conn = DriverManager.getConnection(NODE_URLS, USERNAME, PASSWORD)) {
        callableSelectSendReq(conn, id);
      } catch (SQLException exc) {
        LOGGER.log(Level.INFO, exc.getMessage(), exc);
      }
    }

    public String getIpAddress() {
      return ipAddress;
    }

    public int getPort() {
      return port;
    }

    public byte[] getBlock1() {
      return block1;
    }

    private void setBlock1(String block) {
      block1 = convertToHexBinary(block);
    }

    private byte[] convertToHexBinary(String hex) {
      String hexNoSpaces = hex.replaceAll("\\s", "");
      return DatatypeConverter.parseHexBinary(hexNoSpaces);
    }

    private void callableSelectSendReq(Connection conn, int id) {
      String selectSendReqCall = "{CALL select_log_sendRequisite(?)}";
      SendRequisite sendRequisite = SendRequisite.INSTANCE.getInstance();

      try (CallableStatement selectSendReqStmt = conn.prepareCall(selectSendReqCall)) {
        selectSendReqStmt.setInt(1, id);
        ResultSet rs = selectSendReqStmt.executeQuery();

        while (rs.next()) {
          sendRequisite.setBlock1(rs.getString(1));
          ipAddress = rs.getString(2);
          port = rs.getInt(3);
        }
      } catch (SQLException exc) {
        LOGGER.log(Level.INFO, exc.getMessage(), exc);
      }
    }
  }
}