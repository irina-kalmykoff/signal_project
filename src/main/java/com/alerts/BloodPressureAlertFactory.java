package com.alerts;
import java.util.*;
import com.data_management.*;
import com.strategy.BloodPressureStrategy;
import com.strategy.AlertStrategy;


public class BloodPressureAlertFactory extends AlertFactory{
    private List<AlertStrategy> strategies;
    
    public BloodPressureAlertFactory(DataStorage dataStorage) {
        super(dataStorage); // Call parent constructor
        // Initialize strategies
        this.strategies = new ArrayList<>();
        this.strategies.add(new BloodPressureStrategy());
        
    }

    
    @Override
    public void createAlert(String patientId, String condition, long timestamp) {
        Patient patient = getPatientById(patientId); // Using parent method
        if (patient == null) {
            return;
        }
        // Process regular strategies
        for (AlertStrategy strategy : strategies) {
            List<PatientRecord> systolicRecords = getFilteredRecords(patient, "SystolicPressure");
            List<PatientRecord> diastolicRecords = getFilteredRecords(patient, "DiastolicPressure");
            
            BloodPressureStrategy bpStrategy = (BloodPressureStrategy) strategy;
            Alert alert = bpStrategy.checkAlert(patient, systolicRecords, diastolicRecords);
            
            if (alert != null) {
                triggerAlert(alert);
                return;
            }

        // // Check blood pressure trends
        // checkPressureTrend(patient, systolicRecords, "Systolic");
        // checkPressureTrend(patient, diastolicRecords, "Diastolic");
        // // Check for blood pressure thesholds
        // checkBloodPressureThreshold(patient, systolicRecords, "Systolic");
        // checkBloodPressureThreshold(patient, diastolicRecords, "Diastolic");
    }
    }

}
