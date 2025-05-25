package com.alerts;
import java.util.*;
import com.data_management.Patient;
import com.data_management.PatientRecord;
import com.data_management.DataStorage;
import com.strategy.AlertStrategy;
import com.strategy.OxygenSaturationStrategy;

import java.util.List;

public class BloodOxygenAlertFactory extends AlertFactory{
    private List<AlertStrategy> strategies;

    public BloodOxygenAlertFactory(DataStorage dataStorage) {
        super(dataStorage); // Call parent constructor
        // Initialize strategies
        this.strategies = new ArrayList<>();
        this.strategies.add(new OxygenSaturationStrategy());
    }

    @Override
    public void createAlert(String patientId, String condition, long timestamp) {
        Patient patient = getPatientById(patientId); // Using parent method
        if (patient == null) {
            return;
        }        
        List<PatientRecord> saturationRecords = getFilteredRecords(patient, "Saturation");
        List<PatientRecord> systolicRecords = getFilteredRecords(patient, "SystolicPressure");


        // Process strategies
        for (AlertStrategy strategy : strategies) {
            if (strategy instanceof OxygenSaturationStrategy) {
                // Use the overloaded method to check for combined conditions
                OxygenSaturationStrategy oxygenStrategy = (OxygenSaturationStrategy) strategy;

                Alert hypotensiveHypoxemiaAlert = oxygenStrategy.checkAlert(
                    patient, saturationRecords, systolicRecords);
                
                if (hypotensiveHypoxemiaAlert != null) {
                    triggerAlert(hypotensiveHypoxemiaAlert);
                    return;
                }
                // Regular interface check (will handle low saturation and rapid drops)
                Alert oxygenAlert = oxygenStrategy.checkAlert(patient, saturationRecords);
                
                if (oxygenAlert != null) {
                    triggerAlert(oxygenAlert);
                    return;
                }
            }
        }              
    }
}
