package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import com.data_management.*;
import com.alerts.*;

/**
 * Isolated test class that focuses on testing a single normal patient
 * with all alert factories to verify basic functionality.
 */
public class FirstPatientTests {
    
    private DataStorage dataStorage;
    private TestAlertFactoryManager alertManager;
    private Patient normalPatient;
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
        
        // Generate normal patient data
        generateNormalPatient();
        
        // Load the patient into the data storage
        normalPatient = loadPatientFromFile("normal_patient_0.txt");
    }
    
    /**
     * Generate a normal patient with stable vital signs
     */
    private void generateNormalPatient() throws IOException {
        Random random = new Random();
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        // Generate 10 minutes of stable, very normal data
        for (int i = 0; i < 600; i++) {
            long timestamp = baseTime - (600 - i) * 1000; // Last 10 minutes
            
            // Very stable normal blood pressure (minimal variation)
            records.add(String.format("%d,%d,%s,%.1f", 0, timestamp, "SystolicPressure", 120.0 + random.nextDouble() * 2 - 1));
            records.add(String.format("%d,%d,%s,%.1f", 0, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 2 - 1));
            
            // Stable normal oxygen
            records.add(String.format("%d,%d,%s,%.1f", 0, timestamp, "Saturation", 98.0 + random.nextDouble() - 0.5));
            
            // Very stable ECG
            records.add(String.format("%d,%d,%s,%.3f", 0, timestamp, "ECG", 0.2 + random.nextDouble() * 0.02 - 0.01));
        }
        
        // Write to file
        Path filePath = Paths.get(TEST_DATA_DIR, "normal_patient_0.txt");
        Files.write(filePath, records, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * Helper method to load a patient from a file
     */
    private Patient loadPatientFromFile(String filename) {
        try {
            Path filePath = Paths.get(TEST_DATA_DIR, filename);
            List<String> lines = Files.readAllLines(filePath);
            
            // Parse the records and add to data storage
            if (!lines.isEmpty()) {
                // Process the first line to get the patient ID
                String[] parts = lines.get(0).split(",");
                int patientId = Integer.parseInt(parts[0]);
                
                // Parse and add each record
                for (String line : lines) {
                    parts = line.split(",");
                    if (parts.length >= 4) {
                        int recordPatientId = Integer.parseInt(parts[0]);
                        long timestamp = Long.parseLong(parts[1]);
                        String recordType = parts[2];
                        double value = Double.parseDouble(parts[3]);
                        
                        // Add data using your API
                        dataStorage.addPatientData(recordPatientId, value, recordType, timestamp);
                    }
                }
                
                // Return the patient from storage
                return dataStorage.getPatient(patientId);
            }
        } catch (Exception e) {
            System.err.println("Error loading patient from file: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Test that all factories process a normal patient correctly with no alerts
     */
    @Test
    public void testNormalPatientNoAlerts() {
        // Process the normal patient
        alertManager.checkAllAlerts(normalPatient);
        
        // Export results to CSV for manual inspection
        alertManager.exportResultsToCSV("normal_patient_results.csv");
        
        // Check that no alerts were triggered
        List<Alert> allAlerts = alertManager.getAllAlerts();
        
        // Print detailed information about any alerts that were triggered
        if (!allAlerts.isEmpty()) {
            System.out.println("UNEXPECTED ALERTS FOR NORMAL PATIENT:");
            for (Alert alert : allAlerts) {
                System.out.println("  - " + alert.getCondition() + " (Patient: " + alert.getPatientId() + ")");
            }
        }
        
        assertEquals(0, alertManager.getOxygenAlertFactory().getCapturedAlerts().size(), 
                "Normal patient should not trigger oxygen alerts");
        assertEquals(0, alertManager.getPressureAlertFactory().getCapturedAlerts().size(), 
                "Normal patient should not trigger pressure alerts");
        assertEquals(0, alertManager.getEcgAlertFactory().getCapturedAlerts().size(), 
                "Normal patient should not trigger ECG alerts");
        assertEquals(0, alertManager.getCallButtonAlertFactory().getCapturedAlerts().size(), 
                "Normal patient should not trigger call button alerts");
        assertEquals(0, alertManager.getDecoratedAlertFactory().getCapturedAlerts().size(), 
                "Normal patient should not trigger decorated alerts");
    }
    
    /**
     * Test the alert factory detection thresholds directly
     */
    @Test
    public void testBloodPressureThresholds() {
        // Create patient with high and normal blood pressure
        int patientId = 999;
        long now = System.currentTimeMillis();
        
        // Add a normal reading first
        dataStorage.addPatientData(patientId, 120.0, "SystolicPressure", now - 2000);
        
        // Get the patient
        Patient testPatient = dataStorage.getPatient(patientId);
        
        // Test exact threshold (should NOT trigger alert)
        dataStorage.addPatientData(patientId, 180.0, "SystolicPressure", now - 1000);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(testPatient);
        assertEquals(0, alertManager.getPressureAlertFactory().getCapturedAlerts().size(), 
                "Blood pressure exactly at high threshold should not trigger alert");
        
        // Reset patient
        patientId = 998;
        dataStorage.addPatientData(patientId, 120.0, "SystolicPressure", now - 2000);
        
        // Test above threshold (SHOULD trigger alert)
        dataStorage.addPatientData(patientId, 181.0, "SystolicPressure", now - 1000);
        testPatient = dataStorage.getPatient(patientId);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(testPatient);
        assertEquals(1, alertManager.getPressureAlertFactory().getCapturedAlerts().size(), 
                "Blood pressure above high threshold should trigger alert");
    }
    
    /**
     * Test increasing blood pressure trend detection with exact values
     */
    @Test
    public void testIncreasingBloodPressureTrend() {
        long now = System.currentTimeMillis();
        
        // Test case 1: Increasing trend exactly 10 mmHg (should NOT trigger alert)
        int patientId1 = 1001;
        dataStorage.addPatientData(patientId1, 120.0, "SystolicPressure", now - 3000);
        dataStorage.addPatientData(patientId1, 130.0, "SystolicPressure", now - 2000); // +10
        dataStorage.addPatientData(patientId1, 140.0, "SystolicPressure", now - 1000); // +10
        
        Patient patient1 = dataStorage.getPatient(patientId1);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(patient1);
        
        System.out.println("DEBUG - Increasing Trend Test (Exactly 10 mmHg):");
        System.out.println("  - First reading: 120.0");
        System.out.println("  - Second reading: 130.0 (diff: +10.0)");
        System.out.println("  - Third reading: 140.0 (diff: +10.0)");
        System.out.println("  - Alerts generated: " + alertManager.getPressureAlertFactory().getCapturedAlerts().size());
        
        assertEquals(0, alertManager.getPressureAlertFactory().getCapturedAlerts().size(), 
                "Exactly 10 mmHg increases should not trigger alert");
        
        // Test case 2: Increasing trend > 10 mmHg (SHOULD trigger alert)
        int patientId2 = 1002;
        dataStorage.addPatientData(patientId2, 120.0, "SystolicPressure", now - 3000);
        dataStorage.addPatientData(patientId2, 131.0, "SystolicPressure", now - 2000); // +11
        dataStorage.addPatientData(patientId2, 142.0, "SystolicPressure", now - 1000); // +11
        
        Patient patient2 = dataStorage.getPatient(patientId2);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(patient2);
        
        System.out.println("DEBUG - Increasing Trend Test (> 10 mmHg):");
        System.out.println("  - First reading: 120.0");
        System.out.println("  - Second reading: 131.0 (diff: +11.0)");
        System.out.println("  - Third reading: 142.0 (diff: +11.0)");
        System.out.println("  - Alerts generated: " + alertManager.getPressureAlertFactory().getCapturedAlerts().size());
        
        assertEquals(1, alertManager.getPressureAlertFactory().getCapturedAlerts().size(), 
                "Increases > 10 mmHg should trigger alert");
    }
    
    /**
     * Test decreasing blood pressure trend detection with exact values
     */
    @Test
    public void testDecreasingBloodPressureTrend() {
        long now = System.currentTimeMillis();
        
        // Test case 1: Decreasing trend exactly 10 mmHg (should NOT trigger alert)
        int patientId1 = 2001;
        dataStorage.addPatientData(patientId1, 150.0, "SystolicPressure", now - 3000);
        dataStorage.addPatientData(patientId1, 140.0, "SystolicPressure", now - 2000); // -10
        dataStorage.addPatientData(patientId1, 130.0, "SystolicPressure", now - 1000); // -10
        
        Patient patient1 = dataStorage.getPatient(patientId1);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(patient1);
        
        System.out.println("DEBUG - Decreasing Trend Test (Exactly 10 mmHg):");
        System.out.println("  - First reading: 150.0");
        System.out.println("  - Second reading: 140.0 (diff: -10.0)");
        System.out.println("  - Third reading: 130.0 (diff: -10.0)");
        System.out.println("  - Alerts generated: " + alertManager.getPressureAlertFactory().getCapturedAlerts().size());
        
        assertEquals(0, alertManager.getPressureAlertFactory().getCapturedAlerts().size(), 
                "Exactly 10 mmHg decreases should not trigger alert");
        
        // Test case 2: Decreasing trend > 10 mmHg (SHOULD trigger alert)
        int patientId2 = 2002;
        dataStorage.addPatientData(patientId2, 150.0, "SystolicPressure", now - 3000);
        dataStorage.addPatientData(patientId2, 139.0, "SystolicPressure", now - 2000); // -11
        dataStorage.addPatientData(patientId2, 128.0, "SystolicPressure", now - 1000); // -11
        
        Patient patient2 = dataStorage.getPatient(patientId2);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(patient2);
        
        System.out.println("DEBUG - Decreasing Trend Test (> 10 mmHg):");
        System.out.println("  - First reading: 150.0");
        System.out.println("  - Second reading: 139.0 (diff: -11.0)");
        System.out.println("  - Third reading: 128.0 (diff: -11.0)");
        System.out.println("  - Alerts generated: " + alertManager.getPressureAlertFactory().getCapturedAlerts().size());
        
        assertEquals(1, alertManager.getPressureAlertFactory().getCapturedAlerts().size(), 
                "Decreases > 10 mmHg should trigger alert");
    }
    
    /**
     * Test ECG abnormality detection with extreme values
     */
    @Test
    public void testECGAbnormality() {
        long now = System.currentTimeMillis();
        int patientId = 3001;
        
        // Add 50 normal ECG readings to establish baseline
        for (int i = 0; i < 50; i++) {
            dataStorage.addPatientData(patientId, 0.2, "ECG", now - (60 - i) * 1000);
        }
        
        // Add abnormal ECG readings (far outside normal range)
        for (int i = 0; i < 10; i++) {
            dataStorage.addPatientData(patientId, 3.0, "ECG", now - 10000 + i * 1000); // Very high
        }
        
        Patient patient = dataStorage.getPatient(patientId);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(patient);
        
        // Check if ECG alert was triggered
        System.out.println("DEBUG - ECG Abnormality Test:");
        System.out.println("  - Added 50 normal readings of 0.2");
        System.out.println("  - Added 10 abnormal readings of 3.0");
        System.out.println("  - Alerts generated: " + alertManager.getEcgAlertFactory().getCapturedAlerts().size());
        
        // If no alert was triggered, print more details
        if (alertManager.getEcgAlertFactory().getCapturedAlerts().isEmpty()) {
            System.out.println("WARNING: ECG ABNORMALITY NOT DETECTED!");
        }
        
        assertEquals(1, alertManager.getEcgAlertFactory().getCapturedAlerts().size(), 
                "Abnormal ECG values should trigger alert");
    }
    
    /**
     * Test hypotensive hypoxemia detection (low BP and low O2 together)
     */
    @Test
    public void testHypotensiveHypoxemia() {
        long now = System.currentTimeMillis();
        int patientId = 4001;
        
        // Add baseline normal records
        for (int i = 0; i < 10; i++) {
            dataStorage.addPatientData(patientId, 120.0, "SystolicPressure", now - 20000 - i * 1000);
            dataStorage.addPatientData(patientId, 98.0, "Saturation", now - 20000 - i * 1000);
        }
        
        // Add low values for both at same time
        for (int i = 0; i < 5; i++) {
            dataStorage.addPatientData(patientId, 89.0, "SystolicPressure", now - 5000 + i * 1000); // Low BP
            dataStorage.addPatientData(patientId, 91.0, "Saturation", now - 5000 + i * 1000); // Low O2
        }
        
        Patient patient = dataStorage.getPatient(patientId);
        alertManager.clearAllAlerts();
        alertManager.checkAllAlerts(patient);
        
        // Check if hypotensive hypoxemia was detected
        boolean foundAlert = alertManager.containsAlertWithKeyword("Hypotensive") || 
                             alertManager.containsAlertWithKeyword("hypoxemia");
        
        System.out.println("DEBUG - Hypotensive Hypoxemia Test:");
        System.out.println("  - Added 10 normal BP (120.0) and O2 (98.0) readings");
        System.out.println("  - Added 5 low BP (89.0) and low O2 (91.0) readings");
        System.out.println("  - Alert detected: " + foundAlert);
        System.out.println("  - Oxygen alerts: " + alertManager.getOxygenAlertFactory().getCapturedAlerts().size());
        System.out.println("  - Pressure alerts: " + alertManager.getPressureAlertFactory().getCapturedAlerts().size());
        
        assertTrue(foundAlert, "Should detect hypotensive hypoxemia condition");
    }
    
    /**
     * Main method for running the test from command line
     */
    public static void main(String[] args) {
        FirstPatientTests test = new FirstPatientTests();
        try {
            // System.out.println("Setting up test environment...");
            test.setUp();
            
            // System.out.println("\nRunning normal patient test...");
            test.testNormalPatientNoAlerts();
            
            // System.out.println("\nRunning blood pressure threshold test...");
            test.testBloodPressureThresholds();
            
            // System.out.println("\nRunning increasing trend test...");
            test.testIncreasingBloodPressureTrend();
            
            // System.out.println("\nRunning decreasing trend test...");
            test.testDecreasingBloodPressureTrend();
            
            // System.out.println("\nRunning ECG abnormality test...");
            test.testECGAbnormality();
            
            // System.out.println("\nRunning hypotensive hypoxemia test...");
            test.testHypotensiveHypoxemia();
            
            // System.out.println("\nAll tests completed. Check output for results.");
        } catch (Exception e) {
            // System.err.println("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
}