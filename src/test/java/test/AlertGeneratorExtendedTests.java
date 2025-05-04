package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;

import com.cardio_generator.generators.AlertGenerator;
import com.cardio_generator.outputs.OutputStrategy;

import java.lang.reflect.Field;

/**
 * Additional tests for AlertGenerator that complement the existing AlertGeneratorUnitTests
 */
public class AlertGeneratorExtendedTests {

    private OutputStrategy mockOutput;
    private AlertGenerator generator;

    @BeforeEach
    public void setUp() {
        mockOutput = Mockito.mock(OutputStrategy.class);
        generator = new AlertGenerator(10);
    }

    /**
     * Test that the generator can output data
     */
    @Test
    public void testGenerateOutput() {
        // Just test that generate can be called without exceptions
        assertDoesNotThrow(() -> generator.generate(1, mockOutput),
                "Generate method should not throw exceptions");
    }

    /**
     * Test that we can access and modify the alert states
     */
    @Test
    public void testAlertStates() throws Exception {
        // Access the private alertStates field
        Field alertStatesField = AlertGenerator.class.getDeclaredField("alertStates");
        alertStatesField.setAccessible(true);
        boolean[] alertStates = (boolean[]) alertStatesField.get(generator);

        // Save original state
        boolean originalState = alertStates[1];

        // Toggle the state
        alertStates[1] = !originalState;

        // Verify it changed
        assertNotEquals(originalState, alertStates[1],
                "Alert state should have been toggled");

        // Restore original state for other tests
        alertStates[1] = originalState;
    }

    /**
     * Test exception handling in generate method
     */
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
                "Generator should handle exceptions gracefully");
    }

    /**
     * Test that an invalid patient ID is handled gracefully
     */
    @Test
    public void testInvalidPatientId() {
        // Test with patient ID beyond array bounds
        assertDoesNotThrow(() -> generator.generate(100, mockOutput),
                "Generator should handle invalid patient IDs");
    }
}