package com.strategy;
import com.alerts.*;
import com.data_management.*;
import java.util.List;

public class OxygenSaturationStrategy implements AlertStrategy {

     // Implement the interface method (required)
     @Override
     public Alert checkAlert(Patient patient, List<PatientRecord> saturationRecords) {
          // A general implementation for single-problem checks
        if (saturationRecords != null && !saturationRecords.isEmpty()) {
          
          Alert lowSaturationAlert = checkLowSaturation(patient, saturationRecords);
          if (lowSaturationAlert != null) return lowSaturationAlert;}
          
          Alert rapidSaturationDropAlert = checkRapidSaturationDrop(patient, saturationRecords);
          if (rapidSaturationDropAlert != null) return rapidSaturationDropAlert;
          return null;
     }
     
     public Alert checkAlert(Patient patient, List<PatientRecord> saturationRecords, List<PatientRecord> systolicRecords) {
          if (saturationRecords != null && !saturationRecords.isEmpty()
          && systolicRecords != null && !systolicRecords.isEmpty()
          ) {                   
          Alert hypotensiveHypoxemiaAlert = checkHypotensiveHypoxemia(patient, systolicRecords, saturationRecords);
          if (hypotensiveHypoxemiaAlert != null) return hypotensiveHypoxemiaAlert;}
          return null;
     }     

     /**
     * Checks if the patient's blood oxygen saturation level is below the critical threshold of 92%.
     * Low blood oxygen saturation can indicate respiratory distress or other serious conditions.
     *
     * @param patient the patient to check for low saturation
     * @param records the list of saturation records to analyze
     */

     private Alert checkLowSaturation(Patient patient, List<PatientRecord> records) {
          if (records.isEmpty()) {
          return null;
          }

          for (int i = 0; i < records.size(); i++) {
          PatientRecord record = records.get(i);
          Double value = record.getMeasurementValue();
          String patientId = String.valueOf(record.getPatientId());

          boolean isThresholdViolated = false;
          String alertMessage = "";

          if (value < 92) {
               isThresholdViolated = true;
               //alertMessage = "Value: " + value + " is below 92? " + (value < 92) +  "Extremely low blood oxygen saturation level";
               alertMessage = "Extremely low blood oxygen saturation level";
          }
          if (isThresholdViolated) {
               Alert alert = new Alert(
                         patientId,
                         alertMessage + " (" + value + "%)",
                         System.currentTimeMillis()
               );
               return alert;  // Exit after finding the first violation to avoid multiple alerts
          }
          }
          return null;
     }

     /**
      * Detects rapid drops in blood oxygen saturation over a 10-minute period.
     * Triggers an alert if a drop of 5% or more is detected, which could indicate
     * deteriorating respiratory function or other acute issues.
     *
     * @param patient the patient to check for saturation drops
     * @param records the list of saturation records to analyze
     */
     private Alert checkRapidSaturationDrop(Patient patient, List<PatientRecord> records) {
          if (records.size() < 2) {
          return null;
          }

          // Get the most recent reading
          PatientRecord latestRecord = records.get(records.size() - 1);
          double latestValue = latestRecord.getMeasurementValue();
          long latestTime = latestRecord.getTimestamp();

          for (int i = 0; i < records.size() - 1; i++) {
          PatientRecord earlierRecord = records.get(i);
          double earlierValue = earlierRecord.getMeasurementValue();
          long earlierTime = earlierRecord.getTimestamp();

          // Calculate the drop
          double dropPercentage = earlierValue - latestValue;

          // Only alert if drop occurred within 10 minutes
          long timeDifference = latestTime - earlierTime;
          if (dropPercentage > 5.0 && timeDifference <= 10 * 60 * 1000) {
               String patientId = String.valueOf(latestRecord.getPatientId());

               Alert alert = new Alert(
                         patientId,
                         "Rapid Oxygen Saturation Drop of " + String.format("%.1f", dropPercentage) + "% in 10 minutes",
                         System.currentTimeMillis()
               );
               return alert;
          }
          }
          return null;
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
     private Alert checkHypotensiveHypoxemia (Patient patient, List<PatientRecord> systolicRecords,
                                             List<PatientRecord> saturationRecords){
          if (systolicRecords.isEmpty() || saturationRecords.isEmpty()) {
          return null;
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
                              return alert; // Exit after finding the first occurrence
                         }
                    }
               }
          }
          }
          return null;
     }

}
