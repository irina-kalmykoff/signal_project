package com.alerts;
import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;
import com.strategy.AlertStrategy;
import com.strategy.HeartRateStrategy;
import java.util.*;


public class ECGAlertFactory extends AlertFactory {
    private List<AlertStrategy> strategies;

    public ECGAlertFactory(DataStorage dataStorage) {

        super(dataStorage); // Call parent constructor
        this.strategies = new ArrayList<>();
        this.strategies.add(new HeartRateStrategy());
    }

    @Override
    public void createAlert(String patientId, String condition, long timestamp) {
        Patient patient = getPatientById(patientId); // Using parent method
        if (patient == null) {
            return;            
        }

        // Process regular strategies
        for (AlertStrategy strategy : strategies) {
            List<PatientRecord> ecgRecords = getFilteredRecords(patient, "ECG");
                    
                HeartRateStrategy hearRateStrategy = (HeartRateStrategy) strategy;
                Alert alert = hearRateStrategy.checkAlert(patient, ecgRecords);
                    
                if (alert != null) {
                    triggerAlert(alert);
                    return;
                }
            }
    }

   
}
