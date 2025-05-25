package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

import com.data_management.*;

/**
 * Tests specifically targeting the processFile method in FileDataReader
 */
public class FileDataReaderProcessFileTests {

    private DataStorage dataStorage;
    private FileDataReader reader;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        // Reset singleton before each test
        resetDataStorage();
        dataStorage = DataStorage.getInstance();
        reader = new FileDataReader(tempDir.toString());
    }
    
    @AfterEach
    public void tearDown() {
        // Reset singleton after each test
        resetDataStorage();
    }
    
    /**
     * Reset DataStorage singleton between tests
     */
    private void resetDataStorage() {
        try {
            Field instance = DataStorage.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
            System.out.println("DataStorage singleton reset successfully");
        } catch (Exception e) {
            System.err.println("Failed to reset DataStorage singleton: " + e.getMessage());
        }
    }

    /**
     * Test the standard data format handling
     */
    @Test
    public void testStandardDataFormat() throws Exception {
        // Create a file with standard format data
        Path testFile = createTestDataFile("standard.txt",
                "1,1000,HeartRate,75",
                "2,2000,SystolicPressure,120",
                "3,3000,DiastolicPressure,80");

        // Call processFile directly using reflection
        Method processFileMethod = FileDataReader.class.getDeclaredMethod(
                "processFile", Path.class, DataStorage.class);
        processFileMethod.setAccessible(true);
        processFileMethod.invoke(reader, testFile, dataStorage);

        // Verify all data was processed correctly
        assertEquals(75, dataStorage.getRecords(1, 0, 2000).get(0).getMeasurementValue());
        assertEquals(120, dataStorage.getRecords(2, 0, 3000).get(0).getMeasurementValue());
        assertEquals(80, dataStorage.getRecords(3, 0, 4000).get(0).getMeasurementValue());
    }

    /**
     * Test handling of "Alert" type data with numeric values
     */
    @Test
    public void testAlertTypeWithNumericValue() throws Exception {
        // Create a file with Alert type using numeric values
        Path testFile = createTestDataFile("alert_numeric.txt",
                "1,1000,Alert,1.0",
                "2,2000,Alert,0.0");

        // Call processFile directly
        Method processFileMethod = FileDataReader.class.getDeclaredMethod(
                "processFile", Path.class, DataStorage.class);
        processFileMethod.setAccessible(true);
        processFileMethod.invoke(reader, testFile, dataStorage);

        // Verify Alert data was processed correctly
        assertEquals(1.0, dataStorage.getRecords(1, 0, 2000).get(0).getMeasurementValue());
        assertEquals(0.0, dataStorage.getRecords(2, 0, 3000).get(0).getMeasurementValue());
    }

    /**
     * Test handling of "Alert" type data with string values
     */
    @Test
    public void testAlertTypeWithStringValue() throws Exception {
        // Create a file with Alert type using string values
        Path testFile = createTestDataFile("alert_string.txt",
                "1,1000,Alert,triggered",
                "2,2000,Alert,resolved");

        // Call processFile directly
        Method processFileMethod = FileDataReader.class.getDeclaredMethod(
                "processFile", Path.class, DataStorage.class);
        processFileMethod.setAccessible(true);
        processFileMethod.invoke(reader, testFile, dataStorage);

        // Verify Alert data was converted correctly
        assertEquals(1.0, dataStorage.getRecords(1, 0, 2000).get(0).getMeasurementValue());
        assertEquals(0.0, dataStorage.getRecords(2, 0, 3000).get(0).getMeasurementValue());
    }

    /**
     * Test handling of non-numeric values for normal record types
     */
    @Test
    public void testNonNumericValueForNormalType() throws Exception {
        // Create a file with non-numeric values for normal types
        Path testFile = createTestDataFile("non_numeric.txt",
                "1,1000,HeartRate,seventy-five");

        // Call processFile directly
        Method processFileMethod = FileDataReader.class.getDeclaredMethod(
                "processFile", Path.class, DataStorage.class);
        processFileMethod.setAccessible(true);
        processFileMethod.invoke(reader, testFile, dataStorage);

        // Verify the invalid record was skipped
        assertTrue(dataStorage.getRecords(1, 0, 2000).isEmpty());
    }

    /**
     * Test handling of invalid data formats
     */
    @Test
    public void testInvalidDataFormat() throws Exception {
        // Create a file with invalid data formats
        Path testFile = createTestDataFile("invalid_format.txt",
                "1,1000,HeartRate", // Missing value
                "1,1000,HeartRate,75,extra" // Extra field
        );

        // Call processFile directly
        Method processFileMethod = FileDataReader.class.getDeclaredMethod(
                "processFile", Path.class, DataStorage.class);
        processFileMethod.setAccessible(true);
        processFileMethod.invoke(reader, testFile, dataStorage);

        // Verify the first record was skipped (too few fields)
        // But the second record should be processed (ignoring extra fields)
        assertEquals(1, dataStorage.getRecords(1, 0, 2000).size());
        assertEquals(75, dataStorage.getRecords(1, 0, 2000).get(0).getMeasurementValue());
    }

    /**
     * Test handling of invalid numeric values
     */
    @Test
    public void testInvalidNumericValues() throws Exception {
        // Create a file with invalid numeric values
        Path testFile = createTestDataFile("invalid_numeric.txt",
                "abc,1000,HeartRate,75", // Invalid patient ID
                "1,xyz,HeartRate,75" // Invalid timestamp
        );

        // Call processFile directly
        Method processFileMethod = FileDataReader.class.getDeclaredMethod(
                "processFile", Path.class, DataStorage.class);
        processFileMethod.setAccessible(true);
        processFileMethod.invoke(reader, testFile, dataStorage);

        // Verify both records were skipped due to invalid numeric values
        assertEquals(0, dataStorage.getAllPatients().size());
    }

    /**
     * Test with a mixture of valid and invalid records
     */
    @Test
    public void testMixedValidAndInvalidRecords() throws Exception {
        // Create a file with a mix of valid and invalid records
        Path testFile = createTestDataFile("mixed.txt",
                "1,1000,HeartRate,75", // Valid
                "abc,1000,HeartRate,75", // Invalid patient ID
                "2,2000,SystolicPressure,120", // Valid
                "3,3000,HeartRate,not-a-number", // Invalid value
                "4,4000,Alert,triggered" // Valid
        );

        // Call processFile directly
        Method processFileMethod = FileDataReader.class.getDeclaredMethod(
                "processFile", Path.class, DataStorage.class);
        processFileMethod.setAccessible(true);
        processFileMethod.invoke(reader, testFile, dataStorage);

        // Verify valid records were processed
        assertEquals(75, dataStorage.getRecords(1, 0, 2000).get(0).getMeasurementValue());
        assertEquals(120, dataStorage.getRecords(2, 0, 3000).get(0).getMeasurementValue());
        assertEquals(1.0, dataStorage.getRecords(4, 0, 5000).get(0).getMeasurementValue());

        // Verify patient 3 has no valid records
        assertTrue(dataStorage.getRecords(3, 0, 4000).isEmpty());
    }

    /**
     * Test handling of empty file
     */
    @Test
    public void testEmptyFile() throws Exception {
        // Create an empty file
        Path testFile = createTestDataFile("empty.txt");

        // Call processFile directly
        Method processFileMethod = FileDataReader.class.getDeclaredMethod(
                "processFile", Path.class, DataStorage.class);
        processFileMethod.setAccessible(true);
        processFileMethod.invoke(reader, testFile, dataStorage);

        // Verify no records were added
        assertEquals(0, dataStorage.getAllPatients().size());
    }

    /**
     * Test handling of Alert type with custom string
     */
    @Test
    public void testAlertTypeWithCustomString() throws Exception {
        // Create a file with Alert type using custom string values
        Path testFile = createTestDataFile("alert_custom.txt",
                "1,1000,Alert,active", // Not "triggered" but should convert to 1.0
                "2,2000,Alert,inactive" // Not "resolved" but should convert to 0.0
        );

        // Call processFile directly
        Method processFileMethod = FileDataReader.class.getDeclaredMethod(
                "processFile", Path.class, DataStorage.class);
        processFileMethod.setAccessible(true);
        processFileMethod.invoke(reader, testFile, dataStorage);

        // Verify Alert data was converted correctly based on the code logic
        // According to the implementation, anything not exactly "triggered" would be 0.0
        assertEquals(0.0, dataStorage.getRecords(1, 0, 2000).get(0).getMeasurementValue());
        assertEquals(0.0, dataStorage.getRecords(2, 0, 3000).get(0).getMeasurementValue());
    }

    /**
     * Helper method to create a test data file
     */
    private Path createTestDataFile(String filename, String... lines) throws IOException {
        Path filePath = tempDir.resolve(filename);
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath))) {
            for (String line : lines) {
                writer.println(line);
            }
        }
        return filePath;
    }
}