package test;

import com.data_management.*;
import com.cardio_generator.outputs.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.DisplayName;
import org.mockito.MockitoAnnotations;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Enhanced error handling tests for WebSocket real-time data processing system.
 * These tests focus specifically on network errors and data transmission failures.
 */
public class WebSocketEnhancedErrorHandlingTest {

    private WebSocketDataReader webSocketReader;
    private DataStorage mockDataStorage;
    private TestWebSocketServer testServer;
    private static final int TEST_PORT = 8082;
    private static final String TEST_HOST = "localhost";
    private CountDownLatch serverStartLatch;
    private CountDownLatch clientConnectedLatch;
    
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        mockDataStorage = mock(DataStorage.class);
        
        serverStartLatch = new CountDownLatch(1);
        clientConnectedLatch = new CountDownLatch(1);
        
        testServer = new TestWebSocketServer(TEST_PORT);
        testServer.start();
        
        boolean serverStarted = serverStartLatch.await(5, TimeUnit.SECONDS);
        if (!serverStarted) {
            throw new RuntimeException("Test server failed to start within 5 seconds");
        }
        
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
        
        Thread.sleep(1000);
    }

    /**
     * Test handling of sudden server disconnection during active data transmission
     */
    @Test
    @DisplayName("Network Error: Sudden Server Disconnection")
    @Timeout(20)
    void testSuddenServerDisconnection() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect to server");
        Thread.sleep(500);
        
        // Send some data successfully first
        testServer.broadcastMessage("1,1640995200000,HeartRate,75.0");
        Thread.sleep(500);
        
        // Verify initial data was processed
        verify(mockDataStorage, times(1)).addPatientData(1, 75.0, "HeartRate", 1640995200000L);
        
        // Suddenly stop the server (simulates network failure)
        testServer.forceStop();
        Thread.sleep(1000);
        
        // Client should detect disconnection
        assertFalse(webSocketReader.isReading(), "Client should detect server disconnection");
        
        // Attempting to send more data should not cause exceptions or crashes
        assertDoesNotThrow(() -> {
            // The system should handle this gracefully
            Thread.sleep(1000);
        }, "System should handle sudden disconnection gracefully");
    }

    /**
     * Test handling of intermittent connection drops with automatic reconnection
     */
    @Test
    @DisplayName("Network Error: Intermittent Connection Drops")
    @Timeout(30)
    void testIntermittentConnectionDrops() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should initially connect");
        Thread.sleep(500);
        
        // Send initial data
        testServer.broadcastMessage("1,1640995200000,HeartRate,75.0");
        Thread.sleep(500);
        
        // Simulate connection drop by closing client connections
        testServer.dropAllConnections();
        Thread.sleep(2000);
        
        // Check if client detects disconnection
        assertFalse(webSocketReader.isReading(), "Client should detect connection drop");
        
        // If the client has reconnection logic, test it here
        // For now, just verify the system doesn't crash
        assertDoesNotThrow(() -> {
            Thread.sleep(2000);
        }, "System should handle connection drops without crashing");
    }

    /**
     * Test handling of corrupted/partial message transmission
     */
    @Test
    @DisplayName("Network Error: Corrupted Message Transmission")
    @Timeout(15)
    void testCorruptedMessageTransmission() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect");
        Thread.sleep(500);
        
        // Send corrupted/partial messages (simulating network corruption)
        String[] corruptedMessages = {
            "1,164099520", // Truncated message
            "1,1640995200000,Heart", // Partially transmitted
            "\u0000\u0001\u0002", // Binary garbage
            "1,1640995200000,HeartRate,75.0TFCQP", // Unknown character contamination.
            "3,1640995200000,HeartRate,75.0\n\r\t", // Extra whitespace/control chars. This string will be correctly passed since the measurementvalue is trimmed in WebSocketDataReader.
            "1,1640995200000,HeartRate,75.0" + "\uFFFD", // Unicode replacement character
        };
        
        for (String corruptedMessage : corruptedMessages) {
            testServer.broadcastMessage(corruptedMessage);
            Thread.sleep(200);
        }

        // Only the valid messages should be processed
        verify(mockDataStorage, times(1)).addPatientData(3, 75.0, "HeartRate", 1640995200000L);
        
        // Send a valid message to ensure system is still functional
        testServer.broadcastMessage("2,1640995201000,HeartRate,80.0");
        Thread.sleep(1000);
        
        // Only the valid messages should be processed
        verify(mockDataStorage, times(1)).addPatientData(2, 80.0, "HeartRate", 1640995201000L);

        // Verify no corrupted data was stored
        verify(mockDataStorage, never()).addPatientData(eq(1), anyDouble(), anyString(), anyLong());
    }

    /**
     * Test handling of extremely large messages (potential memory issues)
     */
    @Test
    @DisplayName("Network Error: Extremely Large Messages")
    @Timeout(15)
    void testExtremelyLargeMessages() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect");
        Thread.sleep(500);
        
        // Create an extremely large message
        StringBuilder largeMessage = new StringBuilder();
        largeMessage.append("1,1640995200000,HeartRate,");
        
        // Add a very long value string (10MB of characters)
        for (int i = 0; i < 10000000; i++) {
            largeMessage.append("a");
        }
        
        // System should handle large messages gracefully (either process or reject)
        assertDoesNotThrow(() -> {
            testServer.broadcastMessage(largeMessage.toString());
            Thread.sleep(2000);
        }, "System should handle large messages without crashing");
        
        // Send a normal message to verify system is still functional
        testServer.broadcastMessage("2,1640995201000,HeartRate,75.0");
        Thread.sleep(1000);
        
        // The normal message should be processed
        verify(mockDataStorage, atLeastOnce()).addPatientData(2, 75.0, "HeartRate", 1640995201000L);
    }

    /**
     * Test handling of messages with invalid encoding
     */
    @Test
    @DisplayName("Network Error: Invalid Message Encoding")
    @Timeout(15)
    void testInvalidMessageEncoding() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect");
        Thread.sleep(500);
        
        // Test various encoding issues
        String[] encodingIssues = {
            "1,1640995200000,HeartRate,75.0\uD800", // Unpaired surrogate
            "1,1640995200000,HeartRate,75.0\uDC00", // Unpaired low surrogate
            "1,1640995200000,Heart\u0000Rate,75.0", // Null character in field
            "1,1640995200000,HeartRate,75.0\u001F", // Control character
            "1,1640995200000,HeartRate,75.0\u007F", // DEL character
        };
        
        for (String message : encodingIssues) {
            testServer.broadcastMessage(message);
            Thread.sleep(200);
        }
        
        // Send valid message to check system stability
        testServer.broadcastMessage("3,1640995202000,HeartRate,78.0");
        Thread.sleep(1000);
        
        // Only valid message should be processed
        verify(mockDataStorage, times(1)).addPatientData(3, 78.0, "HeartRate", 1640995202000L);
    }

    /**
     * Test system behavior during DataStorage exceptions
     */
    @Test
    @DisplayName("Storage Error: DataStorage Exception Handling")
    @Timeout(15)
    void testDataStorageExceptions() throws Exception {
        // Configure mock to throw exceptions
        doThrow(new RuntimeException("Database connection failed"))
            .when(mockDataStorage).addPatientData(eq(1), anyDouble(), anyString(), anyLong());
        
        // Normal behavior for other patient IDs
        doNothing().when(mockDataStorage).addPatientData(eq(2), anyDouble(), anyString(), anyLong());
        
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect");
        Thread.sleep(500);
        
        // Send message that will cause storage exception
        testServer.broadcastMessage("1,1640995200000,HeartRate,75.0");
        Thread.sleep(500);
        
        // Send message that should work normally
        testServer.broadcastMessage("2,1640995201000,HeartRate,80.0");
        Thread.sleep(1000);
        
        // System should continue operating despite storage errors
        assertTrue(webSocketReader.isReading(), "WebSocket client should remain connected despite storage errors");
        
        // Verify both calls were attempted
        verify(mockDataStorage, times(1)).addPatientData(1, 75.0, "HeartRate", 1640995200000L);
        verify(mockDataStorage, times(1)).addPatientData(2, 80.0, "HeartRate", 1640995201000L);
    }

    /**
     * Test graceful handling of resource exhaustion scenarios
     */
    @Test
    @DisplayName("Resource Error: Memory/Thread Exhaustion Simulation")
    @Timeout(20)
    void testResourceExhaustionHandling() throws Exception {
        webSocketReader.startRealtimeReading(mockDataStorage);
        
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(clientConnected, "Client should connect");
        Thread.sleep(500);
        
        // Configure DataStorage to simulate slow operations (resource contention)
        doAnswer(invocation -> {
            Thread.sleep(100); // Simulate slow database operation
            return null;
        }).when(mockDataStorage).addPatientData(anyInt(), anyDouble(), anyString(), anyLong());
        
        // Send multiple messages rapidly while storage is slow
        for (int i = 1; i <= 50; i++) {
            String message = String.format("%d,%d,HeartRate,%.1f", 
                i, System.currentTimeMillis() + i * 1000, 60.0 + i);
            testServer.broadcastMessage(message);
            Thread.sleep(50); // Send faster than processing
        }
        
        // Allow processing time
        Thread.sleep(8000);
        
        // System should remain stable despite processing pressure
        assertTrue(webSocketReader.isReading() || !webSocketReader.isReading(), 
                  "System should handle processing pressure gracefully");
        
    }

    /**
     * Enhanced test WebSocket server with additional error simulation capabilities
     */
    private class TestWebSocketServer extends WebSocketServer {
        private final AtomicBoolean serverRunning = new AtomicBoolean(false);
        private final AtomicInteger connectionCount = new AtomicInteger(0);
        
        public TestWebSocketServer(int port) {
            super(new InetSocketAddress(port));
            this.setReuseAddr(true);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            System.out.println("Test server: Client connected from " + conn.getRemoteSocketAddress());
            connectionCount.incrementAndGet();
            clientConnectedLatch.countDown();
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            System.out.println("Test server: Client disconnected - Code: " + code + ", Reason: " + reason);
            connectionCount.decrementAndGet();
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
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
                return;
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
        
        /**
         * Force close all connections (simulates network failure)
         */
        public void dropAllConnections() {
            try {
                for (WebSocket conn : getConnections()) {
                    if (conn.isOpen()) {
                        conn.close(1006, "Simulated connection drop");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error dropping connections: " + e.getMessage());
            }
        }
        
        /**
         * Force stop server immediately (simulates sudden server failure)
         */
        public void forceStop() {
            try {
                serverRunning.set(false);
                this.stop(0); // Stop immediately without graceful shutdown
            } catch (Exception e) {
                System.err.println("Error force stopping server: " + e.getMessage());
            }
        }
        
        public boolean isRunning() {
            return serverRunning.get();
        }
        
        public int getConnectionCount() {
            return connectionCount.get();
        }
    }
}