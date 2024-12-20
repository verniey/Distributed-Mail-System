package dslab.mailbox;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MailboxServer implements IMailboxServer, Runnable {

    private final Config config;
    private final ExecutorService executor;
    private ServerSocket dmtpServerSocket;
    private ServerSocket dmapServerSocket;
    private final Shell shell;
    private final MailboxService mailboxService;
    private final UserService userService;
    private final Set<Socket> activeConnections = Collections.synchronizedSet(new HashSet<>());
    private final UserRepository userRepository;

    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        this.executor = Executors.newFixedThreadPool(10);
        this.mailboxService = new MailboxService();
        this.userRepository = new UserRepository(config);
        this.userService = new UserService(userRepository);
        this.shell = new Shell(in, out);
        this.shell.register(this);
        this.shell.setPrompt(componentId + " > ");
    }

    @Override
    public void run() {
        try {
            int dmtpPort = config.getInt("dmtp.tcp.port");
            int dmapPort = config.getInt("dmap.tcp.port");

            System.out.println("📢 Starting Mailbox Server on DMTP: " + dmtpPort + ", DMAP: " + dmapPort);

            dmtpServerSocket = new ServerSocket(dmtpPort);
            dmapServerSocket = new ServerSocket(dmapPort);

            executor.submit(shell);
            executor.submit(() -> handleConnections(dmtpServerSocket, true));
            executor.submit(() -> handleConnections(dmapServerSocket, false));

        } catch (IOException e) {
            shell.err().println("Server startup failed: " + e.getMessage());
        }
    }

    private void handleConnections(ServerSocket serverSocket, boolean isDMTP) {
        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                activeConnections.add(clientSocket);

                executor.submit(() -> {
                    try {
                        if (isDMTP) {
                            new DMTPHandler(clientSocket, mailboxService, userService).run();
                        } else {
                            new DMAPHandler(clientSocket, userService, mailboxService).run();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        activeConnections.remove(clientSocket);
                        try { clientSocket.close(); } catch (IOException ignored) {}
                    }
                });
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                shell.err().println("Connection handling error: " + e.getMessage());
            }
        }
    }

    @Command
    @Override
    public void shutdown() {
        try {

            if (dmtpServerSocket != null && !dmtpServerSocket.isClosed()) dmtpServerSocket.close();
            if (dmapServerSocket != null && !dmapServerSocket.isClosed()) dmapServerSocket.close();

            synchronized (activeConnections) {
                for (Socket socket : activeConnections) {
                    try { socket.close(); } catch (IOException ignored) {}
                }
                activeConnections.clear();
            }

            executor.shutdownNow();

        } catch (IOException e) {
            shell.err().println("Error during shutdown: " + e.getMessage());
        }
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = new MailboxServer(args[0], new Config(args[0]), System.in, System.out);
        server.run();
    }
}
