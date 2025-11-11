package main.network;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class NotificationServer {
    private static DatagramSocket socket;
    private boolean running;
    private static InetAddress broadcastAddress;

    static {
        try {
            broadcastAddress = InetAddress.getByName("255.255.255.255");
            socket = new DatagramSocket();
            socket.setBroadcast(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void notify(String text) {
        try {
            byte[] buffer = text.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, 8888);
            socket.send(packet);
            System.out.println("[UDP] Sent notification: " + text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public NotificationServer() {
        try {
            broadcastAddress = InetAddress.getByName("255.255.255.255");
            socket = new DatagramSocket();
            socket.setBroadcast(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        running = true;
        System.out.println("[UDP] Notification Server Started");
    }

    public void broadcast(String msg) {
        if (!running) return;

        try {
            byte[] buffer = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, broadcastAddress, 8888
            );
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        socket.close();
    }
}
