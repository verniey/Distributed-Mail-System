package dslab.mailbox;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class DMAPHandler implements Runnable {

  private final Socket socket;
  private final MailboxService mailboxService;
  private final UserService userService;
  private boolean loggedIn = false;
  private String currentUser;

  public DMAPHandler(Socket socket, UserService loginService, MailboxService mailboxService) {
    this.socket = socket;
    this.userService = loginService;
    this.mailboxService = mailboxService;
  }

  @Override
  public void run() {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

      out.println("ok DMAP");

      String line;
      while ((line = in.readLine()) != null) {

        if (line.equalsIgnoreCase("quit")) {
          out.println("ok bye");
          break;
        } else if (line.startsWith("login ")) {
          handleLogin(line, out);
        } else if (line.equalsIgnoreCase("list")) {
          handleList(out);
        } else if (line.startsWith("show ")) {
          handleShow(line, out);
        } else if (line.startsWith("delete ")) {
          handleDelete(line, out);
        } else if (line.startsWith("logout")) {
          handleLogout(out);
        } else {
          out.println("error unknown command");
        }
      }
    } catch (Exception e) {
      System.err.println("DMAP handler error: " + e.getMessage());
    }
  }

  private void handleLogout(PrintWriter out) {
    if (loggedIn) {
      loggedIn = false;
      currentUser = null;
      out.println("ok logout");
    } else {
      out.println("error not logged in");
    }
  }

  private void handleLogin(String line, PrintWriter out) {
    String[] parts = line.split(" ");
    if (parts.length == 3) {
      String email = parts[1].trim();
      String password = parts[2].trim();

      if (userService.authenticate(email, password)) {
        loggedIn = true;
        currentUser = email + "@" + userService.getDomain();

        out.println("ok");
      } else {
        out.println("error invalid credentials");
      }
    } else {
      out.println("error invalid login format");
    }
  }

  private void handleList(PrintWriter out) {
    if (!loggedIn) {
      out.println("error not logged in");
      return;
    }


    List<String> emails = mailboxService.listEmails(currentUser);

    if (emails.isEmpty()) {
      out.println("ok");
      return;
    }

    for (String email : emails) {
      out.println(email);
    }
    out.println("ok");
  }

  private void handleShow(String line, PrintWriter out) {
    if (loggedIn) {
      try {
        int messageId = Integer.parseInt(line.split(" ")[1]);
        String message = mailboxService.getEmail(currentUser, messageId);
        if (message != null) {
          out.println(message);
        } else {
          out.println("error message not found");
        }
      } catch (Exception e) {
        out.println("error invalid message ID");
      }
    } else {
      out.println("error not logged in");
    }
  }

  private void handleDelete(String line, PrintWriter out) {
    if (loggedIn) {
      try {
        int messageId = Integer.parseInt(line.split(" ")[1]);
        if (mailboxService.deleteEmail(currentUser, messageId)) {
          out.println("ok");
        } else {
          out.println("error message not found");
        }
      } catch (Exception e) {
        out.println("error invalid message ID");
      }
    } else {
      out.println("error not logged in");
    }
  }
}


