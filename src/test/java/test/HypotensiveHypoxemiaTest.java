package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import com.data_management.*;
import com.alerts.*;

/**
 * Test class focused on hypotensive hypoxemia detection
 */
public class HypotensiveHypoxemiaTest {

    private DataStorage dataStorage;
    private TestAlertFactoryManager alertManager;
    private static final String TEST_DATA_DIR = "test_data";

    @BeforeEach
    public void setUp() throws IOException {
        // Create test data directory if it doesn't exist
        Path directoryPath = Paths.get(TEST_DATA_DIR);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }

        // Get the singleton instance of DataStorage
        dataStorage = DataStorage.getInstance();

        // Initialize alert manager
        alertManager = new TestAlertFactoryManager(dataStorage);
    }

    /**
     * Test hypotensive hypoxemia detection (low BP and low O2 together)
     */
    @Test
    public void testHypotensiveHypoxemia() {
        long now = System.currentTimeMillis();
        int patientId = 5001;

        // Generate data with both low BP and low O2 at the same time
        for (int i = 0; i < 10; i++) {
            long timestamp = now - 5000 + i * 500; // 5 seconds of data

            // Low blood pressure (systolic < 90)
            dataStorage.addPatientData(patientId, 88.0, "SystolicPressure", timestamp);

            // Low oxygen saturation (< 92%)
            dataStorage.addPatientData(patientId, 91.0, "Saturation", timestamp);
        }

        Patient patient = dataStorage.getPatient(patientId);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(patient);

        // Check specifically for the hypoxemia alert by name
        boolean foundHypotensiveHypoxemiaAlert = alertManager.containsAlertWithKeyword("Hypotensive Hypoxemia");

        // Detailed debug output
        // System.out.println("DEBUG - Hypotensive Hypoxemia Test (Standalone):");
        // System.out.println("  - Patient records: " + patient.getRecords(0, Long.MAX_VALUE).size());
        // System.out.println("  - Hypotensive Hypoxemia alert found: " + foundHypotensiveHypoxemiaAlert);
        // System.out.println("  - Oxygen alerts: " + alertManager.getOxygenAlertFactory().getCapturedAlerts().size());
        // System.out.println("  - Pressure alerts: " + alertManager.getPressureAlertFactory().getCapturedAlerts().size());

        // Print all captured alerts for debugging
        List<Alert> allAlerts = alertManager.getAllAlerts();
        for (Alert alert : allAlerts) {
            System.out.println("  - Alert: " + alert.getCondition());
        }

        assertTrue(foundHypotensiveHypoxemiaAlert,
                "Should detect hypotensive hypoxemia condition when both low BP and low O2 occur together");
    }

    /**
     * Main method for running the test from command line
     */
    public static void main(String[] args) {
        HypotensiveHypoxemiaTest test = new HypotensiveHypoxemiaTest();
        try {
            System.out.println("Setting up test environment...");
            test.setUp();

            System.out.println("\nRunning hypotensive hypoxemia test...");
            test.testHypotensiveHypoxemia();

            System.out.println("\nTest completed. Check output for results.");
        } catch (Exception e) {
            System.err.println("Error running test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}