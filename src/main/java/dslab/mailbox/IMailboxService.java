package dslab.mailbox;

import java.util.List;

public interface IMailboxService {

  /**
   * Deletes an email for a specific user.
   *
   * @param username  The username of the email owner.
   * @param messageId The unique ID of the email to be deleted.
   * @return true if the email was successfully deleted, false otherwise.
   */
  boolean deleteEmail(String username, int messageId);

  /**
   * Retrieves an email's content for a specific user.
   *
   * @param username  The username of the email owner.
   * @param messageId The unique ID of the email to retrieve.
   * @return The content of the email as a String, or null if not found.
   */
  String getEmail(String username, int messageId);

  /**
   * Lists all emails for a specific user.
   *
   * @param username The username of the email owner.
   * @return A list of email summaries (e.g., subject lines or metadata).
   */
  List<String> listEmails(String username);

  /**
   * Stores a new email in the system.
   *
   * @param to      The recipient's username.
   * @param from    The sender's username.
   * @param subject The subject of the email.
   * @param data    The body/content of the email.
   */
  void storeEmail(String to, String from, String subject, String data);
}
