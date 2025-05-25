package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import com.data_management.*;
import com.alerts.*;
import com.strategy.HeartRateStrategy;

/**
 * Test class focused specifically on ECG abnormality detection
 */
public class ECGAbnormalityTest {
    
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
     * Test ECG abnormality detection with extreme values
     */
    @Test
    public void testECGAbnormality() {
        long now = System.currentTimeMillis();
        int patientId = 7001;
        
        // Add many normal ECG readings to establish baseline
        for (int i = 0; i < 50; i++) {
            dataStorage.addPatientData(patientId, 0.2, "ECG", now - (60 - i) * 1000);
        }
        
        // Add EXTREMELY abnormal ECG readings
        for (int i = 0; i < 10; i++) {
            dataStorage.addPatientData(patientId, 5.0, "ECG", now - 10000 + i * 1000); // Very high (25x normal)
        }
        
        Patient patient = dataStorage.getPatient(patientId);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(patient);
        
        // Detailed debug output
        System.out.println("DEBUG - ECG Abnormality Test:");
        System.out.println("  - 50 normal readings of 0.2");
        System.out.println("  - 10 abnormal readings of 5.0 (25x normal value)");
        System.out.println("  - Alerts generated: " + alertManager.getEcgAlertFactory().getCapturedAlerts().size());
        
        // Print details of alerts
        List<Alert> alerts = alertManager.getEcgAlertFactory().getCapturedAlerts();
        for (Alert alert : alerts) {
            System.out.println("  - Alert: " + alert.getCondition() + " (Patient: " + alert.getPatientId() + ")");
        }
        
        // Check if ECG abnormality alert was captured
        boolean foundAbnormalityAlert = alertManager.containsAlertWithKeyword("Abnormality");
        
        assertTrue(foundAbnormalityAlert, "Should detect ECG abnormality with extremely abnormal values");
    }
    
    /**
     * Test the HeartRateStrategy logic directly
     */
    @Test
    public void testHeartRateStrategyLogic() {
        // Create a strategy to test directly
        HeartRateStrategy strategy = new HeartRateStrategy();
        
        // Create sample data
        List<PatientRecord> ecgRecords = new ArrayList<>();
        
        // Add 50 normal readings - using correct constructor: (patientId, measurementValue, recordType, timestamp)
        long now = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            ecgRecords.add(new PatientRecord(9999, 0.2, "ECG", now - (60 - i) * 1000));
        }
        
        // Add 10 extremely abnormal readings
        for (int i = 0; i < 10; i++) {
            ecgRecords.add(new PatientRecord(9999, 5.0, "ECG", now - 10000 + i * 1000));
        }
        
        // Call the strategy directly and see what it returns
        Alert alert = strategy.checkAlert(null, ecgRecords);
        
        System.out.println("DEBUG - Direct strategy test:");
        System.out.println("  - Alert returned: " + (alert != null ? "YES" : "NO"));
        if (alert != null) {
            System.out.println("  - Alert condition: " + alert.getCondition());
        }
        
        // Calculate statistics for debugging
        double sum = 0;
        for (PatientRecord record : ecgRecords) {
            sum += record.getMeasurementValue();
        }
        double mean = sum / ecgRecords.size();
        
        double sumSquaredDiff = 0;
        for (PatientRecord record : ecgRecords) {
            sumSquaredDiff += Math.pow(record.getMeasurementValue() - mean, 2);
        }
        double stdDev = Math.sqrt(sumSquaredDiff / ecgRecords.size());
        
        double upperBound = mean + 3 * stdDev;
        double lowerBound = mean - 3 * stdDev;
        
        System.out.println("DEBUG - Statistics:");
        System.out.println("  - Mean: " + mean);
        System.out.println("  - StdDev: " + stdDev);
        System.out.println("  - Upper bound (mean + 3*stdDev): " + upperBound);
        System.out.println("  - Lower bound (mean - 3*stdDev): " + lowerBound);
        System.out.println("  - Abnormal value (5.0) is " + 
                          ((5.0 - mean) / stdDev) + " standard deviations from the mean");
    }
    
    /**
     * Main method for running the test from command line
     */
    public static void main(String[] args) {
        ECGAbnormalityTest test = new ECGAbnormalityTest();
        try {
            System.out.println("Setting up test environment...");
            test.setUp();
            
            System.out.println("\nTesting ECG abnormality detection...");
            test.testECGAbnormality();
            
            System.out.println("\nTesting heart rate strategy directly...");
            test.testHeartRateStrategyLogic();
            
            System.out.println("\nAll tests completed. Check output for results.");
        } catch (Exception e) {
            System.err.println("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
}