package com.alerts;
import java.util.*;
import java.util.stream.Collectors;
import com.cardio_generator.outputs.OutputStrategy;

import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;


import java.util.List;

/**
 * The {@code AlertGenerator} class is responsible for monitoring patient data
 * and generating alerts when certain predefined conditions are met. This class
 * relies on a {@link DataStorage} instance to access patient data and evaluate
 * it against specific health criteria.
 */
public class AlertGenerator {
    //private DataStorage dataStorage;
    private boolean[] alertStates;
    private Random randomGenerator;
    private static final double RESOLUTION_PROBABILITY = 0.1;
    private static final double ALERT_RATE_LAMBDA = 0.1;

    /**
     * Constructs an {@code AlertGenerator} with a specified {@code DataStorage}.
     * The {@code DataStorage} is used to retrieve patient data that this class
     * will monitor and evaluate.
     *
     * @param dataStorage the data storage system that provides access to patient
     *                    data
     */
    public AlertGenerator(DataStorage dataStorage) {
       // this.dataStorage = dataStorage;
        this.alertStates = new boolean[1000]; // Assuming max 1000 patients
        this.randomGenerator = new Random();
    }

    /**
     * Evaluates the specified patient's data to determine if any alert conditions
     * are met. If a condition is met, an alert is triggered via the
     * {@link #triggerAlert}
     * method. This method should define the specific conditions under which an
     * alert
     * will be triggered.
     *
     * @param patient the patient data to evaluate for alert conditions
     */
    public void evaluateData(Patient patient) {
        // Calling the helper methods
        checkBloodPressure(patient); // 1 method for all blood-related checks
        checkECGAbnormalities(patient);
        checkTriggeredAlert(patient);
    }


