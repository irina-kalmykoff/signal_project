package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import com.data_management.*;
import com.alerts.*;
import com.strategy.BloodPressureStrategy;

/**
 * Test class focused specifically on blood pressure trend detection
 */
public class BloodPressureTrendTest {
    
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
     * Test increasing blood pressure trend detection with exact values
     * This test creates three readings with exactly the conditions from TestDataGenerator
     */
    @Test
    public void testIncreasingBloodPressureTrend() {
        long now = System.currentTimeMillis();
        
        // Test case: Increasing trend exactly 12 mmHg (should trigger alert)
        int patientId = 6001;
        dataStorage.addPatientData(patientId, 120.0, "SystolicPressure", now - 3000); // Base reading
        dataStorage.addPatientData(patientId, 132.0, "SystolicPressure", now - 2000); // +12 mmHg
        dataStorage.addPatientData(patientId, 144.0, "SystolicPressure", now - 1000); // +12 mmHg
        
        Patient patient = dataStorage.getPatient(patientId);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(patient);
        
        // Detailed debug output
        System.out.println("DEBUG - Increasing BP Trend Test:");
        System.out.println("  - First reading: 120.0 mmHg at " + (now - 3000));
        System.out.println("  - Second reading: 132.0 mmHg at " + (now - 2000) + " (diff: +12.0)");
        System.out.println("  - Third reading: 144.0 mmHg at " + (now - 1000) + " (diff: +12.0)");
        System.out.println("  - Alerts generated: " + alertManager.getPressureAlertFactory().getCapturedAlerts().size());
        
        // Print full details of any captured alerts
        List<Alert> alerts = alertManager.getPressureAlertFactory().getCapturedAlerts();
        for (Alert alert : alerts) {
            System.out.println("  - Alert: " + alert.getCondition() + " (Patient: " + alert.getPatientId() + ")");
        }
        
        // Check if BP trend alert was captured
        boolean foundTrendAlert = alertManager.containsAlertWithKeyword("Increasing") &&
                                 alertManager.containsAlertWithKeyword("Trend");
        
        assertTrue(foundTrendAlert, "Should detect increasing blood pressure trend when differences are > 10 mmHg");
    }
    
    /**
     * Test decreasing blood pressure trend detection with exact values
     */
    @Test
    public void testDecreasingBloodPressureTrend() {
        long now = System.currentTimeMillis();
        
        // Test case: Decreasing trend exactly 12 mmHg (should trigger alert)
        int patientId = 6002;
        dataStorage.addPatientData(patientId, 150.0, "SystolicPressure", now - 3000); // Base reading
        dataStorage.addPatientData(patientId, 138.0, "SystolicPressure", now - 2000); // -12 mmHg
        dataStorage.addPatientData(patientId, 126.0, "SystolicPressure", now - 1000); // -12 mmHg
        
        Patient patient = dataStorage.getPatient(patientId);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(patient);
        
        // Detailed debug output
        System.out.println("DEBUG - Decreasing BP Trend Test:");
        System.out.println("  - First reading: 150.0 mmHg at " + (now - 3000));
        System.out.println("  - Second reading: 138.0 mmHg at " + (now - 2000) + " (diff: -12.0)");
        System.out.println("  - Third reading: 126.0 mmHg at " + (now - 1000) + " (diff: -12.0)");
        System.out.println("  - Alerts generated: " + alertManager.getPressureAlertFactory().getCapturedAlerts().size());
        
        // Print full details of any captured alerts
        List<Alert> alerts = alertManager.getPressureAlertFactory().getCapturedAlerts();
        for (Alert alert : alerts) {
            System.out.println("  - Alert: " + alert.getCondition() + " (Patient: " + alert.getPatientId() + ")");
        }
        
        // Check if BP trend alert was captured
        boolean foundTrendAlert = alertManager.containsAlertWithKeyword("Decreasing") &&
                                 alertManager.containsAlertWithKeyword("Trend");
        
        assertTrue(foundTrendAlert, "Should detect decreasing blood pressure trend when differences are > 10 mmHg");
    }
    
    /**
     * Examine the BloodPressureStrategy logic directly
     */
    @Test
    public void testBloodPressureStrategyLogic() {
        // Create a strategy to test directly
        BloodPressureStrategy strategy = new BloodPressureStrategy();
        
        // Create sample data for increasing trend
        List<PatientRecord> increasingRecords = new ArrayList<>();
        
        // Using the correct constructor order: (patientId, measurementValue, recordType, timestamp)
        increasingRecords.add(new PatientRecord(9999, 120.0, "SystolicPressure", System.currentTimeMillis() - 3000));
        increasingRecords.add(new PatientRecord(9999, 132.0, "SystolicPressure", System.currentTimeMillis() - 2000));
        increasingRecords.add(new PatientRecord(9999, 144.0, "SystolicPressure", System.currentTimeMillis() - 1000));
        
        // Call the strategy directly and see what it returns
        Alert increasingAlert = strategy.checkAlert(null, increasingRecords);
        
        System.out.println("DEBUG - Direct strategy test (increasing):");
        System.out.println("  - Alert returned: " + (increasingAlert != null ? "YES" : "NO"));
        if (increasingAlert != null) {
            System.out.println("  - Alert condition: " + increasingAlert.getCondition());
        }
        
        // Create sample data for decreasing trend
        List<PatientRecord> decreasingRecords = new ArrayList<>();
        
        // Using the correct constructor order
        decreasingRecords.add(new PatientRecord(9998, 150.0, "SystolicPressure", System.currentTimeMillis() - 3000));
        decreasingRecords.add(new PatientRecord(9998, 138.0, "SystolicPressure", System.currentTimeMillis() - 2000));
        decreasingRecords.add(new PatientRecord(9998, 126.0, "SystolicPressure", System.currentTimeMillis() - 1000));
        
        // Call the strategy directly and see what it returns
        Alert decreasingAlert = strategy.checkAlert(null, decreasingRecords);
        
        System.out.println("DEBUG - Direct strategy test (decreasing):");
        System.out.println("  - Alert returned: " + (decreasingAlert != null ? "YES" : "NO"));
        if (decreasingAlert != null) {
            System.out.println("  - Alert condition: " + decreasingAlert.getCondition());
        }
    }
    
    /**
     * Main method for running the test from command line
     */
    public static void main(String[] args) {
        BloodPressureTrendTest test = new BloodPressureTrendTest();
        try {
            System.out.println("Setting up test environment...");
            test.setUp();
            
            System.out.println("\nTesting increasing blood pressure trend...");
            test.testIncreasingBloodPressureTrend();
            
            System.out.println("\nTesting decreasing blood pressure trend...");
            test.testDecreasingBloodPressureTrend();
            
            System.out.println("\nTesting blood pressure strategy directly...");
            test.testBloodPressureStrategyLogic();
            
            System.out.println("\nAll tests completed. Check output for results.");
        } catch (Exception e) {
            System.err.println("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
}