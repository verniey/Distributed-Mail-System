package dslab.monitoring;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.util.Config;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MonitoringServer implements IMonitoringServer {

    private final Config config;
    private final Shell shell;
    private DatagramSocket udpSocket;
    private final ExecutorService executor;
    private final Map<String, Integer> serverStats = new ConcurrentHashMap<>();
    private final Map<String, Integer> senderStats = new ConcurrentHashMap<>();
    private volatile boolean running = true;

    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        this.shell = new Shell(in, out);
        this.shell.register(this);
        this.shell.setPrompt(componentId + " > ");
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        try {
            int udpPort = config.getInt("udp.port");
            udpSocket = new DatagramSocket(udpPort);
            executor.submit(shell); // Start CLI shell

            shell.out().println("Monitoring server running on UDP port " + udpPort);

            byte[] buffer = new byte[1024];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);  // Receive UDP statistics packet
                String data = new String(packet.getData(), 0, packet.getLength());
                processPacket(data);
            }
        } catch (SocketException e) {
            if (running) {
                shell.err().println("Socket error: " + e.getMessage());
            }
        } catch (Exception e) {
            shell.err().println("Error: " + e.getMessage());
        }
    }

    /**
     * Processes the received UDP packet, extracts statistics, and updates counters.
     */
    private void processPacket(String data) {
        try {
            String[] parts = data.split(" ");
            if (parts.length == 2) {
                String server = parts[0];  // Format: <server-ip>:<port>
                String sender = parts[1];  // Format: <sender-email>

                // Update statistics
                serverStats.merge(server, 1, Integer::sum);  // Count messages per server
                senderStats.merge(sender, 1, Integer::sum);  // Count messages per sender

            } else {
                shell.err().println("Invalid packet format: " + data);
            }
        } catch (Exception e) {
            shell.err().println("Failed to process packet: " + data);
        }
    }

    /**
     * Prints the number of messages sent per sender email.
     */
    @Command
    @Override
    public void addresses() {
        if (senderStats.isEmpty()) {
            shell.out().println("No data available for addresses.");
        } else {
            senderStats.forEach((address, count) -> shell.out().println(address + " " + count));
        }
    }

    /**
     * Prints the number of messages sent via each transfer server.
     */
    @Command
    @Override
    public void servers() {
        if (serverStats.isEmpty()) {
            shell.out().println("No data available for servers.");
        } else {
            serverStats.forEach((server, count) -> shell.out().println(server + " " + count));
        }
    }

    /**
     * Gracefully shuts down the monitoring server.
     */
    @Command
    @Override
    public void shutdown() {
        running = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        executor.shutdownNow();
        shell.out().println("Monitoring server shutdown complete.");
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = new MonitoringServer(args[0], new Config(args[0]), System.in, System.out);
        server.run();
    }
}
