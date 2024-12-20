package dslab.transfer;


import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;

import dslab.util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransferServer implements ITransferServer, Runnable {

    private final Config config;
    private final ExecutorService executor;
    private ServerSocket serverSocket;
    private final MessageForwarder forwarder;
    private final Shell shell;
    private volatile boolean running = true;
    private final DomainRegistry domainRegistry;
    private final Set<Socket> activeConnections = Collections.synchronizedSet(new HashSet<>());


    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        this.executor = Executors.newFixedThreadPool(10);
        this.domainRegistry = new DomainRegistry("domains.properties");

        this.forwarder = new MessageForwarder(domainRegistry, config);
        executor.submit(forwarder);

        this.shell = new Shell(in, out);
        this.shell.register(this);
        this.shell.setPrompt(componentId + " > ");
    }

    @Override
    public void run() {
        try {
            int tcpPort = config.getInt("tcp.port");
            serverSocket = new ServerSocket(tcpPort);

            executor.submit(shell);
            executor.submit(this::handleConnections); // Handle incoming client connections

        } catch (Exception e) {
            System.err.println("Server startup error: " + e.getMessage());
        }
    }

    private void handleConnections() {
        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                activeConnections.add(clientSocket);

                executor.submit(() -> {
                    try {
                        new DMTPServer(clientSocket, forwarder).run();
                    } catch (Exception e) {
                        System.err.println("Error handling client: " + e.getMessage());
                    } finally {
                        activeConnections.remove(clientSocket);
                        try { clientSocket.close(); } catch (Exception ignored) {}
                    }
                });
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                System.err.println("Connection handling error: " + e.getMessage());
            }
        }
    }

    @Command
    @Override
    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            synchronized (activeConnections) {
                for (Socket socket : activeConnections) {
                    try { socket.close(); } catch (Exception ignored) {}
                }
                activeConnections.clear();
            }

            executor.shutdownNow();
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }

        throw new StopShellException();
    }



    public static void main(String[] args) throws Exception {
        ITransferServer server = new TransferServer(args[0], new Config(args[0]), System.in, System.out);
        server.run();
    }
}


