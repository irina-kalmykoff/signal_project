package test;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import com.data_management.*;
import com.alerts.*;

/**
 * Diagnostic test to investigate why regular tests are failing
 */
public class DiagnosticTest {

    /**
     * Reset DataStorage singleton between tests
     */
    @BeforeEach
    public void resetDataStorage() {
        try {
            // Use reflection to reset the singleton
            Field instance = DataStorage.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);

            System.out.println("DataStorage singleton reset before test");
        } catch (Exception e) {
            System.err.println("Failed to reset DataStorage singleton: " + e.getMessage());
        }
    }

    /**
     * Check state of DataStorage at start of test
     */
    @Test
    public void checkInitialState() {
        // Get singleton instance
        DataStorage dataStorage = DataStorage.getInstance();

        // Check if it already has data
        List<Patient> patients = dataStorage.getAllPatients();

        System.out.println("Initial state:");
        System.out.println("  - Number of patients: " + patients.size());

        // Print details of each patient
        for (Patient patient : patients) {
            List<PatientRecord> records = patient.getRecords(0, Long.MAX_VALUE);
            System.out.println("  - Patient " + records.get(0).getPatientId() + " has " + records.size() + " records");
        }

        // Verify instance is clean
        Assertions.assertEquals(0, patients.size(), "DataStorage should be empty at start of test");
    }

    /**
     * Try to run a simplified version of a failing test
     */
    @Test
    public void simplifiedBloodPressureThresholdTest() {
        // Get singleton instance
        DataStorage dataStorage = DataStorage.getInstance();

        // Create test alert factory manager
        TestAlertFactoryManager alertManager = new TestAlertFactoryManager(dataStorage);

        // Add test data
        int patientId = 9001;
        long now = System.currentTimeMillis();

        // Add blood pressure above threshold
        dataStorage.addPatientData(patientId, 181.0, "SystolicPressure", now);

        // Get patient and check alerts
        Patient patient = dataStorage.getPatient(patientId);
        alertManager.checkAllAlerts(patient);

        // Check for alerts
        int alerts = alertManager.getPressureAlertFactory().getCapturedAlerts().size();

        System.out.println("Blood Pressure Threshold Test:");
        System.out.println("  - Systolic pressure: 181.0 mmHg (above 180 threshold)");
        System.out.println("  - Alerts generated: " + alerts);

        // This should pass based on our standalone test
        Assertions.assertEquals(1, alerts, "Blood pressure above threshold should trigger alert");
    }

    /**
     * Examine actual failing test class
     */
    @Test
    public void examineAlertGeneratorUnitTests() {
        try {
            // Try to create an instance of the failing test class
            Class<?> testClass = Class.forName("test.AlertGeneratorUnitTests");
            Object testInstance = testClass.getDeclaredConstructor().newInstance();

            // Print its fields
            System.out.println("Fields in AlertGeneratorUnitTests:");
            for (Field field : testClass.getDeclaredFields()) {
                field.setAccessible(true);
                System.out.println("  - " + field.getName() + ": " + field.getType().getName());
                try {
                    Object value = field.get(testInstance);
                    System.out.println("    Value: " + (value == null ? "null" : value.toString()));
                } catch (Exception e) {
                    System.out.println("    Could not access value: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to examine AlertGeneratorUnitTests: " + e.getMessage());
        }
    }

    /**
     * Main method for running tests directly
     */
    public static void main(String[] args) {
        DiagnosticTest test = new DiagnosticTest();
        try {
            System.out.println("Running diagnostic tests...");

            test.resetDataStorage();
            test.checkInitialState();

            test.resetDataStorage();
            test.simplifiedBloodPressureThresholdTest();

            test.resetDataStorage();
            test.examineAlertGeneratorUnitTests();

            System.out.println("Diagnostic tests completed.");
        } catch (Exception e) {
            System.err.println("Error running diagnostic tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
}