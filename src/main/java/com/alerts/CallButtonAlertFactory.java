package com.alerts;
import java.util.*;
import com.data_management.*;
import com.strategy.*;
import com.strategy.CallButtonAlertStrategy;

public class CallButtonAlertFactory extends AlertFactory {
    private List<AlertStrategy> strategies;

    public CallButtonAlertFactory(DataStorage dataStorage) {

        super(dataStorage); // Call parent constructor
        this.strategies = new ArrayList<>();
        this.strategies.add(new CallButtonAlertStrategy());
    }

    @Override
    public void createAlert(String patientId, String condition, long timestamp) {
        Patient patient = getPatientById(patientId); // Using parent method
        if (patient == null) {
            return;            
        }
       
        // Process regular strategies
        for (AlertStrategy strategy : strategies) {
            List<PatientRecord> alertRecords = getFilteredRecords(patient, "Alert");
                
            CallButtonAlertStrategy callButtonAlertStrategy = (CallButtonAlertStrategy) strategy;
            Alert callButtonAlert = callButtonAlertStrategy.checkAlert(patient, alertRecords);
                
            if (callButtonAlert != null) {
                triggerAlert(callButtonAlert);
                return;
            }
        }
    }
   
}
