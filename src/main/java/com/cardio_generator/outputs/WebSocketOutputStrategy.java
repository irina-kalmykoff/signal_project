package com.cardio_generator.outputs;

import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class WebSocketOutputStrategy implements OutputStrategy {

    private WebSocketServer server;

    public WebSocketOutputStrategy(int port) {
        server = new SimpleWebSocketServer(new InetSocketAddress(port));
        System.out.println("WebSocket server created on port: " + port + ", listening for connections...");
        server.start();
    }

    @Override
    public void output(int patientId, long timestamp, String label, String data) {
        // Enhanced data validation to ensure all necessary patient information is present
        if (patientId < 0) {
            System.err.println("Invalid patient ID (negative value): " + patientId + " - skipping send");
            return;
        }
        
        if (timestamp < 0) {
            System.err.println("Invalid timestamp (negative value): " + timestamp + " - skipping send");
            return;
        }
        
        if (label == null || label.trim().isEmpty()) {
            System.err.println("Invalid label (null or empty): " + label + " - skipping send");
            return;
        }
        
        if (data == null) {
            System.err.println("Invalid data (null): " + data + " - skipping send");
            return;
        }
        
        // Format the data correctly for WebSocket transmission
        // Format: patientId,timestamp,recordType,value
        String message = String.format("%d,%d,%s,%s", patientId, timestamp, label, data);
        
        // Broadcast the message to all connected clients
        int clientCount = 0;
        for (WebSocket conn : server.getConnections()) {
            if (conn.isOpen()) {
                try {
                    conn.send(message);
                    clientCount++;
                } catch (Exception e) {
                    System.err.println("Error sending message to client: " + e.getMessage());
                }
            }
        }
        
        if (clientCount == 0) {
            System.out.println("No connected clients to receive message: " + message);
        }
    }

    private static class SimpleWebSocketServer extends WebSocketServer {

        public SimpleWebSocketServer(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket conn, org.java_websocket.handshake.ClientHandshake handshake) {
            System.out.println("New connection: " + conn.getRemoteSocketAddress());
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            System.out.println("Closed connection: " + conn.getRemoteSocketAddress());
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            // Not used in this context
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            ex.printStackTrace();
        }

        @Override
        public void onStart() {
            System.out.println("Server started successfully");
        }
    }
}
