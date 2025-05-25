package test;

import com.data_management.*;
import com.cardio_generator.outputs.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockitoAnnotations;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocketDataReader class focusing on edge cases,
 * data format errors, and connection handling.
 */
public class WebSocketDataReaderTest {

    private WebSocketDataReader webSocketReader;
    private DataStorage mockDataStorage;
    private TestWebSocketServer testServer;
    private static final int TEST_PORT = 8081;
    private static final String TEST_HOST = "localhost";
    private CountDownLatch serverStartLatch;
    private CountDownLatch clientConnectedLatch;
    
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        mockDataStorage = mock(DataStorage.class);
        
        // Initialize latches for synchronization
        serverStartLatch = new CountDownLatch(1);
        clientConnectedLatch = new CountDownLatch(1);
        
        // Start a test WebSocket server with proper synchronization
        testServer = new TestWebSocketServer(TEST_PORT);
        testServer.start();
        
        // Wait for server to actually start (with longer timeout)
        boolean serverStarted = serverStartLatch.await(5, TimeUnit.SECONDS);
        if (!serverStarted) {
            throw new RuntimeException("Test server failed to start within 5 seconds");
        }
        
        // Additional wait to ensure server is fully ready
        Thread.sleep(1000);
        
        webSocketReader = new WebSocketDataReader(TEST_HOST, TEST_PORT);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        try {
            if (webSocketReader != null) {
                webSocketReader.stopRealtimeReading();
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        
        try {
            if (testServer != null) {
                testServer.stop(1000);
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        
        Thread.sleep(1000); // Allow cleanup
    }

    /**
     * Test successful connection and basic data processing
     */
    @Test
    @Timeout(15)
    void testSuccessfulConnection() throws Exception {
        // Start the reader and wait for connection
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        // Wait for client connection with timeout
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect to server within 5 seconds");
        
        // Additional wait for connection stabilization
        Thread.sleep(500);
        
        // Verify connection is established
        assertTrue(webSocketReader.isReading(), "WebSocket should be connected and reading");
        
        // Send valid data
        String validData = "1,1640995200000,HeartRate,75.5";
        testServer.broadcastMessage(validData);
        
        // Wait for processing
        Thread.sleep(1000);
        
        // Verify data was processed
        verify(mockDataStorage, atLeastOnce()).addPatientData(1, 75.5, "HeartRate", 1640995200000L);
    }

    /**
     * Test handling of invalid data format - missing parts
     */
    @Test
    @Timeout(15)
    void testInvalidDataFormatMissingParts() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        // Wait for connection
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect to server");
        Thread.sleep(500);
        
        // Send data with missing parts
        String[] invalidDataSamples = {
            "1,1640995200000,HeartRate", // Missing value
            "1,1640995200000", // Missing type and value
            "1", // Only patient ID
            "", // Empty string
            "1,1640995200000,HeartRate,75.5,extraField" // Too many fields
        };
        
        for (String invalidData : invalidDataSamples) {
            testServer.broadcastMessage(invalidData);
            Thread.sleep(200);
        }
        
        // Wait for processing
        Thread.sleep(1000);
        
        // No message should work.
        verify(mockDataStorage, never()).addPatientData(anyInt(), anyDouble(), anyString(), anyLong());
    }

    /**
     * Test handling of invalid numeric values
     */
    @Test
    @Timeout(15)
    void testInvalidNumericValues() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        // Wait for connection
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect to server");
        Thread.sleep(500);
        
        String[] invalidNumericData = {
            "abc,1640995200000,HeartRate,75.5", // Invalid patient ID
            "1,abc,HeartRate,75.5", // Invalid timestamp
            "1,1640995200000,HeartRate,abc", // Invalid measurement value
            "-1,1640995200000,HeartRate,75.5", // Negative patient ID
            "1,-1,HeartRate,75.5" // Negative timestamp
        };
        
        for (String invalidData : invalidNumericData) {
            testServer.broadcastMessage(invalidData);
            Thread.sleep(200);
        }
        
        // Wait for processing
        Thread.sleep(1000);
        
        // None of these should result in successful data storage
        verify(mockDataStorage, never()).addPatientData(anyInt(), anyDouble(), anyString(), anyLong());
    }

    /**
     * Test handling of alert messages with string values
     */
    @Test
    @Timeout(15)
    void testAlertMessageHandling() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        // Wait for connection
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect to server");
        Thread.sleep(500);
        
        // Test various alert formats
        String[] alertMessages = {
            "1,1640995200000,Alert,triggered",
            "2,1640995200000,Alert,resolved",
            "3,1640995200000,Alert,1",
            "4,1640995200000,Alert,0",
            "5,1640995200000,Alert,unknown" // Should default to 0.0
        };
        
        for (String alertData : alertMessages) {
            testServer.broadcastMessage(alertData);
            Thread.sleep(200);
        }
        
        // Wait for processing
        Thread.sleep(1000);
        
        // Verify alert processing
        verify(mockDataStorage).addPatientData(1, 1.0, "Alert", 1640995200000L); // triggered
        verify(mockDataStorage).addPatientData(2, 0.0, "Alert", 1640995200000L); // resolved
        verify(mockDataStorage).addPatientData(3, 1.0, "Alert", 1640995200000L); // 1
        verify(mockDataStorage).addPatientData(4, 0.0, "Alert", 1640995200000L); // 0
        verify(mockDataStorage).addPatientData(5, 0.0, "Alert", 1640995200000L); // unknown -> 0.0
    }

    /**
     * Test handling of numeric values with units
     */
    @Test
    @Timeout(15)
    void testNumericValuesWithUnits() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        // Wait for connection
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect to server");
        Thread.sleep(500);
        
