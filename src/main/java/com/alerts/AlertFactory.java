package com.alerts;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;
import com.data_management.Patient;
import com.data_management.DataStorage;
import com.data_management.PatientRecord;

/**
 * Abstract factory class for creating alerts.
 * Serves as the base class for all specific alert factory implementations.
 */
public abstract class AlertFactory {

    protected DataStorage dataStorage;

    /**
     * Constructor that initializes the factory with data storage.
     *
     * @param dataStorage the data storage system that provides access to patient data
     */
    public AlertFactory(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }


    /**
     * Creates and triggers alerts based on the provided patient data.
     *
     * @param patientId The unique identifier for the patient
     * @param condition The medical condition or reading that triggered the alert
     * @param timestamp The time when the alert condition was detected
     */
    public abstract void createAlert(String patientId, String condition, long timestamp);

        /**
     * Helper method to get filtered and sorted patient records for the latest 10 minutes for  a specific type
     *
     * @param patient    the patient whose records to retrieve
     * @param recordType the type of record to filter for (e.g., "SystolicPressure")
     * @return a list of patient records of the specified type, sorted by timestamp
     */
    protected List<PatientRecord> getFilteredRecords(Patient patient, String recordType) {

        // Calculate timestamp for 10 minutes ago
        long tenMinutesAgo = System.currentTimeMillis() - (10 * 60 * 1000);

        // Get the patient's records for the past 10 minutes
        List<PatientRecord> allRecords = patient.getRecords(tenMinutesAgo, Long.MAX_VALUE);

        // Filter to only the specified record type and sort by timestamp
        return allRecords.stream()
                .filter(record -> record.getRecordType().equals(recordType))
                .sorted(Comparator.comparing(PatientRecord::getTimestamp))
                .collect(Collectors.toList());
    }

    /**
     * Triggers an alert for the monitoring system. This method can be extended to
     * notify medical staff, log the alert, or perform other actions. The method
     * currently assumes that the alert information is fully formed when passed as
     * an argument.
     *
     * @param alert the alert object containing details about the alert condition
     */
    protected void triggerAlert(Alert alert){
        // Implementation might involve logging the alert or notifying staff
        System.out.println("ALERT: " + alert.getCondition() +
                " for Patient ID: " + alert.getPatientId() +
                " at " + new java.util.Date(alert.getTimestamp()));
    }

     /**
     * Helper method to get a Patient object from a patient ID string.
     *
     * @param patientId the patient ID as a string
     * @return the Patient object, or null if not found or ID is invalid
     */
    protected Patient getPatientById(String patientId) {
        try {
            int patientIdInt = Integer.parseInt(patientId);
            return dataStorage.getPatient(patientIdInt);
        } catch (NumberFormatException e) {
            System.err.println("Invalid patient ID format: " + patientId);
            return null;
        }
    }
}