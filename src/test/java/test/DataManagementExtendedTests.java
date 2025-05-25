package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;

import com.data_management.*;

/**
 * Extended tests for the com.data_management package
 * to improve code coverage.
 */
public class DataManagementExtendedTests {

    private DataStorage dataStorage;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        // Reset singleton before each test
        resetDataStorage();
        dataStorage = DataStorage.getInstance();
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

    @Test
    public void testPatientRecordConstructorAndGetters() {
        // Test PatientRecord constructor and getters
        int patientId = 123;
        double measurementValue = 98.6;
        String recordType = "Temperature";
        long timestamp = System.currentTimeMillis();

        PatientRecord record = new PatientRecord(patientId, measurementValue, recordType, timestamp);

        assertEquals(patientId, record.getPatientId());
        assertEquals(measurementValue, record.getMeasurementValue());
        assertEquals(recordType, record.getRecordType());
        assertEquals(timestamp, record.getTimestamp());
    }

    @Test
    public void testDataStorageAddPatientData() {
        // Test adding data for a new patient
        dataStorage.addPatientData(1, 120, "SystolicPressure", 1000);

        // Get the patient
        Patient patient = dataStorage.getPatient(1);
        assertNotNull(patient);

        // Verify records
        List<PatientRecord> records = patient.getRecords(0, 2000);
        assertEquals(1, records.size());
        assertEquals(120, records.get(0).getMeasurementValue());
        assertEquals("SystolicPressure", records.get(0).getRecordType());
        assertEquals(1000, records.get(0).getTimestamp());

        // Test adding more data for the same patient
        dataStorage.addPatientData(1, 80, "DiastolicPressure", 1001);

        // Verify updated records
        records = patient.getRecords(0, 2000);
        assertEquals(2, records.size());
    }

    @Test
    public void testDataStorageGetRecords() {
        // Add data for a patient
        dataStorage.addPatientData(1, 120, "SystolicPressure", 1000);
        dataStorage.addPatientData(1, 125, "SystolicPressure", 2000);
        dataStorage.addPatientData(1, 130, "SystolicPressure", 3000);

        // Test getRecords with time range
        List<PatientRecord> records = dataStorage.getRecords(1, 1500, 2500);
        assertEquals(1, records.size());
        assertEquals(125, records.get(0).getMeasurementValue());
        assertEquals(2000, records.get(0).getTimestamp());

        // Test with non-existent patient
        records = dataStorage.getRecords(999, 0, 5000);
        assertTrue(records.isEmpty());
    }

    @Test
    public void testDataStorageGetAllPatients() {
        // Add multiple patients
        dataStorage.addPatientData(1, 120, "SystolicPressure", 1000);
        dataStorage.addPatientData(2, 130, "SystolicPressure", 1000);
        dataStorage.addPatientData(3, 140, "SystolicPressure", 1000);

        // Get all patients
        List<Patient> patients = dataStorage.getAllPatients();
        assertEquals(3, patients.size());

        // Verify each patient has their correct data
        for (Patient patient : patients) {
            List<PatientRecord> records = patient.getRecords(0, 2000);
            assertEquals(1, records.size());

            int patientId = records.get(0).getPatientId();
            double expectedValue = 110 + (patientId * 10); // 120, 130, 140
            assertEquals(expectedValue, records.get(0).getMeasurementValue());
        }
    }

    @Test
    public void testPatientAddRecord() {
        // Create a patient
        Patient patient = new Patient(1);

        // Add records directly
        patient.addRecord(120, "SystolicPressure", 1000);
        patient.addRecord(80, "DiastolicPressure", 1001);
        patient.addRecord(75, "HeartRate", 1002);

        // Verify records
        List<PatientRecord> records = patient.getRecords(0, 2000);
        assertEquals(3, records.size());

        // Verify record values
        boolean foundSystolic = false;
        boolean foundDiastolic = false;
        boolean foundHeartRate = false;

        for (PatientRecord record : records) {
            if ("SystolicPressure".equals(record.getRecordType())) {
                assertEquals(120, record.getMeasurementValue());
                foundSystolic = true;
            } else if ("DiastolicPressure".equals(record.getRecordType())) {
                assertEquals(80, record.getMeasurementValue());
                foundDiastolic = true;
            } else if ("HeartRate".equals(record.getRecordType())) {
                assertEquals(75, record.getMeasurementValue());
                foundHeartRate = true;
            }
        }

        assertTrue(foundSystolic);
        assertTrue(foundDiastolic);
        assertTrue(foundHeartRate);
    }

