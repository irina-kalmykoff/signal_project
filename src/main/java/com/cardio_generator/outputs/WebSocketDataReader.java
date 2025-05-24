package com.cardio_generator.outputs;

import com.data_management.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A WebSocket client that connects to a server and processes health data in real-time.
 * This class implements the ContinuousDataReader interface to receive and store data from
 * the WebSocket server continuously.
 */
public class WebSocketDataReader implements ContinuousDataReader {
    private final String serverUri;
    private PatientDataWebSocketClient client;
    private boolean isRunning = false;
    private CountDownLatch connectLatch;

    /**
     * Creates a new WebSocketDataReader that connects to the specified WebSocket server.
     *
     * @param hostname the server hostname
     * @param port the server port
     */
    public WebSocketDataReader(String hostname, int port) {
        this.serverUri = "ws://" + hostname + ":" + port;
        this.connectLatch = new CountDownLatch(1);
    }

    @Override
    public void readData(DataStorage dataStorage) throws IOException {
        try {
            URI uri = new URI(serverUri);
            client = new PatientDataWebSocketClient(uri, dataStorage);

            // Connect to the WebSocket server
            client.connect();
            
            // Wait for the connection to be established
            boolean connected = connectLatch.await(10, TimeUnit.SECONDS);
            if (!connected) {
                throw new IOException("Failed to connect to WebSocket server at " + serverUri);
            }
            
            isRunning = true;
            System.out.println("Successfully connected to WebSocket server at " + serverUri);
            
            // Keep the client connected until explicitly stopped
            // The connection will be maintained by the WebSocket client
            
        } catch (URISyntaxException e) {
            throw new IOException("Invalid WebSocket URI: " + serverUri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Connection attempt was interrupted", e);
        }
    }

    /**
     * Stops the WebSocket client connection.
     */
    public void stopRealtimeReading() {
        if (client != null && isRunning) {
            client.close();
            isRunning = false;
            System.out.println("WebSocket connection closed");
        }
    }
    
    /**
     * Checks if the WebSocket client is currently connected and receiving data.
     * 
     * @return true if the client is connected and receiving data, false otherwise
     */
     public boolean isReading() {
        return isRunning && client != null && client.isOpen();
    }
    
    /**
     * Starts the continuous data reading process.
     * 
     * @param dataStorage the storage where data will be stored
     * @throws IOException if there is an error connecting to the WebSocket server
     */
    public void startRealtimeReading(DataStorage dataStorage) throws IOException {
        readData(dataStorage);
    }
    
    /**
     * Stops the WebSocket client connection.
     * @deprecated Use {@link #stopReading()} instead.
     */
    @Deprecated
    public void stop() {
        stopRealtimeReading();
    }

    /**
     * The PatientDataWebSocketClient handles the WebSocket connection and processes incoming messages.
     */
    private class PatientDataWebSocketClient extends WebSocketClient {
        private final DataStorage dataStorage;

        /**
         * Creates a new PatientDataWebSocketClient.
         *
         * @param serverUri the URI of the WebSocket server
         * @param dataStorage the data storage to add records to
         */
        public PatientDataWebSocketClient(URI serverUri, DataStorage dataStorage) {
            super(serverUri);
            this.dataStorage = dataStorage;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            System.out.println("Connected to WebSocket server");
            connectLatch.countDown();
        }

        @Override
        public void onMessage(String message) {
            try {
                // Process the received message
                // Expected format: patientId,timestamp,recordType,value
                if (message == null || message.trim().isEmpty()) {
                    System.err.println("Received empty or null message");
                    return;
                }
                
                String[] parts = message.trim().split(",");
                
                if (parts.length < 4) {
                    System.err.println("Invalid data format in message (expected 4 parts, got " + 
                                     parts.length + "): " + message);
                    return;
                }
                
                // Parse message components
                int patientId = Integer.parseInt(parts[0].trim());
                long timestamp = Long.parseLong(parts[1].trim());
                String recordType = parts[2].trim();
                String valueStr = parts[3].trim();

                // Validate parsed data
                if (patientId < 0) {
                    System.err.println("Invalid patient ID (negative): " + patientId);
                    return;
                }
                
                if (timestamp < 0) {
                    System.err.println("Invalid timestamp (negative): " + timestamp);
                    return;
                }
                
                if (recordType.isEmpty()) {
                    System.err.println("Empty record type in message: " + message);
                    return;
                }

                // Handle different data types
                if ("Alert".equals(recordType)) {
                    handleAlertMessage(patientId, timestamp, recordType, valueStr);
                } else {
                    handleNumericMessage(patientId, timestamp, recordType, valueStr);
                }
                
            } catch (NumberFormatException e) {
                System.err.println("Error parsing numeric values in message: " + message + " - " + e.getMessage());
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Array index error processing message: " + message + " - " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error processing message: " + message + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        /**
         * Handles alert messages with special parsing for string values.
         */
        private void handleAlertMessage(int patientId, long timestamp, String recordType, String valueStr) {
            try {
                // First try to parse as a numeric value
                double alertValue = Double.parseDouble(valueStr);
                dataStorage.addPatientData(patientId, alertValue, recordType, timestamp);
            } catch (NumberFormatException e) {
                // Convert string alerts to numeric values
                double alertValue;
                if (valueStr.equalsIgnoreCase("triggered") || valueStr.equals("1")) {
                    alertValue = 1.0;
                } else if (valueStr.equalsIgnoreCase("resolved") || valueStr.equals("0")) {
                    alertValue = 0.0;
                } else {
                    System.err.println("Unknown alert value: " + valueStr + " - defaulting to 0.0");
                    alertValue = 0.0;
                }
                dataStorage.addPatientData(patientId, alertValue, recordType, timestamp);
            }
        }
        
        /**
         * Handles numeric health data messages.
         */
        private void handleNumericMessage(int patientId, long timestamp, String recordType, String valueStr) {
            try {
                // Clean the value string to handle percentages and other formats
                String cleanValue = cleanNumericValue(valueStr);
                double value = Double.parseDouble(cleanValue);
                dataStorage.addPatientData(patientId, value, recordType, timestamp);
                
                // Optional: Debug output to see what's happening
                System.out.println(String.format("Parsed %s for patient %d: %s -> %.2f", 
                                            recordType, patientId, valueStr, value));
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse " + recordType + " value: '" + valueStr + "' - " + e.getMessage());
            }
        }

        /**
         * Cleans numeric values by removing common non-numeric suffixes
         */
        private String cleanNumericValue(String valueStr) {
            if (valueStr == null || valueStr.trim().isEmpty()) {
                return "0";
            }
            
            // Remove common suffixes and trim whitespace
            String cleaned = valueStr.trim()
                                    .replace("%", "")      // Remove percentage
                                    .replace("mmHg", "")   // Remove blood pressure unit
                                    .replace("BPM", "")    // Remove heart rate unit
                                    .replace("bpm", "")    // Remove heart rate unit (lowercase)
                                    .replace("°C", "")     // Remove temperature unit
                                    .replace("°F", "");    // Remove temperature unit
            
            // Handle empty string after cleaning
            return cleaned.trim().isEmpty() ? "0" : cleaned.trim();
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("WebSocket connection closed by " + (remote ? "server" : "client") + 
                             " - Code: " + code + ", Reason: " + reason);
            isRunning = false;
            
            // Reset the latch for potential reconnection
            connectLatch = new CountDownLatch(1);
        }

        @Override
        public void onError(Exception ex) {
            System.err.println("WebSocket error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
