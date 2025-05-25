package com.strategy;
import com.alerts.Alert;
import com.data_management.Patient;
import com.data_management.PatientRecord;
import java.util.List;

/**
 * The {@code AlertChecker} interface defines a standard method for checking
 * specific medical alert conditions. Classes implementing this interface
 * are responsible for analyzing patient records and creating appropriate
 * alerts when conditions are met.
 */
public interface AlertStrategy {

    /**
     * Checks for specific alert conditions in a patient's records.
     * This method should analyze the provided records according to medical criteria
     * and create alerts when conditions warrant.
     *
     * @param patient the patient whose data is being checked
     * @param records the list of patient records to analyze
     * @return an Alert if a condition is detected, or null if no alert is necessary
     */
    Alert checkAlert(Patient patient, List<PatientRecord> records);

}