    @Test
    public void testPatientGetRecordsTimeFiltering() {
        // Create a patient
        Patient patient = new Patient(1);

        // Add records with different timestamps
        patient.addRecord(120, "SystolicPressure", 1000);
        patient.addRecord(125, "SystolicPressure", 2000);
        patient.addRecord(130, "SystolicPressure", 3000);

        // Test time filtering

        // Get all records
        List<PatientRecord> allRecords = patient.getRecords(0, 4000);
        assertEquals(3, allRecords.size());

        // Get records in middle time range
        List<PatientRecord> middleRecords = patient.getRecords(1500, 2500);
        assertEquals(1, middleRecords.size());
        assertEquals(125, middleRecords.get(0).getMeasurementValue());

        // Get records from start to middle
        List<PatientRecord> startRecords = patient.getRecords(0, 1500);
        assertEquals(1, startRecords.size());
        assertEquals(120, startRecords.get(0).getMeasurementValue());

        // Get records from middle to end
        List<PatientRecord> endRecords = patient.getRecords(2500, 4000);
        assertEquals(1, endRecords.size());
        assertEquals(130, endRecords.get(0).getMeasurementValue());

        // Test with no records in range
        List<PatientRecord> noRecords = patient.getRecords(5000, 6000);
        assertTrue(noRecords.isEmpty());
    }

    @Test
    public void testFileDataReader() throws IOException {
        // Create test files in the temp directory
        createTestDataFile(tempDir, "patient1.txt",
                "1,1000,HeartRate,75",
                "1,1001,SystolicPressure,120",
                "1,1002,DiastolicPressure,80");

        createTestDataFile(tempDir, "patient2.txt",
                "2,1000,HeartRate,72",
                "2,1001,SystolicPressure,130",
                "2,1002,DiastolicPressure,85");

        // Create FileDataReader with temp directory
        FileDataReader reader = new FileDataReader(tempDir.toString());

        // Create data storage
        DataStorage storage = DataStorage.getInstance();
        // Read data
        reader.readData(storage);

        // Verify patients were created
        List<Patient> patients = storage.getAllPatients();
        assertEquals(2, patients.size());

        // Verify patient 1 data
        List<PatientRecord> patient1Records = storage.getRecords(1, 0, 2000);
        assertEquals(3, patient1Records.size());

        // Verify patient 2 data
        List<PatientRecord> patient2Records = storage.getRecords(2, 0, 2000);
        assertEquals(3, patient2Records.size());
    }

    @Test
    public void testFileDataReaderErrorHandling() throws IOException {
        // Create test files with invalid data
        createTestDataFile(tempDir, "invalid1.txt",
                "not_a_number,1000,HeartRate,75", // Invalid patient ID
                "1,1001,SystolicPressure,120");

        createTestDataFile(tempDir, "invalid2.txt",
                "1,not_a_number,HeartRate,75", // Invalid timestamp
                "1,1001,SystolicPressure,120");

        createTestDataFile(tempDir, "invalid3.txt",
                "1,1000,HeartRate", // Too few fields
                "1,1001,SystolicPressure,120");

        // Create FileDataReader with temp directory
        FileDataReader reader = new FileDataReader(tempDir.toString());

        // Create data storage
        DataStorage storage = DataStorage.getInstance();
        // Read data - should not throw
        assertDoesNotThrow(() -> reader.readData(storage));

        // Should still read the valid records
        List<Patient> patients = storage.getAllPatients();
        assertTrue(patients.size() > 0);
    }

    @Test
    public void testFileDataReaderNonExistentDirectory() {
        // Create FileDataReader with non-existent directory
        FileDataReader reader = new FileDataReader("/non/existent/directory");

        // Create data storage
        DataStorage storage = DataStorage.getInstance();

        // Read data - should throw IOException
        assertThrows(IOException.class, () -> reader.readData(storage));
    }

    @Test
    public void testDataStorageMainMethod() {
        // Just call the main method to increase coverage
        assertDoesNotThrow(() -> DataStorage.main(new String[0]));
    }

    /**
     * Helper method to create a test data file
     */
    private void createTestDataFile(Path directory, String filename, String... lines) throws IOException {
        Path filePath = directory.resolve(filename);
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath))) {
            for (String line : lines) {
                writer.println(line);
            }
        }
    }
}