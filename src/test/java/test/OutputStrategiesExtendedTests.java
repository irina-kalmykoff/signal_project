package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
//import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;

import com.cardio_generator.outputs.*;

/**
 * Extends testing coverage for the output strategies in com.cardio_generator.outputs package.
 */
public class OutputStrategiesExtendedTests {

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

    @Test
    public void testConsoleOutputMultipleWrites() {
        ConsoleOutputStrategy strategy = new ConsoleOutputStrategy();

        // Test with different data types
        strategy.output(1, 1000, "HeartRate", "75");
        strategy.output(1, 1001, "BloodPressure", "120/80");
        strategy.output(1, 1002, "Saturation", "98%");
        strategy.output(1, 1003, "ECG", "0.5");

        // Check output contains all writes
        String output = outContent.toString();
        assertTrue(output.contains("HeartRate"));
        assertTrue(output.contains("BloodPressure"));
        assertTrue(output.contains("Saturation"));
        assertTrue(output.contains("ECG"));
    }

    @Test
    public void testFileOutputConcurrency() throws Exception {
        // Create file output strategy with temp dir
        FileOutputStrategy strategy = new FileOutputStrategy(tempDir.toString());

        // Set up concurrent threads to write to files
        final int threadCount = 10;
        final int writesPerThread = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Launch threads
        for (int i = 0; i < threadCount; i++) {
            final int patientId = i + 1;
            new Thread(() -> {
                try {
                    for (int j = 0; j < writesPerThread; j++) {
                        strategy.output(patientId, System.currentTimeMillis(),
                                "TestData", "Value" + j);
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Wait for all threads to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Verify file creation
        Path testDataFile = tempDir.resolve("TestData.txt");
        assertTrue(Files.exists(testDataFile));

        // Verify file contains data from all threads
        List<String> lines = Files.readAllLines(testDataFile);
        assertEquals(threadCount * writesPerThread, lines.size());

        // Verify file contains data from each patient
        for (int patientId = 1; patientId <= threadCount; patientId++) {
            final int id = patientId;
            assertTrue(lines.stream().anyMatch(l -> l.contains("Patient ID: " + id)));
        }
    }

    @Test
    public void testFileOutputFileMap() {
        FileOutputStrategy strategy = new FileOutputStrategy(tempDir.toString());

        // Generate multiple label types
        strategy.output(1, 1000, "Label1", "Data1");
        strategy.output(1, 1001, "Label2", "Data2");
        strategy.output(1, 1002, "Label3", "Data3");

        // Check file_map contents
        ConcurrentHashMap<String, String> fileMap = strategy.file_map;
        assertEquals(3, fileMap.size());
        assertTrue(fileMap.containsKey("Label1"));
        assertTrue(fileMap.containsKey("Label2"));
        assertTrue(fileMap.containsKey("Label3"));

        // Verify paths
        for (String label : fileMap.keySet()) {
            String path = fileMap.get(label);
            assertTrue(path.endsWith(label + ".txt"));
        }

        // Check that all expected files exist
        for (String label : fileMap.keySet()) {
            Path file = tempDir.resolve(label + ".txt");
            assertTrue(Files.exists(file));
        }
    }

    @Test
    public void testFileOutputDirectoryCreation() throws Exception {
        // Create nested directory structure
        Path nestedDir = tempDir.resolve("level1/level2/level3");

        // Create strategy with nested directory
        FileOutputStrategy strategy = new FileOutputStrategy(nestedDir.toString());

        // Output some data
        strategy.output(1, 1000, "Test", "TestData");

        // Verify directories were created
        assertTrue(Files.exists(nestedDir));

        // Verify file was created
        Path testFile = nestedDir.resolve("Test.txt");
        assertTrue(Files.exists(testFile));

        // Verify file contents
        List<String> lines = Files.readAllLines(testFile);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("TestData"));
    }

    @Test
    public void testFileOutputErrorRecovery() {
        // First create with invalid path
        FileOutputStrategy invalidStrategy = new FileOutputStrategy("/invalid/path");

        // This should not throw even though directory creation will fail
        assertDoesNotThrow(() -> invalidStrategy.output(1, 1000, "Test", "TestData"));

        // Now create with valid path
        FileOutputStrategy validStrategy = new FileOutputStrategy(tempDir.toString());

        // This should work
        assertDoesNotThrow(() -> validStrategy.output(1, 1000, "Test", "TestData"));

        // Verify file was created in the valid path
        Path testFile = tempDir.resolve("Test.txt");
        assertTrue(Files.exists(testFile));
    }

    @Test
    public void testTcpOutputStrategyCreation() {
        // Test that the strategy can be created with different ports
        assertDoesNotThrow(() -> {
            TcpOutputStrategy strategy1 = new TcpOutputStrategy(0); // Use any free port
            TcpOutputStrategy strategy2 = new TcpOutputStrategy(9999);

            // Verify output doesn't throw even with no clients
            strategy1.output(1, 1000, "Test", "TestData");
            strategy2.output(1, 1000, "Test", "TestData");
        });
    }

    @Test
    public void testConsoleOutputFormatting() {
        ConsoleOutputStrategy strategy = new ConsoleOutputStrategy();

        // Clear previous output
        outContent.reset();

        // Test with special characters
        strategy.output(1, 1000, "Special", "Data with spaces");
        strategy.output(2, 1001, "Unicode", "Éñçøðîñg");
        strategy.output(3, 1002, "Numbers", "123.456");

        // Check formatting in output
        String output = outContent.toString();
        assertTrue(output.contains("Data with spaces"));
        assertTrue(output.contains("Éñçøðîñg"));
        assertTrue(output.contains("Numbers"));
        assertTrue(output.contains("123.456"));
    }

    @Test
    public void testOutputStrategyComposition() {
        // Create a composed strategy that outputs to multiple strategies
        List<OutputStrategy> strategies = new ArrayList<>();

        // Create individual strategies
        ConsoleOutputStrategy consoleStrategy = new ConsoleOutputStrategy();
        FileOutputStrategy fileStrategy = new FileOutputStrategy(tempDir.toString());

        // Add to list
        strategies.add(consoleStrategy);
        strategies.add(fileStrategy);

        // Create composite strategy
        OutputStrategy compositeStrategy = new OutputStrategy() {
            @Override
            public void output(int patientId, long timestamp, String label, String data) {
                for (OutputStrategy strategy : strategies) {
                    strategy.output(patientId, timestamp, label, data);
                }
            }
        };

        // Test composite strategy
        compositeStrategy.output(1, 1000, "Composite", "TestData");

        // Verify console output
        assertTrue(outContent.toString().contains("Composite"));
        assertTrue(outContent.toString().contains("TestData"));

        // Verify file output
        Path testFile = tempDir.resolve("Composite.txt");
        assertTrue(Files.exists(testFile));
    }

    @Test
    public void testFileOutputLargeVolume() throws Exception {
        // Test with large volume of data
        FileOutputStrategy strategy = new FileOutputStrategy(tempDir.toString());

        // Write many records
        final int recordCount = 1000;
        for (int i = 0; i < recordCount; i++) {
            strategy.output(1, i, "LargeVolume", "Data" + i);
        }

        // Verify file exists
        Path testFile = tempDir.resolve("LargeVolume.txt");
        assertTrue(Files.exists(testFile));

        // Verify record count
        List<String> lines = Files.readAllLines(testFile);
        assertEquals(recordCount, lines.size());
    }
}