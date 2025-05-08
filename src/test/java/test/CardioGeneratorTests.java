package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.cardio_generator.generators.HealthDataGenerator;
import com.cardio_generator.outputs.OutputStrategy;
import com.cardio_generator.outputs.FileOutputStrategy;
import com.cardio_generator.outputs.ConsoleOutputStrategy;

/**
 * Comprehensive tests for the com.cardio_generator package.
 * This focuses specifically on increasing test coverage for the HealthDataGenerator class.
 */
public class CardioGeneratorTests {

    private OutputStrategy mockOutput;
    private HealthDataGenerator generator;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        mockOutput = Mockito.mock(OutputStrategy.class);
        generator = new HealthDataGenerator();
    }

    @Test
    public void testHealthDataGeneratorMultipleCalls() {
        // Test many iterations to cover all code paths
        for (int i = 0; i < 200; i++) {
            generator.generateData(1, mockOutput);
        }

        // Verify minimum expected calls
        ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mockOutput, Mockito.atLeast(200))
                .output(Mockito.anyInt(), Mockito.anyLong(),
                        labelCaptor.capture(), Mockito.anyString());

        // Get all captured labels
        List<String> capturedLabels = labelCaptor.getAllValues();

        // Verify all expected vital signs were generated
        assertTrue(capturedLabels.contains("SystolicPressure"), "Should generate systolic pressure");
        assertTrue(capturedLabels.contains("DiastolicPressure"), "Should generate diastolic pressure");
        assertTrue(capturedLabels.contains("Saturation"), "Should generate oxygen saturation");
        assertTrue(capturedLabels.contains("ECG"), "Should generate ECG readings");
    }

    @Test
    public void testHealthDataGeneratorMultiplePatients() {
        // Test with multiple patient IDs
        for (int patientId = 1; patientId <= 10; patientId++) {
            generator.generateData(patientId, mockOutput);
        }

        // Verify calls for each patient
        for (int patientId = 1; patientId <= 10; patientId++) {
            final int id = patientId; // Final variable for lambda
            Mockito.verify(mockOutput, Mockito.atLeastOnce())
                    .output(Mockito.eq(id), Mockito.anyLong(),
                            Mockito.anyString(), Mockito.anyString());
        }
    }

    @Test
    public void testHealthDataGeneratorWithRealOutputs() {
        // Test with real output strategies rather than mocks
        OutputStrategy fileOutput = new FileOutputStrategy(tempDir.toString());
        OutputStrategy consoleOutput = new ConsoleOutputStrategy();

        // Generate data with file output
        assertDoesNotThrow(() -> {
            generator.generateData(1, fileOutput);
        }, "Should not throw with file output");

        // Generate data with console output
        assertDoesNotThrow(() -> {
            generator.generateData(1, consoleOutput);
        }, "Should not throw with console output");
    }

    @Test
    public void testWithConcurrency() throws Exception {
        // Test concurrent access
        final int threadCount = 5;
        final CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int patientId = i + 1;
            new Thread(() -> {
                try {
                    for (int j = 0; j < 20; j++) {
                        generator.generateData(patientId, mockOutput);
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Wait for all threads to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete in time");

        // Verify calls were made for each patient
        for (int patientId = 1; patientId <= threadCount; patientId++) {
            final int id = patientId;
            Mockito.verify(mockOutput, Mockito.atLeastOnce())
                    .output(Mockito.eq(id), Mockito.anyLong(),
                            Mockito.anyString(), Mockito.anyString());
        }
    }

    @Test
    public void testAlertStateTransitions() {
        // Create a test-specific output strategy to capture alerts
        final boolean[] alertWasTriggered = {false};
        final boolean[] alertWasResolved = {false};

        OutputStrategy testOutput = new OutputStrategy() {
            @Override
            public void output(int patientId, long timestamp, String label, String data) {
                if ("Alert".equals(label)) {
                    if ("1".equals(data)) {
                        alertWasTriggered[0] = true;
                    } else if ("0".equals(data)) {
                        alertWasResolved[0] = true;
                    }
                }
            }
        };

        // Force alert state transitions by running many iterations
        for (int i = 0; i < 1000; i++) {
            generator.generateData(1, testOutput);

            // If we've seen both transitions, we can stop early
            if (alertWasTriggered[0] && alertWasResolved[0]) {
                break;
            }
        }

        // We should have observed both alert states by now
        assertTrue(alertWasTriggered[0], "Alert should have been triggered at least once");
        assertTrue(alertWasResolved[0], "Alert should have been resolved at least once");
    }

    @Test
    public void testVitalSignDistribution() {
        // Capture vital sign values
        final double[] minSystolic = {Double.MAX_VALUE};
        final double[] maxSystolic = {Double.MIN_VALUE};
        final double[] minDiastolic = {Double.MAX_VALUE};
        final double[] maxDiastolic = {Double.MIN_VALUE};
        final double[] minSaturation = {Double.MAX_VALUE};
        final double[] maxSaturation = {Double.MIN_VALUE};

        OutputStrategy testOutput = new OutputStrategy() {
            @Override
            public void output(int patientId, long timestamp, String label, String data) {
                try {
                    double value = Double.parseDouble(data);

                    if ("SystolicPressure".equals(label)) {
                        minSystolic[0] = Math.min(minSystolic[0], value);
                        maxSystolic[0] = Math.max(maxSystolic[0], value);
                    } else if ("DiastolicPressure".equals(label)) {
                        minDiastolic[0] = Math.min(minDiastolic[0], value);
                        maxDiastolic[0] = Math.max(maxDiastolic[0], value);
                    } else if ("Saturation".equals(label)) {
                        minSaturation[0] = Math.min(minSaturation[0], value);
                        maxSaturation[0] = Math.max(maxSaturation[0], value);
                    }
                } catch (NumberFormatException e) {
                    // Skip non-numeric data
                }
            }
        };

        // Generate many data points
        for (int i = 0; i < 100; i++) {
            generator.generateData(1, testOutput);
        }

        // Verify value ranges
        // For systolic pressure (typically 90-180)
        assertTrue(minSystolic[0] < 130, "Minimum systolic should be < 130");
        assertTrue(maxSystolic[0] > 100, "Maximum systolic should be > 100");

        // For diastolic pressure (typically 60-110)
        assertTrue(minDiastolic[0] < 90, "Minimum diastolic should be < 90");
        assertTrue(maxDiastolic[0] > 70, "Maximum diastolic should be > 70");

        // For saturation (typically 85-100%)
        assertTrue(minSaturation[0] < 100, "Minimum saturation should be < 100");
        assertTrue(maxSaturation[0] <= 100, "Maximum saturation should be <= 100");
    }
}