package com.cardio_generator.generators;

import java.util.Random;
import com.cardio_generator.outputs.OutputStrategy;
//import com.data_management.Patient;

/**
 * The {@code HealthDataGenerator} class is responsible for generating various health data,
 * including alerts triggered by nurses or patients pressing the alert button near their beds.
 * This class works alongside {@link AlertGenerator} but specifically handles the generation
 * of manual alert triggers.
 */
public class HealthDataGenerator {
    private Random randomGenerator;
    private static final double ALERT_TRIGGER_PROBABILITY = 0.05;
    private static final double ALERT_RESOLUTION_PROBABILITY = 0.1;
    private boolean[] alertStates;

    /**
     * Constructs a {@code HealthDataGenerator} instance.
     */
    public HealthDataGenerator() {
        this.randomGenerator = new Random();
        this.alertStates = new boolean[1000]; // Assuming max 1000 patients
    }

    /**
     * Generates health data for a specified patient, including possible alert button triggers.
     * This method simulates the scenario where a nurse or patient might press the alert button.
     *
     * @param patientId the ID of the patient for which to generate data
     * @param outputStrategy the strategy to use for outputting the generated data
     */
    public void generateData(int patientId, OutputStrategy outputStrategy) {
        // Generate vital signs and other health data
        generateVitalSigns(patientId, outputStrategy);

        // Generate alert button events
        generateAlertButtonEvents(patientId, outputStrategy);
    }

    /**
     * Generates vital signs data for a patient.
     *
     * @param patientId the ID of the patient
     * @param outputStrategy the strategy to use for outputting data
     */
    private void generateVitalSigns(int patientId, OutputStrategy outputStrategy) {
        // Implementation for generating various vital signs
        // This would include blood pressure, heart rate, oxygen saturation, etc.
        // For example:
        long timestamp = System.currentTimeMillis();

        // Generate systolic blood pressure
        double systolic = 120 + (randomGenerator.nextGaussian() * 10);
        outputStrategy.output(patientId, timestamp, "SystolicPressure", String.valueOf(systolic));

        // Generate diastolic blood pressure
        double diastolic = 80 + (randomGenerator.nextGaussian() * 8);
        outputStrategy.output(patientId, timestamp, "DiastolicPressure", String.valueOf(diastolic));

        // Generate oxygen saturation
        double saturation = 98 + (randomGenerator.nextGaussian() * 2);
        saturation = Math.min(100, Math.max(85, saturation)); // Keep within realistic bounds
        outputStrategy.output(patientId, timestamp, "Saturation", String.valueOf(saturation));

        // Generate ECG reading
        double ecg = 1.0 + (randomGenerator.nextGaussian() * 0.1);
        outputStrategy.output(patientId, timestamp, "ECG", String.valueOf(ecg));
    }

    /**
     * Generates alert button events for a patient.
     * This simulates nurse or patient pressing the alert button near the bed.
     *
     * @param patientId the ID of the patient
     * @param outputStrategy the strategy to use for outputting data
     */
    private void generateAlertButtonEvents(int patientId, OutputStrategy outputStrategy) {
        try {
            if (alertStates[patientId]) {
                // If alert is already active, check if it gets resolved
                if (randomGenerator.nextDouble() < ALERT_RESOLUTION_PROBABILITY) {
                    alertStates[patientId] = false;
                    // Output the resolved alert
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "0"); // 0 for resolved
                }
            } else {
                // If alert is not active, check if a new one gets triggered
                if (randomGenerator.nextDouble() < ALERT_TRIGGER_PROBABILITY) {
                    alertStates[patientId] = true;
                    // Output the triggered alert
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "1"); // 1 for triggered
                }
            }
        } catch (Exception e) {
            System.err.println("An error occurred while generating alert button data for patient " + patientId);
            e.printStackTrace();
        }
    }
}