package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.*;

import com.data_management.*;
import com.alerts.*;

/**
 * Test class focused on blood pressure threshold alerts
 */
public class BloodPressureThresholdTest {

    private DataStorage dataStorage;
    private TestAlertFactoryManager alertManager;

    @BeforeEach
    public void setUp() {
        // Get the singleton instance of DataStorage
        dataStorage = DataStorage.getInstance();

        // Initialize alert manager
        alertManager = new TestAlertFactoryManager(dataStorage);
    }

    /**
     * Test that blood pressure above threshold triggers an alert
     */
    @Test
    public void testBloodPressureAboveThreshold() {
        long now = System.currentTimeMillis();
        int patientId = 8001;

        // Add a blood pressure reading above the threshold (> 180)
        dataStorage.addPatientData(patientId, 181.0, "SystolicPressure", now);

        Patient patient = dataStorage.getPatient(patientId);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(patient);

        // Check for alerts
        int pressureAlerts = alertManager.getPressureAlertFactory().getCapturedAlerts().size();

        System.out.println("DEBUG - Blood Pressure Above Threshold Test:");
        System.out.println("  - Systolic pressure: 181.0 mmHg (above 180 threshold)");
        System.out.println("  - Alerts generated: " + pressureAlerts);

        // Print alert details
        List<Alert> alerts = alertManager.getPressureAlertFactory().getCapturedAlerts();
        for (Alert alert : alerts) {
            System.out.println("  - Alert: " + alert.getCondition());
        }

        assertEquals(1, pressureAlerts, "Blood pressure above threshold should trigger 1 alert");
    }

    /**
     * Test that blood pressure exactly at threshold does NOT trigger an alert
     */
    @Test
    public void testBloodPressureAtThreshold() {
        long now = System.currentTimeMillis();
        int patientId = 8002;

        // Add a blood pressure reading exactly at the threshold (= 180)
        dataStorage.addPatientData(patientId, 180.0, "SystolicPressure", now);

        Patient patient = dataStorage.getPatient(patientId);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(patient);

        // Check for alerts
        int pressureAlerts = alertManager.getPressureAlertFactory().getCapturedAlerts().size();

        System.out.println("DEBUG - Blood Pressure At Threshold Test:");
        System.out.println("  - Systolic pressure: 180.0 mmHg (exactly at 180 threshold)");
        System.out.println("  - Alerts generated: " + pressureAlerts);

        assertEquals(0, pressureAlerts, "Blood pressure exactly at threshold should NOT trigger an alert");
    }

    /**
     * Main method for running the test from command line
     */
    public static void main(String[] args) {
        BloodPressureThresholdTest test = new BloodPressureThresholdTest();
        try {
            System.out.println("Setting up test environment...");
            test.setUp();

            System.out.println("\nTesting blood pressure above threshold...");
            test.testBloodPressureAboveThreshold();

            System.out.println("\nTesting blood pressure at threshold...");
            test.testBloodPressureAtThreshold();

            System.out.println("\nAll tests completed. Check output for results.");
        } catch (Exception e) {
            System.err.println("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
}