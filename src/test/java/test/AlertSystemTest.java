package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.alerts.Alert;
import com.alerts.AlertGenerator;
import com.data_management.DataStorage;
import com.data_management.FileDataReader;
import com.data_management.Patient;
import com.data_management.PatientRecord;

/**
 * Test client for alert system
 */
public class AlertSystemTest {
    
    // A custom alert collector to capture alerts
    private static class TestAlertCollector extends AlertGenerator {
        private List<Alert> capturedAlerts = new ArrayList<>();
        
        public TestAlertCollector(DataStorage dataStorage) {
            super(dataStorage);
        }
        
        @Override
        protected void triggerAlert(Alert alert) {
            capturedAlerts.add(alert);
            System.out.println("ALERT #" + capturedAlerts.size() + ": " + alert.getCondition() +
                    " for Patient ID: " + alert.getPatientId() +
                    " at " + new java.util.Date(alert.getTimestamp()));
        }
        
        public int getAlertCount() {
            return capturedAlerts.size();
        }
        
        
        public List<Alert> getAlertsForPatient(String patientId) {
            List<Alert> patientAlerts = new ArrayList<>();
            for (Alert alert : capturedAlerts) {
                if (alert.getPatientId().equals(patientId)) {
                    patientAlerts.add(alert);
                }
            }
            return patientAlerts;
        }
    }
    
    public static void main(String[] args) {
        try {
            // Define test output directory
            String testOutputDir = "./test_output";
            
            // Step 1: Generate test data
            System.out.println("Generating test data...");
            TestDataGenerator generator = new TestDataGenerator(testOutputDir);
            generator.generateTestData();
            
            // Step 2: Read the data using FileDataReader
            System.out.println("Reading test data...");
            DataStorage dataStorage = new DataStorage();
            FileDataReader reader = new FileDataReader(testOutputDir);
            reader.readData(dataStorage);
            
            // Step 3: Process each patient through the alert system
            System.out.println("Processing alerts...");
            TestAlertCollector alertCollector = new TestAlertCollector(dataStorage);
            
            List<Patient> patients = dataStorage.getAllPatients();
            System.out.println("Found " + patients.size() + " patients in the data");
            
            for (Patient patient : patients) {
                int patientId = -1;
                
                // Try to get the patient ID from the first available record
                List<PatientRecord> records = patient.getRecords(0, Long.MAX_VALUE);
                if (!records.isEmpty()) {
                    patientId = records.get(0).getPatientId();
                } else {
                    // Fallback to index+1 if no records available
                    patientId = patients.indexOf(patient) + 1;
                }
                
                System.out.println("\nProcessing Patient ID: " + patientId);
                alertCollector.evaluateData(patient);
                
                // Print alert results for this patient
                List<Alert> patientAlerts = alertCollector.getAlertsForPatient(String.valueOf(patientId));
                if (patientAlerts.isEmpty()) {
                    System.out.println("No alerts detected for Patient ID: " + patientId);
                } else {
                    System.out.println("Detected " + patientAlerts.size() + " alert(s) for Patient ID: " + patientId);
                }
            }
            
            // Summary
            System.out.println("\nTest Summary:");
            System.out.println("Total number of patients: " + patients.size());
            System.out.println("Total Alerts Generated: " + alertCollector.getAlertCount());
            
            // Expected alerts categorization
            System.out.println("\nExpected Test Results:");
            System.out.println("Patient 0: No alerts (normal)");
            System.out.println("Patient 1: Increasing trend alert (>10mmHg)");
            System.out.println("Patient 2: No alert (trend =10mmHg)");
            System.out.println("Patient 3: Decreasing trend alert (>10mmHg)");
            System.out.println("Patient 4: No alert (trend =10mmHg)");
            System.out.println("Patient 5: High threshold BP alert");
            System.out.println("Patient 6: Low threshold BP alert");
            System.out.println("Patient 7: No alert (at high threshold)");
            System.out.println("Patient 8: No alert (at low threshold)");
            System.out.println("Patient 9: No alert (saturation =92%)");
            System.out.println("Patient 10: Low saturation alert (<92%)");
            System.out.println("Patient 11: Rapid saturation drop alert");
            System.out.println("Patient 12: No alert (drop over >10min)");
            System.out.println("Patient 13: Hypotensive hypoxemia alert");
            System.out.println("Patient 14: No alert (conditions separated)");
            System.out.println("Patient 15: ECG abnormality alert");
            System.out.println("Patient 16: No alert (borderline ECG)");
            System.out.println("Patient 17: Call button alert");
            
        } catch (IOException e) {
            System.err.println("Error during test execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
}