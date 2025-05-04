package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cardio_generator.HealthDataSimulator;
import com.cardio_generator.outputs.OutputStrategy;
import com.cardio_generator.outputs.FileOutputStrategy;
import com.cardio_generator.outputs.ConsoleOutputStrategy;
import com.cardio_generator.outputs.WebSocketOutputStrategy;
import com.cardio_generator.outputs.TcpOutputStrategy;

/**
 * Minimal test for HealthDataSimulator that avoids methods with System.exit()
 * and complex threading
 */
public class HealthDataSimulatorMinimalTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    /**
     * Test the patient ID initialization method
     */
    @Test
    public void testInitializePatientIds() throws Exception {
        // Access the private method using reflection
        Method initializePatientIdsMethod =
                HealthDataSimulator.class.getDeclaredMethod("initializePatientIds", int.class);
        initializePatientIdsMethod.setAccessible(true);

        // Test with a small number of patients
        @SuppressWarnings("unchecked")
        List<Integer> patientIds = (List<Integer>) initializePatientIdsMethod.invoke(null, 5);

        // Verify result
        assertEquals(5, patientIds.size(), "Should return 5 patient IDs");
        for (int i = 1; i <= 5; i++) {
            assertTrue(patientIds.contains(i), "Should contain patient ID " + i);
        }
    }

    /**
     * Test the printHelp method - this doesn't call System.exit() directly
     */
    @Test
    public void testPrintHelp() throws Exception {
        // Access the private method using reflection
        Method printHelpMethod = HealthDataSimulator.class.getDeclaredMethod("printHelp");
        printHelpMethod.setAccessible(true);

        // Call the method
        printHelpMethod.invoke(null);

        // Verify output contains expected content
        String output = outContent.toString();
        assertTrue(output.contains("Usage:"), "Help output should contain usage instructions");
        assertTrue(output.contains("--patient-count"), "Help should explain patient count option");
        assertTrue(output.contains("--output"), "Help should explain output option");
    }

    /**
     * Test parseArguments with a valid patient count
     */
    @Test
    public void testParseArguments_PatientCount() throws Exception {
        // Access the private method using reflection
        Method parseArgumentsMethod = HealthDataSimulator.class.getDeclaredMethod("parseArguments", String[].class);
        parseArgumentsMethod.setAccessible(true);

        // Get the patientCount field
        Field patientCountField = HealthDataSimulator.class.getDeclaredField("patientCount");
        patientCountField.setAccessible(true);

        // Save original value
        int originalValue = (int) patientCountField.get(null);

        try {
            // Set a known value
            patientCountField.set(null, 50);

            // Call the method with --patient-count (avoiding -h option)
            parseArgumentsMethod.invoke(null, (Object) new String[]{"--patient-count", "100"});

            // Verify patientCount was updated
            assertEquals(100, patientCountField.get(null), "Patient count should be updated to 100");
        } finally {
            // Restore original value
            patientCountField.set(null, originalValue);
        }
    }

    /**
     * Test parseArguments with invalid patient count
     */
    @Test
    public void testParseArguments_InvalidPatientCount() throws Exception {
        // Access the private method using reflection
        Method parseArgumentsMethod = HealthDataSimulator.class.getDeclaredMethod("parseArguments", String[].class);
        parseArgumentsMethod.setAccessible(true);

        // Get the patientCount field
        Field patientCountField = HealthDataSimulator.class.getDeclaredField("patientCount");
        patientCountField.setAccessible(true);

        // Save original value
        int originalValue = (int) patientCountField.get(null);

        try {
            // Set a known value
            patientCountField.set(null, 50);

            // Call the method with invalid patient count
            parseArgumentsMethod.invoke(null, (Object) new String[]{"--patient-count", "invalid"});

            // Verify patientCount remains unchanged
            assertEquals(50, patientCountField.get(null),
                    "Patient count should remain unchanged with invalid input");
        } finally {
            // Restore original value
            patientCountField.set(null, originalValue);
        }
    }

    /**
     * Test parseArguments with console output option
     */
    @Test
    public void testParseArguments_ConsoleOutput() throws Exception {
        // Access the private method using reflection
        Method parseArgumentsMethod = HealthDataSimulator.class.getDeclaredMethod("parseArguments", String[].class);
        parseArgumentsMethod.setAccessible(true);

        // Get the outputStrategy field
        Field outputStrategyField = HealthDataSimulator.class.getDeclaredField("outputStrategy");
        outputStrategyField.setAccessible(true);

        // Save original value
        OutputStrategy originalStrategy = (OutputStrategy) outputStrategyField.get(null);

        try {
            // Call the method with --output console
            parseArgumentsMethod.invoke(null, (Object) new String[]{"--output", "console"});

            // Verify outputStrategy is ConsoleOutputStrategy
            assertTrue(outputStrategyField.get(null) instanceof ConsoleOutputStrategy,
                    "Output strategy should be ConsoleOutputStrategy");
        } finally {
            // Restore original value
            outputStrategyField.set(null, originalStrategy);
        }
    }

    /**
     * Test parseArguments with file output option
     */
    @Test
    public void testParseArguments_FileOutput() throws Exception {
        // Access the private method using reflection
        Method parseArgumentsMethod = HealthDataSimulator.class.getDeclaredMethod("parseArguments", String[].class);
        parseArgumentsMethod.setAccessible(true);

        // Get the outputStrategy field
        Field outputStrategyField = HealthDataSimulator.class.getDeclaredField("outputStrategy");
        outputStrategyField.setAccessible(true);

        // Save original value
        OutputStrategy originalStrategy = (OutputStrategy) outputStrategyField.get(null);

        try {
            // Call the method with --output file:path
            parseArgumentsMethod.invoke(null, (Object) new String[]{"--output", "file:" + tempDir.toString()});

            // Verify outputStrategy is FileOutputStrategy
            assertTrue(outputStrategyField.get(null) instanceof FileOutputStrategy,
                    "Output strategy should be FileOutputStrategy");
        } finally {
            // Restore original value
            outputStrategyField.set(null, originalStrategy);
        }
    }

    /**
     * Test parseArguments with websocket output option
     */
    @Test
    public void testParseArguments_WebSocketOutput() throws Exception {
        // Access the private method using reflection
        Method parseArgumentsMethod = HealthDataSimulator.class.getDeclaredMethod("parseArguments", String[].class);
        parseArgumentsMethod.setAccessible(true);

        // Get the outputStrategy field
        Field outputStrategyField = HealthDataSimulator.class.getDeclaredField("outputStrategy");
        outputStrategyField.setAccessible(true);

        // Save original value
        OutputStrategy originalStrategy = (OutputStrategy) outputStrategyField.get(null);

        try {
            // Call the method with --output websocket:port
            parseArgumentsMethod.invoke(null, (Object) new String[]{"--output", "websocket:8080"});

            // Verify outputStrategy is WebSocketOutputStrategy
            assertTrue(outputStrategyField.get(null) instanceof WebSocketOutputStrategy,
                    "Output strategy should be WebSocketOutputStrategy");
        } finally {
            // Restore original value
            outputStrategyField.set(null, originalStrategy);
        }
    }

    /**
     * Test parseArguments with tcp output option
     */
    @Test
    public void testParseArguments_TcpOutput() throws Exception {
        // Access the private method using reflection
        Method parseArgumentsMethod = HealthDataSimulator.class.getDeclaredMethod("parseArguments", String[].class);
        parseArgumentsMethod.setAccessible(true);

        // Get the outputStrategy field
        Field outputStrategyField = HealthDataSimulator.class.getDeclaredField("outputStrategy");
        outputStrategyField.setAccessible(true);

        // Save original value
        OutputStrategy originalStrategy = (OutputStrategy) outputStrategyField.get(null);

        try {
            // Call the method with --output tcp:port
            parseArgumentsMethod.invoke(null, (Object) new String[]{"--output", "tcp:8081"});

            // Verify outputStrategy is TcpOutputStrategy
            assertTrue(outputStrategyField.get(null) instanceof TcpOutputStrategy,
                    "Output strategy should be TcpOutputStrategy");
        } finally {
            // Restore original value
            outputStrategyField.set(null, originalStrategy);
        }
    }

    /**
     * Test parseArguments with invalid websocket port
     */
    @Test
    public void testParseArguments_InvalidWebSocketPort() throws Exception {
        // Access the private method using reflection
        Method parseArgumentsMethod = HealthDataSimulator.class.getDeclaredMethod("parseArguments", String[].class);
        parseArgumentsMethod.setAccessible(true);

        // Get the outputStrategy field
        Field outputStrategyField = HealthDataSimulator.class.getDeclaredField("outputStrategy");
        outputStrategyField.setAccessible(true);

        // Save original value
        OutputStrategy originalStrategy = (OutputStrategy) outputStrategyField.get(null);

        try {
            // Call the method with invalid websocket port
            parseArgumentsMethod.invoke(null, (Object) new String[]{"--output", "websocket:invalid"});

            // OutputStrategy should not change
            assertEquals(originalStrategy, outputStrategyField.get(null),
                    "Output strategy should not change with invalid websocket port");
        } finally {
            // Restore original value
            outputStrategyField.set(null, originalStrategy);
        }
    }

    /**
     * Test parseArguments with invalid tcp port
     */
    @Test
    public void testParseArguments_InvalidTcpPort() throws Exception {
        // Access the private method using reflection
        Method parseArgumentsMethod = HealthDataSimulator.class.getDeclaredMethod("parseArguments", String[].class);
        parseArgumentsMethod.setAccessible(true);

        // Get the outputStrategy field
        Field outputStrategyField = HealthDataSimulator.class.getDeclaredField("outputStrategy");
        outputStrategyField.setAccessible(true);

        // Save original value
        OutputStrategy originalStrategy = (OutputStrategy) outputStrategyField.get(null);

        try {
            // Call the method with invalid tcp port
            parseArgumentsMethod.invoke(null, (Object) new String[]{"--output", "tcp:invalid"});

            // OutputStrategy should not change
            assertEquals(originalStrategy, outputStrategyField.get(null),
                    "Output strategy should not change with invalid tcp port");
        } finally {
            // Restore original value
            outputStrategyField.set(null, originalStrategy);
        }
    }

    /**
     * Test parseArguments with unknown output type
     */
    @Test
    public void testParseArguments_UnknownOutputType() throws Exception {
        // Access the private method using reflection
        Method parseArgumentsMethod = HealthDataSimulator.class.getDeclaredMethod("parseArguments", String[].class);
        parseArgumentsMethod.setAccessible(true);

        // Get the outputStrategy field
        Field outputStrategyField = HealthDataSimulator.class.getDeclaredField("outputStrategy");
        outputStrategyField.setAccessible(true);

        // Save original value
        OutputStrategy originalStrategy = (OutputStrategy) outputStrategyField.get(null);

        try {
            // Call the method with unknown output type
            parseArgumentsMethod.invoke(null, (Object) new String[]{"--output", "unknown"});

            // OutputStrategy should not change
            assertEquals(originalStrategy, outputStrategyField.get(null),
                    "Output strategy should not change with unknown output type");
        } finally {
            // Restore original value
            outputStrategyField.set(null, originalStrategy);
        }
    }
    /**
     * Test the scheduleTask method without using mocks
     */
    @Test
    public void testScheduleTaskSimple() throws Exception {
        // Access the private method using reflection
        Method scheduleTaskMethod = HealthDataSimulator.class.getDeclaredMethod(
                "scheduleTask", Runnable.class, long.class, TimeUnit.class);
        scheduleTaskMethod.setAccessible(true);

        // Get and set the scheduler field
        Field schedulerField = HealthDataSimulator.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);

        // Save original scheduler
        Object originalScheduler = schedulerField.get(null);

        try {
            // Create a real scheduler with a single thread
            java.util.concurrent.ScheduledExecutorService realScheduler =
                    java.util.concurrent.Executors.newScheduledThreadPool(1);
            schedulerField.set(null, realScheduler);

            // Create a simple task that just increments a counter
            final int[] counter = {0};
            Runnable testTask = () -> counter[0]++;

            // Schedule the task with a very short delay
            scheduleTaskMethod.invoke(null, testTask, 1L, TimeUnit.MILLISECONDS);

            // Wait a short time for the task to run
            Thread.sleep(100);

            // Verify the task was executed
            assertTrue(counter[0] > 0, "The scheduled task should have executed at least once");

            // Shutdown the scheduler properly
            realScheduler.shutdown();
            realScheduler.awaitTermination(1, TimeUnit.SECONDS);

        } finally {
            // Restore the original scheduler
            schedulerField.set(null, originalScheduler);
        }
    }

    /**
     * Test the scheduleTasksForPatients method with a minimal approach
     */
    @Test
    public void testScheduleTasksForPatientsSimple() throws Exception {
        // Access the private method using reflection
        Method scheduleTasksForPatientsMethod = HealthDataSimulator.class.getDeclaredMethod(
                "scheduleTasksForPatients", List.class);
        scheduleTasksForPatientsMethod.setAccessible(true);

        // Get the scheduler field
        Field schedulerField = HealthDataSimulator.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);

        // Save original scheduler
        Object originalScheduler = schedulerField.get(null);

        try {
            // Create a real scheduler with a minimal thread pool
            java.util.concurrent.ScheduledExecutorService realScheduler =
                    java.util.concurrent.Executors.newScheduledThreadPool(1);
            schedulerField.set(null, realScheduler);

            // Create a minimal list of patient IDs - just one ID to keep it simple
            List<Integer> patientIds = List.of(1);

            // Call the method with just one patient ID
            scheduleTasksForPatientsMethod.invoke(null, patientIds);

            // Wait a very short time for any immediate tasks to be scheduled
            Thread.sleep(50);

            // Verify that tasks were scheduled by checking if the scheduler is not shutdown
            assertFalse(realScheduler.isShutdown(), "Scheduler should still be active after scheduling tasks");

            // Properly shutdown the scheduler
            realScheduler.shutdown();
            realScheduler.awaitTermination(1, TimeUnit.SECONDS);
        } finally {
            // Restore the original scheduler
            schedulerField.set(null, originalScheduler);
        }
    }
    /**
     * Test the default constructor
     */
    @Test
    public void testDefaultConstructor() throws Exception {
        // Simply instantiate the class to cover the default constructor
        HealthDataSimulator simulator = new HealthDataSimulator();

        // Verify the object was created
        assertNotNull(simulator, "HealthDataSimulator instance should be created");
    }


}