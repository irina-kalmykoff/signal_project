package test;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import com.data_management.*;
import com.alerts.*;
import java.util.stream.Collectors;

public class TestAlertFactoryManager {
    private TestBloodOxygenAlertFactory oxygenAlertFactory;
    private TestBloodPressureAlertFactory pressureAlertFactory;
    private TestECGAlertFactory ecgAlertFactory;
    private TestCallButtonAlertFactory callButtonAlertFactory;
    private TestDecoratedAlertFactory decoratedAlertFactory;
    private final Map<String, Map<String, Integer>> testResults = new HashMap<>();
    
    public TestAlertFactoryManager(DataStorage dataStorage) {
        this.oxygenAlertFactory = new TestBloodOxygenAlertFactory(dataStorage);
        this.pressureAlertFactory = new TestBloodPressureAlertFactory(dataStorage);
        this.ecgAlertFactory = new TestECGAlertFactory(dataStorage);
        this.callButtonAlertFactory = new TestCallButtonAlertFactory(dataStorage);
        this.decoratedAlertFactory = new TestDecoratedAlertFactory(dataStorage);
    }
    
    // Method to check alerts for a patient
    public void checkAllAlerts(Patient patient) {
        if (patient == null || patient.getRecords(0, Long.MAX_VALUE).isEmpty()) {
            return;
        }
        
        String patientId = String.valueOf(patient.getRecords(0, Long.MAX_VALUE).get(0).getPatientId());
        long timestamp = System.currentTimeMillis();
        
        System.out.println("DEBUG: Checking alerts for patient " + patientId); 
        // Clear existing alerts before testing this patient
        clearAllAlerts();

        // Get a count of records of each type to verify we have data
        List<PatientRecord> records = patient.getRecords(0, Long.MAX_VALUE);
        Map<String, Long> recordCounts = records.stream()
        .collect(Collectors.groupingBy(PatientRecord::getRecordType, Collectors.counting()));
    
        System.out.println("DEBUG: Record counts by type:");
        recordCounts.forEach((type, count) -> 
            System.out.println("DEBUG:   " + type + ": " + count));
    
        // Call each factory
        System.out.println("DEBUG: Calling oxygenAlertFactory.createAlert");
        oxygenAlertFactory.createAlert(patientId, "", timestamp);
        
        System.out.println("DEBUG: Calling pressureAlertFactory.createAlert");
        pressureAlertFactory.createAlert(patientId, "", timestamp);
        
        System.out.println("DEBUG: Calling ecgAlertFactory.createAlert");
        ecgAlertFactory.createAlert(patientId, "", timestamp);
        
        System.out.println("DEBUG: Calling callButtonAlertFactory.createAlert");
        callButtonAlertFactory.createAlert(patientId, "", timestamp);
        
        System.out.println("DEBUG: Calling decoratedAlertFactory.createAlert");
        decoratedAlertFactory.createAlert(patientId, "", timestamp);

        // Print captured alerts
        System.out.println("DEBUG: Captured alerts after factory calls:");
        int oxygenAlerts = oxygenAlertFactory.getCapturedAlerts().size();
        int pressureAlerts = pressureAlertFactory.getCapturedAlerts().size();
        int ecgAlerts = ecgAlertFactory.getCapturedAlerts().size();
        int callButtonAlerts = callButtonAlertFactory.getCapturedAlerts().size();
        int decoratedAlerts = decoratedAlertFactory.getCapturedAlerts().size();
        
        System.out.println("DEBUG:   Oxygen alerts: " + oxygenAlerts);
        System.out.println("DEBUG:   Pressure alerts: " + pressureAlerts);
        System.out.println("DEBUG:   ECG alerts: " + ecgAlerts);
        System.out.println("DEBUG:   Call button alerts: " + callButtonAlerts);
        System.out.println("DEBUG:   Decorated alerts: " + decoratedAlerts);

        // Call each factory
        // oxygenAlertFactory.createAlert(patientId, "", timestamp);
        // pressureAlertFactory.createAlert(patientId, "", timestamp);
        // ecgAlertFactory.createAlert(patientId, "", timestamp);
        // callButtonAlertFactory.createAlert(patientId, "", timestamp);
        // decoratedAlertFactory.createAlert(patientId, "", timestamp);

        // Store results for CSV export
        Map<String, Integer> patientResults = new HashMap<>();
        patientResults.put("OxygenAlerts", oxygenAlerts);
        patientResults.put("PressureAlerts", pressureAlerts);
        patientResults.put("ECGAlerts", ecgAlerts);
        patientResults.put("CallButtonAlerts", callButtonAlerts);
        patientResults.put("DecoratedAlerts", decoratedAlerts);
        patientResults.put("TotalAlerts", oxygenAlerts + pressureAlerts + ecgAlerts + callButtonAlerts + decoratedAlerts);
        
        // Log any specific alert messages
        if (oxygenAlerts > 0) {
            logAlertDetails(patientId, "Oxygen", oxygenAlertFactory.getCapturedAlerts());
        }
        if (pressureAlerts > 0) {
            logAlertDetails(patientId, "Pressure", pressureAlertFactory.getCapturedAlerts());
        }
        if (ecgAlerts > 0) {
            logAlertDetails(patientId, "ECG", ecgAlertFactory.getCapturedAlerts());
        }
        if (callButtonAlerts > 0) {
            logAlertDetails(patientId, "CallButton", callButtonAlertFactory.getCapturedAlerts());
        }
        if (decoratedAlerts > 0) {
            logAlertDetails(patientId, "Decorated", decoratedAlertFactory.getCapturedAlerts());
        }
        
        testResults.put(patientId, patientResults);

    }
    
