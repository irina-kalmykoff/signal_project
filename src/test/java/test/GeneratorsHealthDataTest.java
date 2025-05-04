package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Random;

import com.cardio_generator.generators.HealthDataGenerator;
import com.cardio_generator.outputs.OutputStrategy;

/**
 * Tests for the HealthDataGenerator class in the com.cardio_generator.generators package
 */
public class GeneratorsHealthDataTest {

    private OutputStrategy mockOutput;
    private HealthDataGenerator generator;

    @BeforeEach
    public void setUp() {
        mockOutput = Mockito.mock(OutputStrategy.class);
        generator = new HealthDataGenerator();
    }

    @Test
    public void testGenerate() {
        // Test the generate method
        generator.generateData(1, mockOutput);

        // Verify that output was called at least once
        Mockito.verify(mockOutput, Mockito.atLeastOnce())
                .output(Mockito.anyInt(), Mockito.anyLong(),
                        Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testGenerateWithAlertActive() {
        // Access the private field to set up the test
        try {
            // Get the alertStates field
            java.lang.reflect.Field alertStatesField =
                    HealthDataGenerator.class.getDeclaredField("alertStates");
            alertStatesField.setAccessible(true);

            // Get the randomGenerator field
            java.lang.reflect.Field randomGeneratorField =
                    HealthDataGenerator.class.getDeclaredField("randomGenerator");
            randomGeneratorField.setAccessible(true);

            // Create a mock random that will resolve the alert
            Random mockRandom = Mockito.mock(Random.class);
            Mockito.when(mockRandom.nextDouble()).thenReturn(0.01); // Will resolve the alert

            // Set the fields
            boolean[] alertStates = (boolean[]) alertStatesField.get(generator);
            alertStates[1] = true; // Set alert as active
            randomGeneratorField.set(generator, mockRandom);

            // Call the method
            generator.generateData(1, mockOutput);

            // Verify the output was called with "Alert" and "0" (resolved)
            ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);

            Mockito.verify(mockOutput, Mockito.atLeastOnce())
                    .output(Mockito.eq(1), Mockito.anyLong(),
                            labelCaptor.capture(), dataCaptor.capture());

            // Find the Alert output
            boolean foundAlert = false;
            for (int i = 0; i < labelCaptor.getAllValues().size(); i++) {
                if ("Alert".equals(labelCaptor.getAllValues().get(i))) {
                    assertEquals("0", dataCaptor.getAllValues().get(i),
                            "Alert should be resolved (0)");
                    foundAlert = true;
                }
            }

            assertTrue(foundAlert, "Should generate an Alert");

        } catch (Exception e) {
            fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void testGenerateWithAlertInactive() {
        // Access the private field to set up the test
        try {
            // Get the alertStates field
            java.lang.reflect.Field alertStatesField =
                    HealthDataGenerator.class.getDeclaredField("alertStates");
            alertStatesField.setAccessible(true);

            // Get the randomGenerator field
            java.lang.reflect.Field randomGeneratorField =
                    HealthDataGenerator.class.getDeclaredField("randomGenerator");
            randomGeneratorField.setAccessible(true);

            // Create a mock random that will trigger the alert
            Random mockRandom = Mockito.mock(Random.class);
            Mockito.when(mockRandom.nextDouble()).thenReturn(0.01); // Will trigger the alert

            // Set the fields
            boolean[] alertStates = (boolean[]) alertStatesField.get(generator);
            alertStates[1] = false; // Set alert as inactive
            randomGeneratorField.set(generator, mockRandom);

            // Call the method
            generator.generateData(1, mockOutput);

            // Verify the output was called with "Alert" and "1" (triggered)
            ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);

            Mockito.verify(mockOutput, Mockito.atLeastOnce())
                    .output(Mockito.eq(1), Mockito.anyLong(),
                            labelCaptor.capture(), dataCaptor.capture());

            // Find the Alert output
            boolean foundAlert = false;
            for (int i = 0; i < labelCaptor.getAllValues().size(); i++) {
                if ("Alert".equals(labelCaptor.getAllValues().get(i))) {
                    assertEquals("1", dataCaptor.getAllValues().get(i),
                            "Alert should be triggered (1)");
                    foundAlert = true;
                }
            }

            assertTrue(foundAlert, "Should generate an Alert");

        } catch (Exception e) {
            fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void testGenerateExceptionHandling() {
        // Create a failing output strategy
        OutputStrategy failingOutput = new OutputStrategy() {
            @Override
            public void output(int patientId, long timestamp, String label, String data) {
                if ("Alert".equals(label)) {
                    throw new RuntimeException("Test exception");
                }
            }
        };

        // Setup for alert to be triggered
        try {
            // Get the alertStates field
            java.lang.reflect.Field alertStatesField =
                    HealthDataGenerator.class.getDeclaredField("alertStates");
            alertStatesField.setAccessible(true);

            // Get the randomGenerator field
            java.lang.reflect.Field randomGeneratorField =
                    HealthDataGenerator.class.getDeclaredField("randomGenerator");
            randomGeneratorField.setAccessible(true);

            // Create a mock random that will trigger the alert
            Random mockRandom = Mockito.mock(Random.class);
            Mockito.when(mockRandom.nextDouble()).thenReturn(0.01); // Will trigger the alert

            // Set the fields
            boolean[] alertStates = (boolean[]) alertStatesField.get(generator);
            alertStates[1] = false; // Set alert as inactive
            randomGeneratorField.set(generator, mockRandom);

            // Call should not throw exception
            assertDoesNotThrow(() -> generator.generateData(1, failingOutput),
                    "Generate should handle exceptions gracefully");

        } catch (Exception e) {
            fail("Exception occurred: " + e.getMessage());
        }
    }
}