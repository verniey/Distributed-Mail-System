package dslab.transfer;

import dslab.mail.Email;
import dslab.util.Config;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.System.err;

public class MessageForwarder implements Runnable {

  private final DomainRegistry domainRegistry;
  private final BlockingQueue<Email> messageQueue = new LinkedBlockingQueue<>();  // Consistent usage
  private final String serverIp;
  private final Config config;

  public MessageForwarder(DomainRegistry domainRegistry, Config config) {
    this.domainRegistry = domainRegistry;
    this.serverIp = getServerIp();
    this.config = config;
  }

  @Override
  public void run() {
    while (true) {
      try {
        Email message = messageQueue.take();  // Handling Message object
        forwardMessage(message);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  public void queueMessage(Email message) {  // Queue Message directly
    messageQueue.offer(message);
  }

  private void forwardMessage(Email email) {
    for (String recipient : email.getTo()) {
      try {
        String recipientDomain = extractDomain(recipient);
        String mailboxServerAddress = domainRegistry.lookup(recipientDomain);

        if (mailboxServerAddress == null) {
          throw new Exception("Domain lookup failed for " + recipientDomain);
        }

        String[] parts = mailboxServerAddress.split(":");

        DMTPClient client = new DMTPClient(parts[0], Integer.parseInt(parts[1]), config);

        if (!client.sendMessage(new Email(email.getFrom(), List.of(recipient), email.getData(), email.getSubject()))) {
          handleDeliveryFailure(email, recipient, "Mailbox server delivery failed.");
        }
      } catch (Exception e) {
        handleDeliveryFailure(email, recipient, e.getMessage());
      }
    }
  }

  private void handleDeliveryFailure(Email originalMessage, String failedRecipient, String errorReason) {
    err.println("Delivery failed to " + failedRecipient + ": " + errorReason);

    String sender = originalMessage.getFrom();
    if (sender == null) {
      err.println("Cannot notify sender. No 'from' address.");
      return;
    }


    Email errorMessage = new Email(
        "mailer@" + serverIp,
        List.of(sender),
        "Delivery Failure",
        "Your message to " + failedRecipient + " could not be delivered. Reason: " + errorReason
    );

    try {
      String senderDomain = extractDomain(sender);
      System.out.println("📡 Extracted sender domain: " + senderDomain);

      String senderMailboxServer = domainRegistry.lookup(senderDomain);
      if (senderMailboxServer == null) {
        throw new Exception("No mailbox server found for sender: " + senderDomain);
      }

      System.out.println("Found sender's mailbox server: " + senderMailboxServer);

      String[] parts = senderMailboxServer.split(":");
      String mailboxHost = parts[0];
      int mailboxPort = Integer.parseInt(parts[1]);

      System.out.println("Connecting to Mailbox Server at " + mailboxHost + ":" + mailboxPort);

      forwardMessage(errorMessage);


    } catch (Exception ex) {
      err.println("Failed to notify sender: " + ex.getMessage());
    }
  }

  private String extractDomain(String email) {
    int atIndex = email.indexOf('@');
    return (atIndex != -1) ? email.substring(atIndex + 1) : "";
  }

  private String getServerIp() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (Exception e) {
      return "unknown";
    }
  }
}
