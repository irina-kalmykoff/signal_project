package test;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

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
        
        // public List<Alert> getAlertsForPatient(String patientId) {
        //     List<Alert> patientAlerts = new ArrayList<>();
        //     for (Alert alert : capturedAlerts) {
        //         if (alert.getPatientId().equals(patientId)) {
        //             patientAlerts.add(alert);
        //         }
        //     }
        //     return patientAlerts;
        // }
    }
    
    @Before
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
    
    @After
    public void cleanUp() {
        alertCollector.clearAlerts();
    }
    
    @Test
    public void testNormalPatient() {
        // Patient 0 should be normal with no alerts
        Patient patient = getPatientById(0);
        assertNotNull("Normal patient should exist", patient);
        
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
        
        assertEquals("Normal patient should not generate alerts", 0, alerts.size());
    }
    
    
    @Test
    public void testIncreasingBloodPressureTrend() {
        // Patient 1 should have increasing trend > 10mmHg (alert)
        Patient patient = getPatientById(1);
        assertNotNull("Increasing trend patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        boolean hasIncreasingTrendAlert = alertCollector.containsAlertWithKeyword("Increasing");
        
        assertTrue("Should detect increasing blood pressure trend", hasIncreasingTrendAlert);
        
        // Patient 2 should have increasing trend = 10mmHg (no alert)
        alertCollector.clearAlerts();
        patient = getPatientById(2);
        assertNotNull("Borderline increasing trend patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        hasIncreasingTrendAlert = alertCollector.containsAlertWithKeyword("Increasing");
        
        assertFalse("Should not alert for exactly 10mmHg increase", hasIncreasingTrendAlert);
    }
    
    @Test
    public void testDecreasingBloodPressureTrend() {
        // Patient 3 should have decreasing trend > 10mmHg (alert)
        Patient patient = getPatientById(3);
        assertNotNull("Decreasing trend patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        boolean hasDecreasingTrendAlert = alertCollector.containsAlertWithKeyword("Decreasing");
        
        assertTrue("Should detect decreasing blood pressure trend", hasDecreasingTrendAlert);
        
        // Patient 4 should have decreasing trend = 10mmHg (no alert)
        alertCollector.clearAlerts();
        patient = getPatientById(4);
        assertNotNull("Borderline decreasing trend patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        hasDecreasingTrendAlert = alertCollector.containsAlertWithKeyword("Decreasing");
        
        assertFalse("Should not alert for exactly 10mmHg decrease", hasDecreasingTrendAlert);
    }
    
    @Test
    public void testBloodPressureThresholds() {
        // Patient 5 should have high BP > thresholds (alert)
        Patient patient = getPatientById(5);
        assertNotNull("High BP patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        boolean hasHighBPAlert = alertCollector.containsAlertWithKeyword("high") && 
                                  alertCollector.containsAlertWithKeyword("pressure");
        
        assertTrue("Should detect high blood pressure threshold", hasHighBPAlert);
        
        // Patient 6 should have low BP < thresholds (alert)
        alertCollector.clearAlerts();
        patient = getPatientById(6);
        assertNotNull("Low BP patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        boolean hasLowBPAlert = alertCollector.containsAlertWithKeyword("low") && 
                                 alertCollector.containsAlertWithKeyword("pressure");
        
        assertTrue("Should detect low blood pressure threshold", hasLowBPAlert);
        
        // Patient 7 should have BP = high thresholds (no alert)
        alertCollector.clearAlerts();
        patient = getPatientById(7);
        assertNotNull("Borderline high BP patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        
        assertEquals("Should not alert for exactly at high threshold", 0, alertCollector.getCapturedAlerts().size());
        
        // Patient 8 should have BP = low thresholds (no alert)
        alertCollector.clearAlerts();
        patient = getPatientById(8);
        assertNotNull("Borderline low BP patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        
        assertEquals("Should not alert for exactly at low threshold", 0, alertCollector.getCapturedAlerts().size());
    }
    
    @Test
    public void testOxygenSaturation() {
        // Patient 9 should have O2 = 92% (no alert)
        Patient patient = getPatientById(9);
        assertNotNull("Borderline saturation patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        
        assertEquals("Should not alert for exactly 92% saturation", 0, alertCollector.getCapturedAlerts().size());
        
        // Patient 10 should have O2 < 92% (alert)
        alertCollector.clearAlerts();
        patient = getPatientById(10);
        assertNotNull("Low saturation patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        boolean hasLowO2Alert = alertCollector.containsAlertWithKeyword("low") && 
                                 alertCollector.containsAlertWithKeyword("oxygen");
        
        assertTrue("Should detect low oxygen saturation", hasLowO2Alert);
    }
    
    @Test
    public void testRapidSaturationDrop() {
        // Patient 11 should have O2 drop ≥ 5% in 10min (alert)
        Patient patient = getPatientById(11);
        assertNotNull("Rapid drop patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        boolean hasRapidDropAlert = alertCollector.containsAlertWithKeyword("Rapid") && 
                                     alertCollector.containsAlertWithKeyword("Drop");
        
        assertTrue("Should detect rapid saturation drop", hasRapidDropAlert);
        
        // Patient 12 should have O2 drop ≥ 5% over >10min (no alert)
        alertCollector.clearAlerts();
        patient = getPatientById(12);
        assertNotNull("Extended drop patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        
        assertEquals("Should not alert for drop over extended period", 0, alertCollector.getCapturedAlerts().size());
    }
    
    @Test
    public void testHypotensiveHypoxemia() {
        // Patient 13 should have both low BP and low O2 together (alert)
        Patient patient = getPatientById(13);
        assertNotNull("Hypotensive hypoxemia patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        boolean hasHypotensiveHypoxemiaAlert = alertCollector.containsAlertWithKeyword("Hypotensive") && 
                                               alertCollector.containsAlertWithKeyword("Hypoxemia");
        
        assertTrue("Should detect hypotensive hypoxemia", hasHypotensiveHypoxemiaAlert);
        
        // Patient 14 should have low BP & low O2 far apart (no alert)
        alertCollector.clearAlerts();
        patient = getPatientById(14);
        assertNotNull("Separated conditions patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        hasHypotensiveHypoxemiaAlert = alertCollector.containsAlertWithKeyword("Hypotensive") && 
                                        alertCollector.containsAlertWithKeyword("Hypoxemia");
        
        assertFalse("Should not detect hypotensive hypoxemia when conditions are separated", 
                   hasHypotensiveHypoxemiaAlert);
    }
    
    @Test
    public void testECGAbnormalities() {
        // Patient 15 should have abnormal ECG (alert)
        Patient patient = getPatientById(15);
        assertNotNull("Abnormal ECG patient should exist", patient);
        
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
    
    assertTrue("Should detect ECG abnormality", hasECGAlert);
    }
    
    @Test
    public void testTriggeredAlert() {
        // Patient 17 should have manually triggered alert (alert)
        Patient patient = getPatientById(17);
        assertNotNull("Alert triggered patient should exist", patient);
        
        alertCollector.evaluateData(patient);
        boolean hasButtonAlert = alertCollector.containsAlertWithKeyword("Call Button") || 
                                 alertCollector.containsAlertWithKeyword("Assistance");
        
        assertTrue("Should detect triggered call button alert", hasButtonAlert);
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