    /**
     * Helper method to get filtered and sorted patient records for the latest 10 minutes for  a specific type
     *
     * @param patient    the patient whose records to retrieve
     * @param recordType the type of record to filter for (e.g., "SystolicPressure")
     * @return a list of patient records of the specified type, sorted by timestamp
     */
    private List<PatientRecord> getFilteredRecords(Patient patient, String recordType) {

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
     * Coordinates all blood pressure and oxygenation related alert checks for a patient.
     * This method retrieves the relevant vital sign records and delegates to specialized
     * check methods for different alert conditions.
     *
     * The method performs comprehensive monitoring by checking:
     * - Blood pressure trends (increasing or decreasing)
     * - Blood pressure threshold violations
     * - Low oxygen saturation levels
     * - Rapid drops in oxygen saturation
     * - Combined hypotensive hypoxemia condition
     *
     * @param patient the patient to evaluate for cardiovascular and respiratory alerts
     */
    private void checkBloodPressure(Patient patient) {
        // Get filtered records for each pressure type
        List<PatientRecord> systolicRecords = getFilteredRecords(patient, "SystolicPressure");
        List<PatientRecord> diastolicRecords = getFilteredRecords(patient, "DiastolicPressure");
        List<PatientRecord> saturationRecords = getFilteredRecords(patient, "Saturation");

        // Check blood pressure trends
        checkPressureTrend(patient, systolicRecords, "Systolic");
        checkPressureTrend(patient, diastolicRecords, "Diastolic");
        // Check for blood pressure thesholds
        checkBloodPressureThreshold(patient, systolicRecords, "Systolic");
        checkBloodPressureThreshold(patient, diastolicRecords, "Diastolic");
        // Check saturation
        checkLowSaturation(patient, saturationRecords);
        checkRapidSaturationDrop(patient, saturationRecords);
        // CHeck for hypotensive hypoxemia
        checkHypotensiveHypoxemia(patient, systolicRecords, saturationRecords);

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
    private void checkPressureTrend(Patient patient, List<PatientRecord> records, String pressureType) {
        // We need at least 3 readings to detect a trend
        if (records.size() < 3) {
            return;
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
                triggerAlert(alert);
                return;
            }

            // Check for decreasing trend
            if (previousValue - currentValue > 10 && oldestValue - previousValue > 10) {

                Alert alert = new Alert(
                        String.valueOf(patientId),
                        "Decreasing " + pressureType + " Blood Pressure Trend",
                        System.currentTimeMillis()
                );
                triggerAlert(alert);
                return;
            }
        }
    }

    private void checkBloodPressureThreshold(Patient patient, List<PatientRecord> records, String pressureType) {
        if (records.isEmpty()) {
            return;
        }
        for (int i = 0; i < records.size(); i++) {
            PatientRecord record = records.get(i);
            Double value = record.getMeasurementValue();
            String patientId = String.valueOf(record.getPatientId());

            boolean isThresholdViolated = false;
            String alertMessage = "";

            if (pressureType.equals("Systolic") && (value > 180 || value < 90)) {
                isThresholdViolated = true;
                alertMessage = value > 180 ? "Extremely high systolic pressure" : "Extremely low systolic pressure";
            } else if (pressureType.equals("Diastolic") && (value > 120 || value < 60)) {
                isThresholdViolated = true;
                alertMessage = value > 120 ? "Extremely high diastolic pressure" : "Extremely low diastolic pressure";
            }

            if (isThresholdViolated) {
                Alert alert = new Alert(
                        patientId,
                        alertMessage + " (" + value + " mmHg)",
                        System.currentTimeMillis()
                );
                triggerAlert(alert);
                return;  // Exit after finding the first violation to avoid multiple alerts
            }
        }

    }

    /**
     * Checks if the patient's blood oxygen saturation level is below the critical threshold of 92%.
     * Low blood oxygen saturation can indicate respiratory distress or other serious conditions.
     *
     * @param patient the patient to check for low saturation
     * @param records the list of saturation records to analyze
     */

    private void checkLowSaturation(Patient patient, List<PatientRecord> records) {
        if (records.isEmpty()) {
            return;
        }

        for (int i = 0; i < records.size(); i++) {
            PatientRecord record = records.get(i);
            Double value = record.getMeasurementValue();
            String patientId = String.valueOf(record.getPatientId());

            boolean isThresholdViolated = false;
            String alertMessage = "";

            if (value < 92) {
                isThresholdViolated = true;
                alertMessage = "Extremely low blood oxygen saturation level";
            }
            if (isThresholdViolated) {
                Alert alert = new Alert(
                        patientId,
                        alertMessage + " (" + value + "%)",
                        System.currentTimeMillis()
                );
                triggerAlert(alert);
                return;  // Exit after finding the first violation to avoid multiple alerts
            }
        }
    }

    /**
     * Detects rapid drops in blood oxygen saturation over a 10-minute period.
     * Triggers an alert if a drop of 5% or more is detected, which could indicate
     * deteriorating respiratory function or other acute issues.
     *
     * @param patient the patient to check for saturation drops
     * @param records the list of saturation records to analyze
     */
    private void checkRapidSaturationDrop(Patient patient, List<PatientRecord> records) {
        if (records.size() < 2) {
            // Need at least 2 readings to detect a drop
            return;
        }
        // Get the most recent reading
        PatientRecord latestRecord = records.get(records.size() - 1);
        double latestValue = latestRecord.getMeasurementValue();
       // long latestTime = latestRecord.getTimestamp();

        // Find the highest reading in the 10-minute window
        double highestValueInWindow = latestValue; // Find the maximum and the minimum to see the largest delta
        double lowestValueInWindow = latestValue; // Find the maximum and the minimum to see the largest delta

        for (PatientRecord record : records) {
                double value = record.getMeasurementValue();
                if (value > highestValueInWindow) {
                    highestValueInWindow = value;
                }
                if (value < lowestValueInWindow) {
                    lowestValueInWindow = value;
                }
        }

        // Calculate the percentage drop
        double dropPercentage = highestValueInWindow - lowestValueInWindow;
        if (dropPercentage > 5.0) {
            String patientId = String.valueOf(latestRecord.getPatientId());

            Alert alert = new Alert(
                    patientId,
                    "Rapid Oxygen Saturation Drop of " + String.format("%.1f", dropPercentage) + "% in 10 minutes",
                    System.currentTimeMillis()
            );
            triggerAlert(alert);
        }
    }

    /**
     * Checks for the dangerous condition of hypotensive hypoxemia, which occurs when
     * a patient has both low blood pressure (systolic < 90 mmHg) and low oxygen saturation
     * (< 92%) within a short time period. This combination can indicate severe clinical
     * deterioration requiring immediate intervention.
     *
     * @param patient the patient to check
     * @param systolicRecords the list of systolic blood pressure records
     * @param saturationRecords the list of oxygen saturation records
     */
    private void checkHypotensiveHypoxemia (Patient patient, List<PatientRecord> systolicRecords,
                                            List<PatientRecord> saturationRecords){
        if (systolicRecords.isEmpty() || saturationRecords.isEmpty()) {
            return;
        }
       // boolean isHypotensiveHypoxemia = false;
        String patientId = String.valueOf(systolicRecords.get(systolicRecords.size()-1).getPatientId());

        for (PatientRecord systolicPressure : systolicRecords) {
            double systolicPressureValue = systolicPressure.getMeasurementValue();
            if (systolicPressureValue < 90) {

                long systolicPressureTimestamp = systolicPressure.getTimestamp();
                long oneMinuteBack = systolicPressureTimestamp - (1 * 60 * 1000);
                long oneMinuteForward = systolicPressureTimestamp + (1 * 60 * 1000);

                for (PatientRecord saturation : saturationRecords) {
                    long saturationTimestamp = saturation.getTimestamp();
                    if (saturationTimestamp >= oneMinuteBack && saturationTimestamp <= oneMinuteForward) {
                        double saturationValue = saturation.getMeasurementValue();

                        if (saturationValue < 92) {
                            Alert alert = new Alert(
                                    patientId,
                                    "CRITICAL: Hypotensive Hypoxemia Detected (BP: " +
                                            String.format("%.1f", systolicPressureValue) + " mmHg, O2 Sat: " +
                                            String.format("%.1f", saturationValue) + "%)",
                                    System.currentTimeMillis()
                            );
                            triggerAlert(alert);
                            return; // Exit after finding the first occurrence
                        }
                    }
                }
            }
        }
    }

    /**
     * Analyzes ECG readings to detect abnormal heart electrical activity.
     * This method uses statistical analysis to identify readings that deviate
     * significantly from the patient's recent baseline, which could indicate
     * arrhythmias, conduction disorders, or other cardiac issues.
     *
     * @param patient the patient to check for ECG abnormalities
     */
    private void checkECGAbnormalities (Patient patient){
            List<PatientRecord> ecgRecords = getFilteredRecords(patient, "ECG");

            if (ecgRecords.size() < 20) {
                return;
            }

            // Get the latest record
            PatientRecord latestRecord = ecgRecords.get(ecgRecords.size() - 1);
            String patientId = String.valueOf(latestRecord.getPatientId());
            double latestValue = latestRecord.getMeasurementValue();

            // Calculate the mean and standard deviation of recent values
            List<Double> recentValues = ecgRecords.stream()
                    .skip(Math.max(0, ecgRecords.size() - 30)) // Last 30 readings
                    .map(PatientRecord::getMeasurementValue)
                    .collect(Collectors.toList());

            double mean = recentValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double stdDev = Math.sqrt(recentValues.stream()
                    .mapToDouble(val -> Math.pow(val - mean, 2))
                    .average().orElse(0.0));

            // Define upper and lower bounds (3 standard deviations)
            double upperBound = mean + 3 * stdDev;
            double lowerBound = mean - 3 * stdDev;

            // Check if the latest value exceeds bounds
            if (latestValue > upperBound || latestValue < lowerBound) {
                Alert alert = new Alert(
                        patientId,
                        "ECG Abnormality: Value " + String.format("%.2f", latestValue) +
                                " outside expected range [" + String.format("%.2f", lowerBound) +
                                ", " + String.format("%.2f", upperBound) + "]",
                        System.currentTimeMillis()
                );
                triggerAlert(alert);
            }
    }


    public void generate(int patientId, OutputStrategy outputStrategy) {
        try {
            if (alertStates[patientId]) {
                if (randomGenerator.nextDouble() < RESOLUTION_PROBABILITY) {
                    alertStates[patientId] = false;
                    // Output the alert
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "0"); // for resolved
                }
            } else {
                double p = -Math.expm1(-ALERT_RATE_LAMBDA);
                boolean alertTriggered = randomGenerator.nextDouble() < p;

                if (alertTriggered) {
                    alertStates[patientId] = true;
                    // Output the alert
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "1"); // for triggered
                }
            }
        } catch (Exception e) {
            System.err.println("An error occurred while generating alert data for patient " + patientId);
            e.printStackTrace();
        }
    }

    /**
     * Monitors for manually triggered alerts from patients or staff.
     * These alerts are directly generated when a patient or staff member
     * presses a call button, indicating a need for assistance that may not
     * be captured by vital sign monitoring.
     *
     * @param patient the patient to check for triggered alerts
     */
    private void checkTriggeredAlert(Patient patient) {
        // Get all alert records
        List<PatientRecord> alertRecords = getFilteredRecords(patient, "Alert");

        if (alertRecords.isEmpty()) {
            return;
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
            triggerAlert(alert);
        }
    }


        /**
         * Triggers an alert for the monitoring system. This method can be extended to
         * notify medical staff, log the alert, or perform other actions. The method
         * currently assumes that the alert information is fully formed when passed as
         * an argument.
         *
         * @param alert the alert object containing details about the alert condition
         */
        protected void triggerAlert (Alert alert){
            // Implementation might involve logging the alert or notifying staff
            System.out.println("ALERT: " + alert.getCondition() +
                    " for Patient ID: " + alert.getPatientId() +
                    " at " + new java.util.Date(alert.getTimestamp()));
        }

    }