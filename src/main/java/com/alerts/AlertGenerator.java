package com.alerts;
import java.util.*;
import com.data_management.*;


/**
 * The {@code AlertGenerator} class is responsible for monitoring patient data
 * and generating alerts when certain predefined conditions are met. This class
 * relies on a {@link DataStorage} instance to access patient data and evaluate
 * it against specific health criteria.
 */
public class AlertGenerator {
    private DataStorage dataStorage;
    private boolean[] alertStates;
    private Random randomGenerator;
    private static final double RESOLUTION_PROBABILITY = 0.1;
    private static final double ALERT_RATE_LAMBDA = 0.1;

    // Add factories if you want to use them in the future
    private BloodOxygenAlertFactory bloodOxygenAlertFactory;
    private BloodPressureAlertFactory bloodPressureAlertFactory;
    private ECGAlertFactory ecgAlertFactory;
    private CallButtonAlertFactory callButtonAlertFactory;

     /**
     * Constructs an {@code AlertGenerator} with a specified {@code DataStorage}.
     * The {@code DataStorage} is used to retrieve patient data that this class
     * will monitor and evaluate.
     *
     * @param dataStorage the data storage system that provides access to patient data
     */
    public AlertGenerator(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
        this.alertStates = new boolean[1000]; // Assuming max 1000 patients
        this.randomGenerator = new Random();
        
        // Initialize factories if needed
        this.bloodOxygenAlertFactory = new BloodOxygenAlertFactory(dataStorage);
        this.bloodPressureAlertFactory = new BloodPressureAlertFactory(dataStorage);
        this.ecgAlertFactory = new ECGAlertFactory(dataStorage);
        this.callButtonAlertFactory = new CallButtonAlertFactory(dataStorage);
    }


    /**
     * Generates simulated alerts for a patient.
     * This method is used for testing and simulation purposes.
     *
     * @param patientId the ID of the patient to generate alerts for
     * @param outputStrategy the strategy to use for outputting the generated alerts
     */
//    public void generate(int patientId, OutputStrategy outputStrategy) {
//        try {
//            if (alertStates[patientId]) {
//                if (randomGenerator.nextDouble() < RESOLUTION_PROBABILITY) {
//                    alertStates[patientId] = false;
//                    // Output the alert
//                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "0"); // for resolved
//                }
//            } else {
//                double p = -Math.expm1(-ALERT_RATE_LAMBDA);
//                boolean alertTriggered = randomGenerator.nextDouble() < p;
//
//                if (alertTriggered) {
//                    alertStates[patientId] = true;
//                    // Output the alert
//                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "1"); // for triggered
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("An error occurred while generating alert data for patient " + patientId);
//            e.printStackTrace();
//        }
//    }

            /**
         * Evaluates all data for a patient and triggers appropriate alerts.
         * This method serves as the entry point for alert generation and uses
         * the specialized alert factories to check various conditions.
         *
         * @param patient the patient whose data should be evaluated
         */
        public void evaluateData(Patient patient) {
            if (patient == null) {
                return;
            }
            
            // Get the most recent timestamp (for simplicity, use current time)
            long currentTime = System.currentTimeMillis();
            String patientId = String.valueOf(patient.getPatientId());
            
            // Process through each factory to check their specific conditions
            // For blood oxygen monitoring
            bloodOxygenAlertFactory.createAlert(patientId, null, currentTime);
            
            // For blood pressure monitoring
            bloodPressureAlertFactory.createAlert(patientId, null, currentTime);
            
            // For ECG monitoring
            ecgAlertFactory.createAlert(patientId, null, currentTime);
            
            // For call button alerts
            callButtonAlertFactory.createAlert(patientId, null, currentTime);
        }

    }