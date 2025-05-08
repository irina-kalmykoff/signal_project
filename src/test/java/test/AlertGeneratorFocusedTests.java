package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.cardio_generator.generators.AlertGenerator;
import com.cardio_generator.outputs.OutputStrategy;

import java.lang.reflect.Field;

/**
 * Focused tests to improve branch coverage for AlertGenerator
 */
public class AlertGeneratorFocusedTests {

    private AlertGenerator generator;
    private OutputStrategy mockOutput;

    @BeforeEach
    public void setUp() {
        generator = new AlertGenerator(10);
        mockOutput = Mockito.mock(OutputStrategy.class);
    }

    /**
     * Test the positive path - a patient ID within bounds
     */
    @Test
    public void testValidPatientId() {
        // This should be handled normally without exceptions
        assertDoesNotThrow(() -> generator.generate(5, mockOutput));
    }

    /**
     * Test the negative path - a patient ID below bounds
     */
    @Test
    public void testPatientIdBelowBounds() {
        // This should be handled gracefully without exceptions
        assertDoesNotThrow(() -> generator.generate(-1, mockOutput));

        // Verify no output was generated for invalid ID
        Mockito.verify(mockOutput, Mockito.never()).output(
                Mockito.anyInt(),
                Mockito.anyLong(),
                Mockito.anyString(),
                Mockito.anyString()
        );
    }

    /**
     * Test the negative path - a patient ID above bounds
     */
    @Test
    public void testPatientIdAboveBounds() {
        // This should be handled gracefully without exceptions
        assertDoesNotThrow(() -> generator.generate(100, mockOutput));

        // Verify no output was generated for invalid ID
        Mockito.verify(mockOutput, Mockito.never()).output(
                Mockito.anyInt(),
                Mockito.anyLong(),
                Mockito.anyString(),
                Mockito.anyString()
        );
    }

    /**
     * Test exception handling when output throws an exception
     */
    @Test
    public void testOutputException() {
        // Create an output strategy that throws an exception
        OutputStrategy failingOutput = new OutputStrategy() {
            @Override
            public void output(int patientId, long timestamp, String label, String data) {
                throw new RuntimeException("Test exception");
            }
        };

        // This should handle the exception gracefully
        assertDoesNotThrow(() -> generator.generate(5, failingOutput));
    }

    /**
     * Test behavior with a null output strategy
     */
    @Test
    public void testNullOutputStrategy() {
        // This should handle null gracefully
        assertDoesNotThrow(() -> generator.generate(5, null));
    }

    /**
     * Test with a mocked random generator that always returns a low value
     * to trigger an alert
     */
    @Test
    public void testAlwaysTriggerAlert() throws Exception {
        // Ensure no active alert first
        Field alertStatesField = AlertGenerator.class.getDeclaredField("alertStates");
        alertStatesField.setAccessible(true);
        boolean[] alertStates = (boolean[]) alertStatesField.get(generator);
        alertStates[5] = false;

        // Create a mock that will force triggering an alert
        OutputStrategy verifyOutput = Mockito.mock(OutputStrategy.class);

        // Since we can't override the static final Random, we're going to try
        // enough times that the alert should be triggered at least once
        boolean alertTriggered = false;
        for (int i = 0; i < 50; i++) {
            generator.generate(5, verifyOutput);

            try {
                Mockito.verify(verifyOutput, Mockito.atLeastOnce()).output(
                        Mockito.eq(5),
                        Mockito.anyLong(),
                        Mockito.eq("Alert"),
                        Mockito.eq("triggered")
                );
                alertTriggered = true;
                break;
            } catch (Error e) {
                // Verification failed, continue loop
            }
        }

        assertTrue(alertTriggered, "Alert should be triggered eventually");
    }

    /**
     * Test with a mocked random generator that always returns a low value
     * to resolve an alert
     */
    @Test
    public void testAlwaysResolveAlert() throws Exception {
        // Ensure active alert first
        Field alertStatesField = AlertGenerator.class.getDeclaredField("alertStates");
        alertStatesField.setAccessible(true);
        boolean[] alertStates = (boolean[]) alertStatesField.get(generator);
        alertStates[5] = true;

        // Create a mock that will force resolving an alert
        OutputStrategy verifyOutput = Mockito.mock(OutputStrategy.class);

        // Since we can't override the static final Random, we're going to try
        // enough times that the alert should be resolved at least once
        boolean alertResolved = false;
        for (int i = 0; i < 50; i++) {
            generator.generate(5, verifyOutput);

            try {
                Mockito.verify(verifyOutput, Mockito.atLeastOnce()).output(
                        Mockito.eq(5),
                        Mockito.anyLong(),
                        Mockito.eq("Alert"),
                        Mockito.eq("resolved")
                );
                alertResolved = true;
                break;
            } catch (Error e) {
                // Verification failed, continue loop
            }
        }

        assertTrue(alertResolved, "Alert should be resolved eventually");
    }
}