    // Collect alerts from all factories
    public List<Alert> getAllAlerts() {
        List<Alert> allAlerts = new ArrayList<>();
        allAlerts.addAll(oxygenAlertFactory.getCapturedAlerts());
        allAlerts.addAll(pressureAlertFactory.getCapturedAlerts());
        allAlerts.addAll(ecgAlertFactory.getCapturedAlerts());
        allAlerts.addAll(callButtonAlertFactory.getCapturedAlerts());
        allAlerts.addAll(decoratedAlertFactory.getCapturedAlerts());
        return allAlerts;
    }
    
    // Clear alerts from all factories
    public void clearAllAlerts() {
        oxygenAlertFactory.clearAlerts();
        pressureAlertFactory.clearAlerts();
        ecgAlertFactory.clearAlerts();
        callButtonAlertFactory.clearAlerts();
        decoratedAlertFactory.clearAlerts();
    }
    
    // Check if any factory has an alert with the keyword
    public boolean containsAlertWithKeyword(String keyword) {
        return oxygenAlertFactory.containsAlertWithKeyword(keyword) ||
               pressureAlertFactory.containsAlertWithKeyword(keyword) ||
               ecgAlertFactory.containsAlertWithKeyword(keyword) ||
               callButtonAlertFactory.containsAlertWithKeyword(keyword) ||
               decoratedAlertFactory.containsAlertWithKeyword(keyword);
    }
    
    // Getters for individual factories
    public TestBloodOxygenAlertFactory getOxygenAlertFactory() {
        return oxygenAlertFactory;
    }
    
    public TestBloodPressureAlertFactory getPressureAlertFactory() {
        return pressureAlertFactory;
    }
    
    public TestECGAlertFactory getEcgAlertFactory() {
        return ecgAlertFactory;
    }
    
    public TestCallButtonAlertFactory getCallButtonAlertFactory() {
        return callButtonAlertFactory;
    }

    public TestDecoratedAlertFactory getDecoratedAlertFactory() {
        return decoratedAlertFactory;
    }

 
        // Helper method to log alert details
        private void logAlertDetails(String patientId, String alertType, List<Alert> alerts) {
            System.out.println("DEBUG: " + alertType + " alerts for patient " + patientId + ":");
            
            // Filter alerts to only include those for this patient
            List<Alert> patientAlerts = alerts.stream()
                .filter(alert -> alert.getPatientId().equals(patientId))
                .collect(Collectors.toList());
            
            for (int i = 0; i < patientAlerts.size(); i++) {
                Alert alert = patientAlerts.get(i);
                System.out.println("DEBUG:   [" + (i+1) + "] " + alert.getCondition());
                
                // Store alert condition in results map
                Map<String, Integer> patientResults = testResults.get(patientId);
                if (patientResults != null) {  // Add null check
                    patientResults.put(alertType + "Alert" + (i+1), 1);
                    patientResults.put(alertType + "AlertMsg" + (i+1), alert.getCondition().hashCode());
                }
            }
        }
        
