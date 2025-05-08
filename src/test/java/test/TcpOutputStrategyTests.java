package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.io.PrintWriter;
import java.lang.reflect.Field;

import com.cardio_generator.outputs.TcpOutputStrategy;

public class TcpOutputStrategyTests {

    private TcpOutputStrategy strategy;

    @BeforeEach
    public void setUp() {
        strategy = new TcpOutputStrategy(0); // Use port 0 for auto-assignment
    }

    @Test
    public void testOutputWithNoClient() {
        // Should not throw exception when no client is connected
        assertDoesNotThrow(() -> strategy.output(1, 1000, "Label", "Data"));
    }

    @Test
    public void testOutputWithMockedClient() throws Exception {
        // Get the out field (PrintWriter)
        Field outField = TcpOutputStrategy.class.getDeclaredField("out");
        outField.setAccessible(true);
        
        // Save original writer
        PrintWriter originalWriter = (PrintWriter) outField.get(strategy);
        
        try {
            // Create mock PrintWriter
            PrintWriter mockWriter = Mockito.mock(PrintWriter.class);
            
            // Replace the writer
            outField.set(strategy, mockWriter);
            
            // Test output
            strategy.output(1, 1000, "Label", "Data");
            
            // Verify println was called
            Mockito.verify(mockWriter).println("1,1000,Label,Data");
        } finally {
            // Restore original writer
            outField.set(strategy, originalWriter);
        }
    }
    
    @Test
    public void testMessageFormat() throws Exception {
        // Get the out field (PrintWriter)
        Field outField = TcpOutputStrategy.class.getDeclaredField("out");
        outField.setAccessible(true);
        
        // Save original writer
        PrintWriter originalWriter = (PrintWriter) outField.get(strategy);
        
        try {
            // Create mock PrintWriter
            PrintWriter mockWriter = Mockito.mock(PrintWriter.class);
            
            // Replace the writer
            outField.set(strategy, mockWriter);
            
            // Test different data formats
            strategy.output(1, 1000, "Label1", "Data1");
            strategy.output(2, 2000, "Label2", "Data with spaces");
            strategy.output(3, 3000, "Label3", "Data with, commas");
            strategy.output(4, 4000, "Label4", "Data with \"quotes\"");
            
            // Verify all println calls
            Mockito.verify(mockWriter).println("1,1000,Label1,Data1");
            Mockito.verify(mockWriter).println("2,2000,Label2,Data with spaces");
            Mockito.verify(mockWriter).println("3,3000,Label3,Data with, commas");
            Mockito.verify(mockWriter).println("4,4000,Label4,Data with \"quotes\"");
        } finally {
            // Restore original writer
            outField.set(strategy, originalWriter);
        }
    }
    
    @Test
    public void testServerSocketException() {
        // Just verify that constructor doesn't throw an exception with invalid port
        assertDoesNotThrow(() -> {
            // Try to create a server socket on a privileged port (which should fail gracefully)
            TcpOutputStrategy privilegedStrategy = new TcpOutputStrategy(1);
            
            // Verify that output doesn't throw even if no client is connected
            privilegedStrategy.output(1, 1000, "Label", "Data");
        });
    }
}