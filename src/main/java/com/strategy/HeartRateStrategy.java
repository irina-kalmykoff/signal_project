package com.strategy;
import com.alerts.*;
import java.util.*;
import com.data_management.*;
import java.util.stream.Collectors;

public class HeartRateStrategy implements AlertStrategy {

        // Implement the interface method (required)
        @Override
        public Alert checkAlert(Patient patient, List<PatientRecord> ecgRecords) {

        if (ecgRecords  != null && !ecgRecords.isEmpty()) {
            Alert ecgAbnnormalitiesAlert = checkECGAbnormalities(patient, ecgRecords);
            if (ecgAbnnormalitiesAlert != null) return ecgAbnnormalitiesAlert;                 
            }
        return null;
        }

    /**
     * Analyzes ECG readings to detect abnormal heart electrical activity.
     * This method uses statistical analysis to identify readings that deviate
     * significantly from the patient's recent baseline, which could indicate
     * arrhythmias, conduction disorders, or other cardiac issues.
     *
     * @param patient the patient to check for ECG abnormalities
     */
    private Alert checkECGAbnormalities(Patient patient, List<PatientRecord> ecgRecords) {        

        if (ecgRecords.size() < 20) {
            return null;
        }

        // Get the latest record
        PatientRecord latestRecord = ecgRecords.get(ecgRecords.size() - 1);
        String patientId = String.valueOf(latestRecord.getPatientId());

        // Calculate the mean and standard deviation of recent values
        List<Double> recentValues = ecgRecords.stream()
                .skip(Math.max(0, ecgRecords.size() - 600)) // Use more data if available
                .map(PatientRecord::getMeasurementValue)
                .collect(Collectors.toList());

        double mean = recentValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = Math.sqrt(recentValues.stream()
                .mapToDouble(val -> Math.pow(val - mean, 2))
                .average().orElse(0.0));

        // Define upper and lower bounds (3 standard deviations)
        double upperBound = mean + 2 * stdDev;
        double lowerBound = mean - 2 * stdDev;

        System.out.println("DEBUG - Patient " + patientId + " has " + ecgRecords.size() + " ECG records");
        System.out.println("DEBUG - ECG stats: Mean=" + mean + ", StdDev=" + stdDev);
        System.out.println("DEBUG - Bounds: [" + lowerBound + ", " + upperBound + "]");

        // Check for abnormal patterns in the last few readings
        int abnormalCount = 0;
        boolean hasRapidChange = false;

        // Look at last 5 readings for debugging
        System.out.println("DEBUG - Last 5 ECG values:");
        //List<Double> last5Values = recentValues.subList(Math.max(0, recentValues.size() - 5), recentValues.size());

        // Look at more readings for analysis
        List<Double> lastReadings = recentValues.subList(Math.max(0, recentValues.size() - 10), recentValues.size());
        double prevValue = lastReadings.get(0);

        for (int i = 0; i < lastReadings.size(); i++) {
            double currentValue = lastReadings.get(i);

            // For debugging the last 5
            if (i >= lastReadings.size() - 5) {
                System.out.println("DEBUG - " + String.format("%.3f", currentValue) +
                        (currentValue > upperBound || currentValue < lowerBound ? " OUT OF RANGE" : " in range"));
            }

            // Check for values outside statistical bounds
            if (currentValue > upperBound || currentValue < lowerBound) {
                abnormalCount++;
            }

            // Check for rapid changes between consecutive readings
            if (i > 0) {
                double change = Math.abs(currentValue - prevValue);
                double absoluteThreshold = 0.05; // Minimum absolute change to consider
                double relativeThreshold = 2.5 * stdDev; // Relative to data variability

                if (change > Math.max(absoluteThreshold, relativeThreshold)) {
                    hasRapidChange = true;
                }
            }

            prevValue = currentValue;
        }

        // Check for both statistical outliers and pattern-based abnormalities
        boolean abnormalPattern = checkForAbnormalPattern(lastReadings, mean, stdDev);

        // Trigger alert if multiple abnormal readings or rapid changes or pattern detected
        if ((abnormalCount >= 3) ||
                (hasRapidChange && abnormalCount >= 1) ||
                (abnormalPattern && abnormalCount >= 1))
        {
            Alert alert = new Alert(
                    patientId,
                    "ECG Abnormality: " +
                            (abnormalCount >= 2 ? abnormalCount + " readings outside expected range" :
                                    hasRapidChange ? "Rapid fluctuations detected" : "Abnormal pattern detected"),
                    System.currentTimeMillis()
            );
            return alert;
        }
        return null;
    }

    /**
     * Checks for specific abnormal patterns in ECG data
     * This is a simplified implementation that looks for certain sequences
     * that might indicate arrhythmias or other cardiac issues
     */
    private boolean checkForAbnormalPattern(List<Double> readings, double mean, double stdDev) {
        // Check for alternating high-low pattern (potential indicator of certain arrhythmias)
        boolean alternatingPattern = true;
        boolean highToLow = readings.get(0) > readings.get(1);

        for (int i = 1; i < readings.size() - 1; i++) {
            boolean currentHighToLow = readings.get(i) > readings.get(i + 1);
            if (currentHighToLow == highToLow) {
                alternatingPattern = false;
                break;
            }
            highToLow = currentHighToLow;
        }

        // Check for flatline pattern (multiple consecutive values very close to each other)
        boolean flatlinePattern = true;
        double threshold = stdDev * 0.2; // Very small variation threshold

        for (int i = 0; i < readings.size() - 1; i++) {
            if (Math.abs(readings.get(i) - readings.get(i + 1)) > threshold) {
                flatlinePattern = false;
                break;
            }
        }

        // Check for consistent trend in one direction
        boolean consistentTrend = true;
        boolean increasing = readings.get(0) < readings.get(1);

        for (int i = 1; i < readings.size() - 1; i++) {
            boolean currentIncreasing = readings.get(i) < readings.get(i + 1);
            if (currentIncreasing != increasing) {
                consistentTrend = false;
                break;
            }
        }

        return alternatingPattern || flatlinePattern || consistentTrend;
    }

}
