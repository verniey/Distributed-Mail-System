package dslab.mailbox;

import dslab.mail.Email;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MailboxService implements IMailboxService {
  private final Map<String, List<Email>> mailboxes = new ConcurrentHashMap<>();
  private final AtomicLong messageIdCounter = new AtomicLong(1);

  @Override
  public void storeEmail(String to, String from, String data, String subject) {
    List<String> recipients = Arrays.asList(to.split(",")); // Support multiple recipients

    for (String recipient : recipients) {
      Email email = new Email(messageIdCounter.getAndIncrement(), from, List.of(recipient), data, subject);
      mailboxes.computeIfAbsent(recipient, k -> new ArrayList<>()).add(email);


    }
  }

  @Override
  public List<String> listEmails(String username) {
    List<Email> emails = mailboxes.getOrDefault(username, Collections.emptyList());

    if (emails.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> emailList = new ArrayList<>();
    for (Email email : emails) {
      //System.out.println("    - Found email ID: " + email.getId() + ", Subject: " + email.getSubject());
      emailList.add(email.getId() + " " + email.getFrom() + " " + email.getSubject());
    }

    return emailList;
  }


  @Override
  public String getEmail(String username, int messageId) {
    List<Email> emails = mailboxes.getOrDefault(username, Collections.emptyList());
    if (emails == null) {
      return "error email not found";
    }

    for (Email email : emails) {
      if (email.getId().equals((long) messageId)) {
        return String.format(
            "from %s\nsubject %s\ndata %s",
            email.getFrom(), email.getSubject(), email.getData()
        );
      }
    }
    return "error email not found";
  }

  @Override
  public boolean deleteEmail(String username, int messageId) {
    List<Email> emails = mailboxes.getOrDefault(username, Collections.emptyList());
    if (emails == null) {
      return false;
    }

    return emails.removeIf(email -> email.getId().equals((long) messageId));
  }



}
