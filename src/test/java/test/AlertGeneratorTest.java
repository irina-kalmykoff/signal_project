package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.alerts.Alert;
import com.alerts.AlertGenerator;
import com.data_management.DataStorage;
import com.data_management.FileDataReader;
import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * JUnit tests for the AlertGenerator class
 * Tests each alert type with specific test patients
 */
public class AlertGeneratorTest {
    
    private DataStorage dataStorage;
    private String testOutputDir = "./test_output";
    private TestAlertCollector alertCollector;
    
    // Test alert collector to capture alerts
    private static class TestAlertCollector extends AlertGenerator {
        private List<Alert> capturedAlerts = new ArrayList<>();
        
        public TestAlertCollector(DataStorage dataStorage) {
            super(dataStorage);
        }
        
        @Override
        protected void triggerAlert(Alert alert) {
            capturedAlerts.add(alert);
            System.out.println("TEST ALERT: " + alert.getCondition() + 
                    " for Patient ID: " + alert.getPatientId());
        }
        
        public List<Alert> getCapturedAlerts() {
            return capturedAlerts;
        }
        
        public void clearAlerts() {
            capturedAlerts.clear();
        }
        
        public boolean containsAlertWithKeyword(String keyword) {
            for (Alert alert : capturedAlerts) {
                if (alert.getCondition().contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
        
    }
    
    @BeforeEach
    public void setUp() throws IOException {
        // Generate test data
        TestDataGenerator generator = new TestDataGenerator(testOutputDir);
        generator.generateTestData();
        
        // Set up data storage
        dataStorage = new DataStorage();
        FileDataReader reader = new FileDataReader(testOutputDir);
        reader.readData(dataStorage);
        
        // Set up alert collector
        alertCollector = new TestAlertCollector(dataStorage);
    }
    
    @AfterEach
    public void cleanUp() {
        alertCollector.clearAlerts();
    }
    
    @Test
    public void testNormalPatient() {
        // Patient 0 should be normal with no alerts
        Patient patient = getPatientById(0);
        assertNotNull(patient, "Normal patient should exist");
        
        // Print information about the records for debugging
        List<PatientRecord> allRecords = patient.getRecords(0, Long.MAX_VALUE);
        System.out.println("DEBUG - Patient 0 has " + allRecords.size() + " records");
        
        // Execute the alert check
        alertCollector.evaluateData(patient);
        List<Alert> alerts = alertCollector.getCapturedAlerts();
        
        // Debug: Print what alert is being triggered
        if (!alerts.isEmpty()) {
            System.out.println("DEBUG - Normal patient has " + alerts.size() + " alerts:");
            for (Alert alert : alerts) {
                System.out.println("DEBUG - Alert triggered: " + alert.getCondition());
            }
        }
        
        assertEquals(0, alerts.size(), "Normal patient should not generate alerts");
    }
    
    
    @Test
    public void testIncreasingBloodPressureTrend() {
        // Patient 1 should have increasing trend > 10mmHg (alert)
        Patient patient = getPatientById(1);
        assertNotNull(patient, "Increasing trend patient should exist");
        
        alertCollector.evaluateData(patient);
        boolean hasIncreasingTrendAlert = alertCollector.containsAlertWithKeyword("Increasing");
        
        assertTrue(hasIncreasingTrendAlert, "Should detect increasing blood pressure trend");
        
        // Patient 2 should have increasing trend = 10mmHg (no alert)
        alertCollector.clearAlerts();
        patient = getPatientById(2);
        assertNotNull(patient, "Borderline increasing trend patient should exist");
        
        alertCollector.evaluateData(patient);
        hasIncreasingTrendAlert = alertCollector.containsAlertWithKeyword("Increasing");
        
        assertFalse(hasIncreasingTrendAlert, "Should not alert for exactly 10mmHg increase");
    }
    
    @Test
    public void testDecreasingBloodPressureTrend() {
        // Patient 3 should have decreasing trend > 10mmHg (alert)
        Patient patient = getPatientById(3);
        assertNotNull(patient, "Decreasing trend patient should exist");
        
        alertCollector.evaluateData(patient);
        boolean hasDecreasingTrendAlert = alertCollector.containsAlertWithKeyword("Decreasing");
        
        assertTrue(hasDecreasingTrendAlert, "Should detect decreasing blood pressure trend");
        
        // Patient 4 should have decreasing trend = 10mmHg (no alert)
        alertCollector.clearAlerts();
        patient = getPatientById(4);
        assertNotNull(patient, "Borderline decreasing trend patient should exist");
        
        alertCollector.evaluateData(patient);
        hasDecreasingTrendAlert = alertCollector.containsAlertWithKeyword("Decreasing");
        
        assertFalse(hasDecreasingTrendAlert, "Should not alert for exactly 10mmHg decrease");
    }
    
    @Test
    public void testBloodPressureThresholds() {
        // Patient 5 should have high BP > thresholds (alert)
        Patient patient = getPatientById(5);
        assertNotNull(patient, "High BP patient should exist");
        
        alertCollector.evaluateData(patient);
        boolean hasHighBPAlert = alertCollector.containsAlertWithKeyword("high") && 
                                  alertCollector.containsAlertWithKeyword("pressure");
        
        assertTrue(hasHighBPAlert, "Should detect high blood pressure threshold");
        
        // Patient 6 should have low BP < thresholds (alert)
        alertCollector.clearAlerts();
        patient = getPatientById(6);
        assertNotNull(patient, "Low BP patient should exist");
        
        alertCollector.evaluateData(patient);
        boolean hasLowBPAlert = alertCollector.containsAlertWithKeyword("low") && 
                                 alertCollector.containsAlertWithKeyword("pressure");
        
        assertTrue(hasLowBPAlert, "Should detect low blood pressure threshold");
        
        // Patient 7 should have BP = high thresholds (no alert)
        alertCollector.clearAlerts();
        patient = getPatientById(7);
        assertNotNull(patient, "Borderline high BP patient should exist");
        
        alertCollector.evaluateData(patient);
        
        assertEquals(0, alertCollector.getCapturedAlerts().size(), "Should not alert for exactly at high threshold");
        
        // Patient 8 should have BP = low thresholds (no alert)
        alertCollector.clearAlerts();
        patient = getPatientById(8);
        assertNotNull(patient, "Borderline low BP patient should exist");
        
        alertCollector.evaluateData(patient);
        
        assertEquals(0, alertCollector.getCapturedAlerts().size(), "Should not alert for exactly at low threshold");
    }
    
    @Test
    public void testOxygenSaturation() {
        // Patient 9 should have O2 = 92% (no low oxygen alert)
        Patient patient = getPatientById(9);
        assertNotNull(patient, "Borderline saturation patient should exist");

        alertCollector.evaluateData(patient);
        boolean hasLowO2Alert = false;

        assertEquals(0, countAlertsOfType(alertCollector.getCapturedAlerts(), "low oxygen"),
                "Should not alert for exactly 92% saturation");

        // Patient 10 should have O2 < 92% (alert)
        alertCollector.clearAlerts();
        patient = getPatientById(10);
        assertNotNull(patient, "Low saturation patient should exist");

        alertCollector.evaluateData(patient);
        for (Alert alert : alertCollector.getCapturedAlerts()) {
            String condition = alert.getCondition().toLowerCase();
            if (condition.contains("low") && condition.contains("oxygen") &&
                    !condition.contains("drop") && !condition.contains("rapid")) {
                hasLowO2Alert = true;
                break;
            }
        }

        assertTrue(hasLowO2Alert, "Should detect low oxygen saturation");
    }

    // Helper method to count specific alert types
    private int countAlertsOfType(List<Alert> alerts, String alertType) {
        int count = 0;
        for (Alert alert : alerts) {
            if (alert.getCondition().toLowerCase().contains(alertType.toLowerCase())) {
                count++;
            }
        }
        return count;
    }
    
    @Test
    public void testRapidSaturationDrop() {
        // Patient 11 should have O2 drop ≥ 5% in 10min (alert)
        Patient patient = getPatientById(11);
        assertNotNull(patient, "Rapid drop patient should exist");
        
        alertCollector.evaluateData(patient);
        boolean hasRapidDropAlert = alertCollector.containsAlertWithKeyword("Rapid") && 
                                     alertCollector.containsAlertWithKeyword("Drop");
        
        assertTrue(hasRapidDropAlert, "Should detect rapid saturation drop");
        
        // Patient 12 should have O2 drop ≥ 5% over >10min (no alert)
        alertCollector.clearAlerts();
        patient = getPatientById(12);
        assertNotNull(patient, "Extended drop patient should exist");
        
        alertCollector.evaluateData(patient);
        
        assertEquals(0, alertCollector.getCapturedAlerts().size(), "Should not alert for drop over extended period");
    }
    
    @Test
    public void testHypotensiveHypoxemia() {
        // Patient 13 should have both low BP and low O2 together (alert)
        Patient patient = getPatientById(13);
        assertNotNull(patient, "Hypotensive hypoxemia patient should exist");
        
        alertCollector.evaluateData(patient);
        boolean hasHypotensiveHypoxemiaAlert = alertCollector.containsAlertWithKeyword("Hypotensive") && 
                                               alertCollector.containsAlertWithKeyword("Hypoxemia");
        
        assertTrue(hasHypotensiveHypoxemiaAlert, "Should detect hypotensive hypoxemia");
        
        // Patient 14 should have low BP & low O2 far apart (no alert)
        alertCollector.clearAlerts();
        patient = getPatientById(14);
        assertNotNull(patient, "Separated conditions patient should exist");
        
        alertCollector.evaluateData(patient);
        hasHypotensiveHypoxemiaAlert = alertCollector.containsAlertWithKeyword("Hypotensive") && 
                                        alertCollector.containsAlertWithKeyword("Hypoxemia");
        
        assertFalse(hasHypotensiveHypoxemiaAlert, "Should not detect hypotensive hypoxemia when conditions are separated");
    }
    
    @Test
    public void testECGAbnormalities() {
        // Patient 15 should have abnormal ECG (alert)
        Patient patient = getPatientById(15);
        assertNotNull(patient, "Abnormal ECG patient should exist");
        
        // Debug ECG data
        List<PatientRecord> ecgRecords = patient.getRecords(0, Long.MAX_VALUE)
            .stream()
            .filter(r -> r.getRecordType().equals("ECG"))
            .sorted(Comparator.comparing(PatientRecord::getTimestamp))
            .collect(Collectors.toList());
        
        System.out.println("DEBUG - Patient 15 has " + ecgRecords.size() + " ECG records");
        
        if (ecgRecords.size() >= 20) {
            // Calculate mean and std dev for debugging
            List<Double> values = ecgRecords.stream()
                .skip(Math.max(0, ecgRecords.size() - 30))
                .map(PatientRecord::getMeasurementValue)
                .collect(Collectors.toList());
            
            double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double stdDev = Math.sqrt(values.stream()
                .mapToDouble(val -> Math.pow(val - mean, 2))
                .average().orElse(0.0));
            
            System.out.println("DEBUG - ECG stats: Mean=" + mean + ", StdDev=" + stdDev);
            System.out.println("DEBUG - Bounds: [" + (mean - 3*stdDev) + ", " + (mean + 3*stdDev) + "]");
            
            // Print last 5 values
            System.out.println("DEBUG - Last 5 ECG values:");
            for (int i = Math.max(0, ecgRecords.size() - 5); i < ecgRecords.size(); i++) {
                double value = ecgRecords.get(i).getMeasurementValue();
                System.out.println("DEBUG - " + value + " " + 
                    (value > mean + 3*stdDev ? "ABOVE BOUND" : 
                    value < mean - 3*stdDev ? "BELOW BOUND" : "in range"));
            }
        }
        // Run the test
        alertCollector.evaluateData(patient);
        boolean hasECGAlert = alertCollector.containsAlertWithKeyword("ECG") && 
                              alertCollector.containsAlertWithKeyword("Abnormality");
        
        assertTrue(hasECGAlert, "Should detect ECG abnormality");
    }
    
    @Test
    public void testTriggeredAlert() {
        // Patient 17 should have manually triggered alert (alert)
        Patient patient = getPatientById(17);
        assertNotNull(patient, "Alert triggered patient should exist");
        
        alertCollector.evaluateData(patient);
        boolean hasButtonAlert = alertCollector.containsAlertWithKeyword("Call Button") || 
                                 alertCollector.containsAlertWithKeyword("Assistance");
        
        assertTrue(hasButtonAlert, "Should detect triggered call button alert");
    }
    
    /**
     * Helper method to get a patient by ID from the data storage
     */
    private Patient getPatientById(int patientId) {
        List<Patient> allPatients = dataStorage.getAllPatients();
        for (Patient patient : allPatients) {
            if (!patient.getRecords(0, Long.MAX_VALUE).isEmpty() && 
                patient.getRecords(0, Long.MAX_VALUE).get(0).getPatientId() == patientId) {
                return patient;
            }
        }
        return null;
    }
}