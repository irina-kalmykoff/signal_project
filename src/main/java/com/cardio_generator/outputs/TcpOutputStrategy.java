package com.cardio_generator.outputs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

/**
 * Implements {@link OutputStrategy} to send patient data over TCP connections.
 * <p>
 * This strategy starts a TCP server on a specified port and sends health data
 * to the first connected client in a simple CSV-like format:
 * {@code patientId,timestamp,label,data}
 * </p>
 */
public class TcpOutputStrategy implements OutputStrategy {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;

    /**
     * Creates a TCP server on the specified port and starts listening for client connections.
     * Client connections are accepted asynchronously in a separate thread.
     *
     * @param port The TCP port to listen on (0-65535)
     * @throws IOException If the server socket cannot be opened on the specified port
     */

    public TcpOutputStrategy(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("TCP Server started on port " + port);

            // Accept clients in a new thread to not block the main thread
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    clientSocket = serverSocket.accept();
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    System.out.println("Client connected: " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Formats patient data into a standardized string and writes it to the TCP output stream.
     * <p>
     * The message format is: {@code patientId,timestamp,label,data}
     * </p>
     *
     * @param patientId The ID of the patient (positive integer)
     * @param timestamp The measurement timestamp (milliseconds since epoch)
     * @param label     The type of measurement (e.g., "HeartRate", "BloodPressure")
     * @param data      The measurement value (format depends on label)
     */
    @Override
    public void output(int patientId, long timestamp, String label, String data) {
        if (out != null) {
            String message = String.format("%d,%d,%s,%s", patientId, timestamp, label, data);
            out.println(message);
        }
    }
}
