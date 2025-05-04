package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;

import com.cardio_generator.outputs.WebSocketOutputStrategy;

public class WebSocketOutputStrategyTests {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testConstructor() {
        // Just test that the constructor logs a message about creation
        WebSocketOutputStrategy strategy = new WebSocketOutputStrategy(0);
        String output = outContent.toString();
        assertTrue(output.contains("WebSocket server created"),
                "Constructor should log server creation");
    }

    @Test
    public void testOutputWithNoClients() {
        WebSocketOutputStrategy strategy = new WebSocketOutputStrategy(0);

        // Should not throw exception when no clients are connected
        assertDoesNotThrow(() -> strategy.output(1, 1000, "Label", "Data"));
    }

    @Test
    public void testOutputWithMockedServer() throws Exception {
        // Create the strategy
        WebSocketOutputStrategy strategy = new WebSocketOutputStrategy(0);

        // Get the server field
        Field serverField = WebSocketOutputStrategy.class.getDeclaredField("server");
        serverField.setAccessible(true);

        // Replace with mock server
        WebSocketServer originalServer = (WebSocketServer) serverField.get(strategy);

        try {
            // Create mock server and connections
            WebSocketServer mockServer = Mockito.mock(WebSocketServer.class);
            WebSocket mockConnection = Mockito.mock(WebSocket.class);
            Set<WebSocket> connections = new HashSet<>();
            connections.add(mockConnection);

            // Set up mock behavior
            Mockito.when(mockServer.getConnections()).thenReturn(connections);

            // Replace the server
            serverField.set(strategy, mockServer);

            // Now test output
            strategy.output(1, 1000, "Label", "Data");

            // Verify send was called on the connection
            Mockito.verify(mockConnection).send(Mockito.contains("1,1000,Label,Data"));

        } finally {
            // Restore original server
            if (originalServer != null) {
                serverField.set(strategy, originalServer);
            }
        }
    }

    @Test
    public void testOutputFormatting() throws Exception {
        // Create the strategy
        WebSocketOutputStrategy strategy = new WebSocketOutputStrategy(0);

        // Get the server field
        Field serverField = WebSocketOutputStrategy.class.getDeclaredField("server");
        serverField.setAccessible(true);

        // Replace with mock server
        WebSocketServer originalServer = (WebSocketServer) serverField.get(strategy);

        try {
            // Create mock server and connections
            WebSocketServer mockServer = Mockito.mock(WebSocketServer.class);
            WebSocket mockConnection = Mockito.mock(WebSocket.class);
            Set<WebSocket> connections = new HashSet<>();
            connections.add(mockConnection);

            // Set up mock behavior
            Mockito.when(mockServer.getConnections()).thenReturn(connections);

            // Replace the server
            serverField.set(strategy, mockServer);

            // Test output with various data
            strategy.output(1, 1000, "Label", "Simple Data");
            strategy.output(2, 2000, "Label2", "Data with, comma");
            strategy.output(3, 3000, "Label3", "Data with \"quotes\"");

            // Verify all sends were formatted correctly
            Mockito.verify(mockConnection).send("1,1000,Label,Simple Data");
            Mockito.verify(mockConnection).send("2,2000,Label2,Data with, comma");
            Mockito.verify(mockConnection).send("3,3000,Label3,Data with \"quotes\"");

        } finally {
            // Restore original server
            if (originalServer != null) {
                serverField.set(strategy, originalServer);
            }
        }
    }

    @Test
    public void testMultipleConnections() throws Exception {
        // Create the strategy
        WebSocketOutputStrategy strategy = new WebSocketOutputStrategy(0);

        // Get the server field
        Field serverField = WebSocketOutputStrategy.class.getDeclaredField("server");
        serverField.setAccessible(true);

        // Replace with mock server
        WebSocketServer originalServer = (WebSocketServer) serverField.get(strategy);

        try {
            // Create mock server and multiple connections
            WebSocketServer mockServer = Mockito.mock(WebSocketServer.class);
            WebSocket mockConnection1 = Mockito.mock(WebSocket.class);
            WebSocket mockConnection2 = Mockito.mock(WebSocket.class);
            Set<WebSocket> connections = new HashSet<>();
            connections.add(mockConnection1);
            connections.add(mockConnection2);

            // Set up mock behavior
            Mockito.when(mockServer.getConnections()).thenReturn(connections);

            // Replace the server
            serverField.set(strategy, mockServer);

            // Now test output
            strategy.output(1, 1000, "Label", "Data");

            // Verify send was called on both connections
            Mockito.verify(mockConnection1).send(Mockito.anyString());
            Mockito.verify(mockConnection2).send(Mockito.anyString());

        } finally {
            // Restore original server
            if (originalServer != null) {
                serverField.set(strategy, originalServer);
            }
        }
    }

    @Test
    public void testNoConnections() throws Exception {
        // Create the strategy
        WebSocketOutputStrategy strategy = new WebSocketOutputStrategy(0);

        // Get the server field
        Field serverField = WebSocketOutputStrategy.class.getDeclaredField("server");
        serverField.setAccessible(true);

        // Replace with mock server
        WebSocketServer originalServer = (WebSocketServer) serverField.get(strategy);

        try {
            // Create mock server with no connections
            WebSocketServer mockServer = Mockito.mock(WebSocketServer.class);
            Set<WebSocket> emptyConnections = Collections.emptySet();

            // Set up mock behavior
            Mockito.when(mockServer.getConnections()).thenReturn(emptyConnections);

            // Replace the server
            serverField.set(strategy, mockServer);

            // Should not throw exception when no clients are connected
            assertDoesNotThrow(() -> strategy.output(1, 1000, "Label", "Data"));

        } finally {
            // Restore original server
            if (originalServer != null) {
                serverField.set(strategy, originalServer);
            }
        }
    }
}