package dslab.transfer;


import dslab.mail.Email;
import dslab.util.Config;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.Scanner;

public class DMTPClient {

  private final String host;
  private final int port;
  private final String monitoringHost;
  private final int monitoringPort;


  public DMTPClient(String host, int port, Config config) {
    this.host = host;
    this.port = port;
    this.monitoringHost = config.getString("monitoring.host");
    this.monitoringPort = config.getInt("monitoring.port");
  }


  public boolean sendMessage(Email message) {
    try (Socket socket = new Socket(host, port);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         Scanner in = new Scanner(socket.getInputStream())) {

      System.out.println("Connecting to Mailbox Server at " + host + ":" + port);
      System.out.println("Sending email from: " + message.getFrom());

      in.nextLine();

      out.println("begin");
      in.nextLine();

      out.println("from " + message.getFrom());
      in.nextLine();

      for (String recipient : message.getTo()) {
        out.println("to " + recipient);
        in.nextLine();
      }

      out.println("subject " + message.getSubject());
      in.nextLine();

      out.println("data " + message.getData());
      in.nextLine();

      out.println("send");
      String response = in.nextLine();
      System.out.println("Server Response: " + response);

      if (response.startsWith("ok")) {
        notifyMonitoringServer(message);
        return true;
      } else {
        return false;
      }

    } catch (Exception e) {
      System.err.println("DMTPClient Error: " + e.getMessage());
      return false;
    }
  }

  private void notifyMonitoringServer(Email email) {
    try (DatagramSocket socket = new DatagramSocket()) {
      // Format: "<transfer-server-ip>:<port> <sender-email>"
      String localAddress = InetAddress.getLocalHost().getHostAddress();
      String message = localAddress + ":" + port + " " + email.getFrom();

      byte[] buffer = message.getBytes();
      InetAddress monitoringAddress = InetAddress.getByName(monitoringHost);

      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, monitoringAddress, monitoringPort);
      socket.send(packet);

    } catch (IOException e) {
      System.err.println("Failed to send statistics: " + e.getMessage());
    }
  }

}