        /**
         * Exports test results to a CSV file
         * @param filename the file to write to
         */
        public void exportResultsToCSV(String filename) {
            try {
                Path filePath = Paths.get(filename);
                StringBuilder csv = new StringBuilder();
                
                // Create header row
                Set<String> allColumns = new HashSet<>();
                allColumns.add("PatientID");
                
                // Collect all possible column names
                for (Map<String, Integer> results : testResults.values()) {
                    allColumns.addAll(results.keySet());
                }
                
                // Sort columns for consistent output
                List<String> sortedColumns = new ArrayList<>(allColumns);
                Collections.sort(sortedColumns);
                
                // Move some columns to the beginning for better readability
                List<String> orderedColumns = new ArrayList<>();
                orderedColumns.add("PatientID");
                orderedColumns.add("TotalAlerts");
                orderedColumns.add("OxygenAlerts");
                orderedColumns.add("PressureAlerts");
                orderedColumns.add("ECGAlerts");
                orderedColumns.add("CallButtonAlerts");
                orderedColumns.add("DecoratedAlerts");
                
                // Add remaining columns
                for (String col : sortedColumns) {
                    if (!orderedColumns.contains(col)) {
                        orderedColumns.add(col);
                    }
                }
                
                // Build header row
                csv.append(String.join(",", orderedColumns)).append("\n");
                
                // Add data rows
                for (String patientId : testResults.keySet()) {
                    Map<String, Integer> results = testResults.get(patientId);
                    List<String> rowValues = new ArrayList<>();
                    
                    for (String column : orderedColumns) {
                        if (column.equals("PatientID")) {
                            rowValues.add(patientId);
                        } else if (results.containsKey(column)) {
                            rowValues.add(String.valueOf(results.get(column)));
                        } else {
                            rowValues.add("0");
                        }
                    }
                    
                    csv.append(String.join(",", rowValues)).append("\n");
                }
                
                // Write to file
                Files.writeString(filePath, csv.toString());
                System.out.println("Test results exported to " + filePath.toAbsolutePath());
                
                // Also export a detailed log of alerts
                exportDetailedAlertLog(filename.replace(".csv", "_details.txt"));
                
            } catch (IOException e) {
                System.err.println("Error exporting results to CSV: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        /**
         * Exports a detailed log of all alerts triggered during testing
         * @param filename the file to write to
         */
        private void exportDetailedAlertLog(String filename) {
            try {
                Path filePath = Paths.get(filename);
                StringBuilder log = new StringBuilder();
                
                log.append("DETAILED ALERT LOG\n");
                log.append("=================\n\n");
                
                for (String patientId : testResults.keySet()) {
                    log.append("Patient ID: ").append(patientId).append("\n");
                    log.append("--------------------------\n");
                    
                    // Get alerts for this patient
                    List<Alert> patientAlerts = new ArrayList<>();
                    patientAlerts.addAll(getAlertsForPatient(oxygenAlertFactory.getCapturedAlerts(), patientId));
                    patientAlerts.addAll(getAlertsForPatient(pressureAlertFactory.getCapturedAlerts(), patientId));
                    patientAlerts.addAll(getAlertsForPatient(ecgAlertFactory.getCapturedAlerts(), patientId));
                    patientAlerts.addAll(getAlertsForPatient(callButtonAlertFactory.getCapturedAlerts(), patientId));
                    patientAlerts.addAll(getAlertsForPatient(decoratedAlertFactory.getCapturedAlerts(), patientId));
                    
                    if (patientAlerts.isEmpty()) {
                        log.append("No alerts triggered\n");
                    } else {
                        for (Alert alert : patientAlerts) {
                            log.append("- ").append(alert.getCondition()).append("\n");
                        }
                    }
                    
                    log.append("\n");
                }
                
                // Write to file
                Files.writeString(filePath, log.toString());
                System.out.println("Detailed alert log exported to " + filePath.toAbsolutePath());
                
            } catch (IOException e) {
                System.err.println("Error exporting detailed alert log: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        /**
         * Helper method to filter alerts by patient ID
         */
        private List<Alert> getAlertsForPatient(List<Alert> alerts, String patientId) {
            return alerts.stream()
                    .filter(alert -> alert.getPatientId().equals(patientId))
                    .collect(Collectors.toList());
        }
        
        /**
         * Runs tests for a batch of patients and exports results
         * @param patients list of patients to test
         * @param csvFilename file to export results to
         */
        public void batchTestAndExport(List<Patient> patients, String csvFilename) {
            // Clear previous test results
            testResults.clear();
            
            // Test each patient
            for (Patient patient : patients) {
                checkAllAlerts(patient);
            }
            
            // Export results
            exportResultsToCSV(csvFilename);
        }
}