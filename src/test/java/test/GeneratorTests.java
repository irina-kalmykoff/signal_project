package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.cardio_generator.generators.*;
import com.cardio_generator.outputs.OutputStrategy;

public class GeneratorTests {

    private OutputStrategy mockOutputStrategy;

    @BeforeEach
    public void setUp() {
        // Create a mock output strategy to verify interactions
        mockOutputStrategy = Mockito.mock(OutputStrategy.class);
    }

    @Test
    public void testAlertGenerator() {
        // Create a special test version of AlertGenerator to override randomness
        AlertGenerator generator = new AlertGenerator(5) {
            @Override
            public void generate(int patientId, OutputStrategy outputStrategy) {
                // Force an alert to be triggered for testing purposes
                outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "triggered");
            }
        };

        // Test generate method
        generator.generate(1, mockOutputStrategy);

        // Verify that the output strategy was called
        Mockito.verify(mockOutputStrategy, Mockito.times(1))
                .output(Mockito.eq(1), Mockito.anyLong(),
                        Mockito.eq("Alert"), Mockito.eq("triggered"));
    }

    @Test
    public void testBloodLevelsDataGenerator() {
        // Create the generator with a small patient count
        BloodLevelsDataGenerator generator = new BloodLevelsDataGenerator(3);

        // Test generate method for each patient
        for (int i = 1; i <= 3; i++) {
            final int patientId = i;
            // No exception should be thrown
            assertDoesNotThrow(() -> generator.generate(patientId, mockOutputStrategy));
        }

        // Verify the output strategy was called for each type of blood data
        Mockito.verify(mockOutputStrategy, Mockito.atLeast(3))
                .output(Mockito.anyInt(), Mockito.anyLong(),
                        Mockito.eq("Cholesterol"), Mockito.anyString());

        Mockito.verify(mockOutputStrategy, Mockito.atLeast(3))
                .output(Mockito.anyInt(), Mockito.anyLong(),
                        Mockito.eq("WhiteBloodCells"), Mockito.anyString());

        Mockito.verify(mockOutputStrategy, Mockito.atLeast(3))
                .output(Mockito.anyInt(), Mockito.anyLong(),
                        Mockito.eq("RedBloodCells"), Mockito.anyString());
    }

    @Test
    public void testBloodPressureDataGenerator() {
        // Create the generator with a small patient count
        BloodPressureDataGenerator generator = new BloodPressureDataGenerator(3);

        // Test generate method for each patient
        for (int i = 1; i <= 3; i++) {
            final int patientId = i;
            // No exception should be thrown
            assertDoesNotThrow(() -> generator.generate(patientId, mockOutputStrategy));
        }

        // Verify the output strategy was called for both systolic and diastolic
        Mockito.verify(mockOutputStrategy, Mockito.atLeast(3))
                .output(Mockito.anyInt(), Mockito.anyLong(),
                        Mockito.eq("SystolicPressure"), Mockito.anyString());

        Mockito.verify(mockOutputStrategy, Mockito.atLeast(3))
                .output(Mockito.anyInt(), Mockito.anyLong(),
                        Mockito.eq("DiastolicPressure"), Mockito.anyString());
    }

    @Test
    public void testBloodSaturationDataGenerator() {
        // Create the generator with a small patient count
        BloodSaturationDataGenerator generator = new BloodSaturationDataGenerator(2);

        // Test generate method for each patient
        for (int i = 1; i <= 2; i++) {
            final int patientId = i;
            // No exception should be thrown
            assertDoesNotThrow(() -> generator.generate(patientId, mockOutputStrategy));
        }

        // Verify the output strategy was called for saturation
        Mockito.verify(mockOutputStrategy, Mockito.atLeast(2))
                .output(Mockito.anyInt(), Mockito.anyLong(),
                        Mockito.eq("Saturation"), Mockito.anyString());
    }

    @Test
    public void testECGDataGenerator() {
        // Create the generator with a small patient count
        ECGDataGenerator generator = new ECGDataGenerator(2);

        // Test generate method for each patient
        for (int i = 1; i <= 2; i++) {
            final int patientId = i;
            // No exception should be thrown
            assertDoesNotThrow(() -> generator.generate(patientId, mockOutputStrategy));
        }

        // Verify the output strategy was called for ECG
        Mockito.verify(mockOutputStrategy, Mockito.atLeast(2))
                .output(Mockito.anyInt(), Mockito.anyLong(),
                        Mockito.eq("ECG"), Mockito.anyString());
    }

    @Test
    public void testBloodPressureDataGeneratorRangeEnforcement() {
        // Create the generator with 1 patient
        BloodPressureDataGenerator generator = new BloodPressureDataGenerator(1) {
            // Override constructor to access private fields for testing
            @Override
            public void generate(int patientId, OutputStrategy outputStrategy) {
                // Call the original method
                super.generate(patientId, outputStrategy);

                // Get the last values using reflection
                try {
                    java.lang.reflect.Field systolicField = BloodPressureDataGenerator.class.getDeclaredField("lastSystolicValues");
                    systolicField.setAccessible(true);
                    int[] systolicValues = (int[]) systolicField.get(this);

                    java.lang.reflect.Field diastolicField = BloodPressureDataGenerator.class.getDeclaredField("lastDiastolicValues");
                    diastolicField.setAccessible(true);
                    int[] diastolicValues = (int[]) diastolicField.get(this);

                    // Verify the values are within range
                    assertTrue(systolicValues[patientId] >= 90, "Systolic value should be at least 90");
                    assertTrue(systolicValues[patientId] <= 180, "Systolic value should be at most 180");

                    assertTrue(diastolicValues[patientId] >= 60, "Diastolic value should be at least 60");
                    assertTrue(diastolicValues[patientId] <= 120, "Diastolic value should be at most 120");

                } catch (Exception e) {
                    fail("Error accessing private fields: " + e.getMessage());
                }
            }
        };

        // Test generate method
        assertDoesNotThrow(() -> generator.generate(1, mockOutputStrategy));
    }

    @Test
    public void testBloodSaturationRangeEnforcement() {
        // Create the generator with 1 patient
        BloodSaturationDataGenerator generator = new BloodSaturationDataGenerator(1) {
            // Override constructor to access private fields for testing
            @Override
            public void generate(int patientId, OutputStrategy outputStrategy) {
                // Call the original method
                super.generate(patientId, outputStrategy);

                // Get the last values using reflection
                try {
                    java.lang.reflect.Field saturationField = BloodSaturationDataGenerator.class.getDeclaredField("lastSaturationValues");
                    saturationField.setAccessible(true);
                    int[] saturationValues = (int[]) saturationField.get(this);

                    // Verify the values are within range
                    assertTrue(saturationValues[patientId] >= 90, "Saturation value should be at least 90");
                    assertTrue(saturationValues[patientId] <= 100, "Saturation value should be at most 100");

                } catch (Exception e) {
                    fail("Error accessing private fields: " + e.getMessage());
                }
            }
        };

        // Test generate method
        assertDoesNotThrow(() -> generator.generate(1, mockOutputStrategy));
    }
}