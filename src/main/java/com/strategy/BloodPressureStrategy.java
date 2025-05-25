package com.strategy;
import com.data_management.*;
import com.alerts.*;
import java.util.List;

public class BloodPressureStrategy implements AlertStrategy {
    private static final double EPSILON = 0.00001;
    // Implement the interface method (required)
    @Override
    public Alert checkAlert(Patient patient, List<PatientRecord> records) {
        // A general implementation or could serve as a fallback
        if (records != null && !records.isEmpty()) {
            String recordType = records.get(0).getRecordType();
            if (recordType.equals("SystolicPressure")) {
                return checkPressureTrend(patient, records, "Systolic");
            }
        }
        return null;
    }

    // default implementation working with systolic and diastolic records to check for alerts    
    public Alert checkAlert(Patient patient, List<PatientRecord> systolicRecords, List<PatientRecord> diastolicRecords) {
        // For compatibility with basic interface
        Alert systolicAlert = checkPressureTrend(patient, systolicRecords, "Systolic");
        if (systolicAlert != null) return systolicAlert;
        
        Alert diastolicAlert = checkPressureTrend(patient, diastolicRecords, "Diastolic");
        if (diastolicAlert != null) return diastolicAlert;

        systolicAlert = checkBloodPressureThreshold(patient, systolicRecords, "Systolic");
        if (systolicAlert != null) return systolicAlert;
        
        diastolicAlert = checkBloodPressureThreshold(patient, diastolicRecords, "Diastolic");
        return diastolicAlert;
    }

    /**
     * Monitors blood pressure readings to detect significant upward or downward trends.
     * This method looks for consistent changes of more than 10 mmHg between consecutive
     * readings, which could indicate an underlying clinical issue requiring attention.
     *
     * The method analyzes sets of three consecutive readings, looking for either:
     * - An increasing trend (each reading at least 10 mmHg higher than the previous)
     * - A decreasing trend (each reading at least 10 mmHg lower than the previous)
     *
     * @param patient the patient to monitor for blood pressure trends
     * @param records the list of blood pressure records to analyze
     * @param pressureType the type of pressure being analyzed ("Systolic" or "Diastolic")
     */

    private Alert checkPressureTrend(Patient patient, List<PatientRecord> records, String pressureType) {
        // We need at least 3 readings to detect a trend
        if (records.size() < 3) {
            return null;
        }

        // Check the last 3 readings for trends
        for (int i = records.size() - 1; i >= 2; i--) {
            PatientRecord current = records.get(i);
            PatientRecord previous = records.get(i - 1);
            PatientRecord oldest = records.get(i - 2);

            double currentValue = current.getMeasurementValue();
            double previousValue = previous.getMeasurementValue();
            double oldestValue = oldest.getMeasurementValue();
            String patientId = String.valueOf(current.getPatientId());

            // Check for increasing trend
            if (currentValue - previousValue > 10 && previousValue - oldestValue > 10) {

                Alert alert = new Alert(
                        String.valueOf(patientId),
                        "Increasing " + pressureType + " Blood Pressure Trend",
                        System.currentTimeMillis()
                );
                return alert;
            }

            // Check for decreasing trend
            if (previousValue - currentValue > 10 && oldestValue - previousValue > 10) {

                Alert alert = new Alert(
                        String.valueOf(patientId),
                        "Decreasing " + pressureType + " Blood Pressure Trend",
                        System.currentTimeMillis()
                );
                return alert;
            }
        }
        return null;
    }

    private Alert checkBloodPressureThreshold(Patient patient, List<PatientRecord> records, String pressureType) {
        if (records.isEmpty()) {
            return null;
        }
        for (int i = 0; i < records.size(); i++) {
            PatientRecord record = records.get(i);
            Double value = record.getMeasurementValue();
            String patientId = String.valueOf(record.getPatientId());

            boolean isThresholdViolated = false;
            String alertMessage = "";
          
            
            if (pressureType.equals("Systolic") && (value > 180 || value < 90 )) {
                isThresholdViolated = true;
                alertMessage = value > 180 ? "Extremely high systolic pressure" : "Extremely low systolic pressure";
            } else if (pressureType.equals("Diastolic") && (value > 120 || value < 60 )) {
                isThresholdViolated = true;
                alertMessage = value > 120 ? "Extremely high diastolic pressure" : "Extremely low diastolic pressure";
            }

            if (isThresholdViolated) {
                Alert alert = new Alert(
                        patientId,
                        alertMessage + " (" + value + " mmHg)",
                        System.currentTimeMillis()
                );
                return alert;  // Exit after finding the first violation to avoid multiple alerts
            }
        }
        return null;
    }
}
