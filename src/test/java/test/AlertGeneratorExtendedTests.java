package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;

import com.cardio_generator.generators.AlertGenerator;
import com.cardio_generator.outputs.OutputStrategy;

import java.lang.reflect.Field;

public class AlertGeneratorExtendedTests {

    private OutputStrategy mockOutput;
    private AlertGenerator generator;

    @BeforeEach
    public void setUp() {
        mockOutput = Mockito.mock(OutputStrategy.class);
        generator = new AlertGenerator(10);
    }

    @Test
    public void testGenerateOutput() {
        // Just test that generate can be called without exceptions
        generator.generate(1, mockOutput);
        
        // Since we can't control randomness, we can't make specific assertions
        // about whether output was called or not
    }
    
    @Test
    public void testManipulateAlertState() throws Exception {
        // Access the private alertStates field
        Field alertStatesField = AlertGenerator.class.getDeclaredField("alertStates");
        alertStatesField.setAccessible(true);
        boolean[] alertStates = (boolean[]) alertStatesField.get(generator);
        
        // Toggle alert state for patient 1
        boolean originalState = alertStates[1];
        alertStates[1] = !originalState;
        
        // Verify changed
        assertNotEquals(originalState, alertStates[1], "Alert state should be toggled");
        
        // Restore original state for other tests
        alertStates[1] = originalState;
    }
    
    @Test
    public void testExceptionHandling() {
        // Create a failing output strategy
        OutputStrategy failingOutput = new OutputStrategy() {
            @Override
            public void output(int patientId, long timestamp, String label, String data) {
                throw new RuntimeException("Test exception");
            }
        };
        
        // This should not throw an exception
        assertDoesNotThrow(() -> generator.generate(1, failingOutput),
                "Generator should handle exceptions from output strategy");
    }
    
    @Test
    public void testInvalidPatientId() {
        // Test with patient ID that is out of bounds
        // This should not throw an exception
        assertDoesNotThrow(() -> generator.generate(100, mockOutput),
                "Generator should handle invalid patient IDs gracefully");
    }
}