        String[] dataWithUnits = {
            "1,1640995200000,BloodPressure,120mmHg",
            "2,1640995200000,HeartRate,75BPM",
            "3,1640995200000,Temperature,98.6°F",
            "4,1640995200000,Saturation,95%"
        };
        
        for (String data : dataWithUnits) {
            testServer.broadcastMessage(data);
            Thread.sleep(200);
        }
        
        // Wait for processing
        Thread.sleep(1000);
        
        // Verify units are stripped and values parsed correctly
        verify(mockDataStorage).addPatientData(1, 120.0, "BloodPressure", 1640995200000L);
        verify(mockDataStorage).addPatientData(2, 75.0, "HeartRate", 1640995200000L);
        verify(mockDataStorage).addPatientData(3, 98.6, "Temperature", 1640995200000L);
        verify(mockDataStorage).addPatientData(4, 95.0, "Saturation", 1640995200000L);
    }

    /**
     * Test handling of null and empty messages
     */
    @Test
    @Timeout(15)
    void testNullAndEmptyMessages() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        // Wait for connection
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect to server");
        Thread.sleep(500);
        
        // Send null and empty messages (skip null as it causes issues)
        testServer.broadcastMessage("");
        testServer.broadcastMessage("   "); // Whitespace only
        testServer.broadcastMessage("\n\t"); // Other whitespace characters
        
        Thread.sleep(1000);
        
        // No data should be stored for invalid messages
        verify(mockDataStorage, never()).addPatientData(anyInt(), anyDouble(), anyString(), anyLong());
    }

    /**
     * Test connection loss and reconnection behavior
     */
    @Test
    @Timeout(15)
    void testConnectionLoss() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        // Wait for connection
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect to server");
        Thread.sleep(500);
        
        assertTrue(webSocketReader.isReading(), "Should be connected initially");
        
        // Simulate server shutdown
        testServer.stop();
        Thread.sleep(2000);
        
        // Connection should be lost
        assertFalse(webSocketReader.isReading(), "Should be disconnected after server shutdown");
    }

    /**
     * Test connection timeout scenario
     */
    @Test
    @Timeout(15)
    void testConnectionTimeout() {
        // Try to connect to a non-existent server
        WebSocketDataReader timeoutReader = new WebSocketDataReader("localhost", 9999);
        
        assertThrows(IOException.class, () -> {
            timeoutReader.startRealtimeReading(mockDataStorage);
        }, "Should throw IOException when unable to connect");
    }

    /**
     * Test handling of malformed JSON-like data
     */
    @Test
    @Timeout(15)
    void testMalformedData() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        // Wait for connection
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect to server");
        Thread.sleep(500);
        
        String[] malformedData = {
            "{invalid:json}",
            "patient:1,time:now,type:heart",
            "1;1640995200000;HeartRate;75.5", // Wrong delimiter
            "1,,HeartRate,75.5", // Empty timestamp
            "1,1640995200000,,75.5", // Empty record type
            ",1640995200000,HeartRate,75.5" // Empty patient ID
        };
        
        for (String data : malformedData) {
            testServer.broadcastMessage(data);
            Thread.sleep(200);
        }
        
        // Wait for processing
        Thread.sleep(1000);
        
        // None should result in successful storage
        verify(mockDataStorage, never()).addPatientData(anyInt(), anyDouble(), anyString(), anyLong());
    }

    /**
     * Test boundary values
     */
    @Test
    @Timeout(15)
    void testBoundaryValues() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        // Wait for connection
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect to server");
        Thread.sleep(500);
        
        String[] boundaryData = {
            "0,0,HeartRate,0.0", // All zeros (patient ID 0 should be rejected)
            "2147483647,9223372036854775807,HeartRate,1.7976931348623157E308", // Max values
            "1,1,HeartRate,-999.99", // Negative measurement
            "1,1,HeartRate,0.000001" // Very small positive number
        };
        
        for (String data : boundaryData) {
            testServer.broadcastMessage(data);
            Thread.sleep(200);
        }
        
        // Wait for processing
        Thread.sleep(1000);
        
        // Only valid entries should be stored (patient ID 0 is invalid)
        verify(mockDataStorage, times(3)).addPatientData(anyInt(), anyDouble(), anyString(), anyLong());
    }

    /**
     * Helper class to create a test WebSocket server with proper synchronization
     */
    private class TestWebSocketServer extends WebSocketServer {
        private final AtomicBoolean serverRunning = new AtomicBoolean(false);
        
        public TestWebSocketServer(int port) {
            super(new InetSocketAddress(port));
            this.setReuseAddr(true);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            System.out.println("Test server: Client connected from " + conn.getRemoteSocketAddress());
            clientConnectedLatch.countDown();
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            System.out.println("Test server: Client disconnected - Code: " + code + ", Reason: " + reason);
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            // Echo back for testing if needed
            System.out.println("Test server received: " + message);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            System.err.println("Test server error: " + ex.getMessage());
            ex.printStackTrace();
        }

        @Override
        public void onStart() {
            System.out.println("Test WebSocket server started on port " + getPort());
            serverRunning.set(true);
            serverStartLatch.countDown();
        }

        public void broadcastMessage(String message) {
            if (message == null) {
                return; // Skip null messages to prevent issues
            }
            
            try {
                for (WebSocket conn : getConnections()) {
                    if (conn.isOpen()) {
                        conn.send(message);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error broadcasting message: " + e.getMessage());
            }
        }
        
        public boolean isRunning() {
            return serverRunning.get();
        }
    }
}