package dslab.mailbox;

import dslab.dmtp.DMTPProtocol;
import dslab.mail.Email;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.err;

public class DMTPHandler implements Runnable {

  private final Socket socket;
  private final MailboxService mailboxService;
  private final DMTPProtocol dmtpProtocol;
  private final UserService userService;
  private BufferedReader in;
  private PrintWriter out;

  public DMTPHandler(Socket socket, MailboxService mailboxService, UserService userService) {
    this.socket = socket;
    this.mailboxService = mailboxService;
    this.dmtpProtocol = new DMTPProtocol();
    this.userService = userService;

  }

  @Override
  public void run() {
    try {
      this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(socket.getOutputStream(), true);
      out.println("ok DMTP");

      String line;
      while ((line = in.readLine()) != null) {

        String response = dmtpProtocol.processCommand(line);

        if (line.startsWith("to ") && !response.contains("error")) {
          response = validateRecipients();
        }

        if (response.equals("ok") && line.equalsIgnoreCase("send")) {
          storeEmail();
        }

        if (response.equals("error protocol error")) {
          out.println(response);
          socket.close();
          break;
        }

        if (response.equals("ok bye")) {
          out.println(response);
          socket.close();
          break;
        }
        out.println(response);

      }

    } catch (Exception e) {
      err.println("DMTP handler error: " + e.getMessage());
    }
  }

  private String validateRecipients() {
    Email email = dmtpProtocol.getEmail();
    List<String> validRecipients = new ArrayList<>();
    List<String> invalidRecipients = new ArrayList<>();


    for (String recipient : email.getTo()) {

      String[] parts = recipient.split("@");

      if (parts.length != 2 || !parts[1].equals(userService.getDomain())) {
        invalidRecipients.add(parts[0]);
      } else if (!userService.userExists(parts[0])) {
        invalidRecipients.add(parts[0]);
      } else {
        validRecipients.add(recipient);
      }
    }

    if (!invalidRecipients.isEmpty()) {
      return "error unknown recipient " + String.join(", ", invalidRecipients);
    }

    return "ok " + validRecipients.size();
  }

  private boolean validateEmail(Email email) {
    boolean valid = true;

    if (email.getFrom() == null) {
      valid = false;
      out.println("error no sender");
    } else if (email.getTo().isEmpty()) {
      valid = false;
      out.println("error no recipient");
    } else if (email.getSubject() == null) {
      valid = false;
      out.println("error no subject");
    } else if (email.getData() == null) {
      valid = false;
      out.println("error no data");
    }

    return valid;
  }

  private void storeEmail() {
    Email email = dmtpProtocol.getEmail();
    if (!validateEmail(email)) {
      return;
    }
    boolean hasValidRecipient = false;

    for (String recipient : email.getTo()) {
      if (userService.userExists(recipient.split("@")[0])) {
        mailboxService.storeEmail(recipient, email.getFrom(), email.getSubject(), email.getData());
        hasValidRecipient = true;
      }
    }

    if (!hasValidRecipient) {
      out.println("error no valid recipient");
    }
  }
}



