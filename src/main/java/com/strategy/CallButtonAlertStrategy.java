package com.strategy;
import com.data_management.*;
import com.alerts.*;
import java.util.*;

public class CallButtonAlertStrategy implements AlertStrategy {

    @Override
    public Alert checkAlert(Patient patient, List<PatientRecord> alertRecords) {
        if (alertRecords != null && !alertRecords.isEmpty()) {                   
        Alert callButtonAlert = checkButtonAlert(patient, alertRecords);
        if (callButtonAlert != null) return callButtonAlert;}
        return null;
   }     


    /**
     * Monitors for manually triggered alerts from patients or staff.
     * These alerts are directly generated when a patient or staff member
     * presses a call button, indicating a need for assistance that may not
     * be captured by vital sign monitoring.
     *
     * @param patient the patient to check for triggered alerts
     */
    private Alert checkButtonAlert(Patient patient, List<PatientRecord> alertRecords) {
        // Get all alert records
        // List<PatientRecord> alertRecords = getFilteredRecords(patient, "Alert");

        if (alertRecords.isEmpty()) {
            return null;
        }

        // Check the most recent alert record
        PatientRecord latestAlert = alertRecords.get(alertRecords.size() - 1);
        double alertValue = latestAlert.getMeasurementValue();
        String patientId = String.valueOf(latestAlert.getPatientId());

        // Only pass through "triggered" alerts, ignore "resolved" ones
        if (alertValue == 1.0) { // 1.0 represents "triggered"
            Alert alert = new Alert(
                    patientId,
                    "Call Button Alert: Patient or Staff Requires Assistance",
                    latestAlert.getTimestamp()
            );
            return alert;
        }
    return null;
    }
}
