package dslab.transfer;

import dslab.mail.Email;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class DMTPServer implements Runnable {

  private final Socket socket;
  private final MessageForwarder forwarder;

  public DMTPServer(Socket socket, MessageForwarder forwarder) {
    this.socket = socket;
    this.forwarder = forwarder;
  }

  @Override
  public void run() {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

      out.println("ok DMTP");

      String from = null;
      List<String> recipients = new ArrayList<>();
      String subject = null;
      String data = null;
      boolean inMessage = false;

      String line;
      while ((line = in.readLine()) != null) {
        line = line.trim();

        if (line.equalsIgnoreCase("quit")) {
          out.println("ok bye");
          break;
        } else if (line.equalsIgnoreCase("begin")) {
          from = null;
          recipients.clear();
          subject = null;
          data = null;
          inMessage = true;
          out.println("ok");
        } else if (line.startsWith("from ") && inMessage) {
          from = line.substring(5).trim();
          out.println("ok");
        } else if (line.startsWith("to ") && inMessage) {
          String[] recipientArray = line.substring(3).trim().split(",");
          for (String recipient : recipientArray) {
            recipients.add(recipient.trim());
          }
          out.println("ok " + recipients.size()); // Response includes recipient count
        } else if (line.startsWith("subject ") && inMessage) {
          subject = line.substring(8).trim();
          out.println("ok");
        } else if (line.startsWith("data ") && inMessage) {
          data = line.substring(5).trim();
          out.println("ok");
        } else if (line.equalsIgnoreCase("send") && inMessage) {
          if (from == null) {
            out.println("error no sender");
          } else if (recipients.isEmpty()) {
            out.println("error no recipient");
          } else if (subject == null) {
            out.println("error no subject");
          } else if (data == null) {
            out.println("error no data");
          } else {
            Email message = new Email(from, recipients, data, subject);
            forwarder.queueMessage(message);
            out.println("ok");
          }
        } else {
          out.println("error protocol error");
          socket.close();
          break;
        }
      }
    } catch (Exception e) {
      System.err.println("Connection handler error: " + e.getMessage());
    }
  }
}
