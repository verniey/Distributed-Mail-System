package dslab.dmtp;

import dslab.mail.Email;

import java.util.ArrayList;
import java.util.List;

public class DMTPProtocol {
  private String from;
  private List<String> recipients;
  private String subject;
  private String data;
  private boolean started;
  private Email email;

  public DMTPProtocol() {
    this.recipients = new ArrayList<>();
    this.started = false;
    this.email = null;
  }

  public String processCommand(String command) {
    String[] parts = command.split(" ", 2);
    String action = parts[0].toLowerCase();

    switch (action) {
      case "begin":
        return handleBegin();
      case "from":
        if (started) {
          return handleFrom(parts[1]);
        }
      case "to":
        if (started) {
          return handleTo(parts[1]);
        }
      case "subject":
        if (started) {
          return handleSubject(parts[1]);
        }
      case "data":
        if (started) {
          return handleData(parts[1]);
        }
      case "send":
        if (started) {
          return handleSend();
        }
      case "quit":
        if (started) return handleQuit();
      default:
        return "error protocol error";
    }
  }

  private String handleQuit() {
    return "ok bye";
  }

  private String handleBegin() {
    this.email = new Email();
    this.started = true;
    this.from = null;
    this.recipients.clear();
    this.subject = null;
    this.data = null;
    return "ok";
  }

  private String handleFrom(String sender) {
    this.from = sender;
    return "ok";
  }

  private String handleTo(String recipientsLine) {
    this.recipients.clear();
    String[] recipientArray = recipientsLine.split("\\s*,\\s*");
    for (String recipient : recipientArray) {
      this.recipients.add(recipient);
    }
    return "ok " + this.recipients.size();
  }

  private String handleSubject(String subject) {
    this.subject = subject;
    return "ok";
  }

  private String handleData(String data) {
    this.data = data;
    return "ok";
  }

  private String handleSend() {
    this.email = new Email(from, recipients, subject, data);
    return "ok";
  }

  public Email getEmail() {
    return new Email(from, recipients, subject, data);
  }
}
