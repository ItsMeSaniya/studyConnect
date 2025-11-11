package main.network;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class NotificationClient {
    private DatagramSocket socket;
    private boolean running;
    private NotificationListener listener;
    private String currentUsername;
    private static final int PORT = 8888;

    public interface NotificationListener {
        void onNotificationReceived(String message);
    }

    public NotificationClient(NotificationListener listener, String currentUsername) {
        this.listener = listener;
        this.currentUsername = currentUsername;
    }

    public void start() {
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(PORT)); // 8888
            socket.setBroadcast(true);

            socket.setSoTimeout(1000); // Timeout to check running status periodically

            running = true;

            Thread thread = new Thread(() -> {
                while (running) {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);

                        String msg = new String(packet.getData(), 0,
                                packet.getLength(), StandardCharsets.UTF_8).trim();

                        System.out.println("[UDP] Received broadcast notification from " +
                                packet.getAddress() + ": " + msg);

                        if (listener != null && !msg.isEmpty()) {
                            // This triggers the pop-up logic in MainDashboard
                            String senderName = currentUsername;

                            // Extract sender from message format: "Message from <sender>: <text>"
                            String lower = msg.toLowerCase();
                            if (lower.startsWith("message from ") && msg.contains(":")) {
                                String sender = msg.substring(13, msg.indexOf(":")).trim();
                                if (sender.equals(senderName)) {
                                    System.out.println("[DEBUG] Ignored own broadcast message: " + msg);
                                    continue;
                                }
                            }

                            listener.onNotificationReceived(msg);

                        }
                    } catch (SocketTimeoutException e) {
                        continue;
                    } catch (Exception e) {
                        if (running) {
                            System.err.println("[UDP] Notification client error: " + e.getMessage());
                        }
                        break;
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();

            System.out.println("[UDP] Broadcast Notification Client Started and listening on port " + PORT);

        } catch (Exception e) {
            System.err.println("[UDP] Failed to start notification client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}