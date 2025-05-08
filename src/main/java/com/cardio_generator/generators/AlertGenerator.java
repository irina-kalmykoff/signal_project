package com.cardio_generator.generators;

import java.util.Random;

import com.cardio_generator.outputs.OutputStrategy;

/**
 * Simulates random patient alerts for healthcare monitoring systems.
 * This class implements the PatientDataGenerator interface to 
 * <p>
 * This generator implements a stochastic model for patient alerts that:
 * <ul>
 *  <li>Triggers new alerts based on a Poissson process model</li>
 *  <li>Resolves existing alerts with a configurable probability</li>
 *  <Maintains separate alert states for each patient</li>
 * </ul>
 * </p>
 * <p>
 * The alert generation follows the following rules:
 * <ul>
 *  <li>If a patient has no active alert, there is a small probability one will be triggered</li>
 *  <li>If a patient has an active alert, there is a high probability it will be resolved</li>
 * </ul>
 * </p>
 * <p>
 * This class is designed to simulate realistic alert patterns for education and testing purposes.
 * </p>
 */
public class AlertGenerator implements PatientDataGenerator {

    // Generate random number for alert state changes.
    public static final Random randomGenerator = new Random();

    /** 
     * Probability that an active alert will be resolved during a check cycle.
     * Value of 0.9 means 90% of alerts will be resolved each cycle.
     */
    private static final double RESOLUTION_PROBABILITY = 0.9; //decipher magic number

    // Average rate (alerts per period), adjust based on desired frequency
    // Probability of at least one alert in the period
    private static final double ALERT_RATE_LAMBDA = 0.1; //decipher magic number

    // Current alert state for each patient.
    // false = resolved/no active alert, true = pressed/active alert
    private boolean[] alertStates;  // change variable name to camel case

    /**
     * Creates a new AlertGenerator for the specified number of patients.
     * 
     * @param patientCount The maximum number of patients to simulate alerts for
     */
    public AlertGenerator(int patientCount) {
        alertStates = new boolean[patientCount + 1];
    }

    /**
     * Generates alert data for a specific patient.
     * <p>
     * This method checks the current alert state for the patient and either:
     * <ul>
     *   <li>Attempts to resolve an existing alert with probability RESOLUTION_PROBABILITY</li>
     *   <li>Attempts to trigger a new alert with probability based on Poisson process</li>
     * </ul>
     * </p>
     * <p>
     * The method uses a mathematical model based on the Poisson distribution to determine
     * if a new alert should be triggered, making alert generation realistically random
     * but controlled.
     * </p>
     *
     * @param patientId The ID of the patient to generate alert data for
     * @param outputStrategy The strategy to use for outputting generated alert data
    */
    @Override
    public void generate(int patientId, OutputStrategy outputStrategy) {
        try {
            if (alertStates[patientId]) { // Patient currently has an active alert.
                // Check if the alert should be resolved.
                if (randomGenerator.nextDouble() < RESOLUTION_PROBABILITY) {
                    alertStates[patientId] = false;
                    // Output the alert resolution.
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "resolved");
                }
            } else { // Patient has no active alert. Check if an alert should be triggered.
                // Calculate probability of at least one event in Poisson process.
                double p = -Math.expm1(-ALERT_RATE_LAMBDA);
                boolean alertTriggered = randomGenerator.nextDouble() < p;

                if (alertTriggered) {
                    alertStates[patientId] = true;
                    // Output the alert
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "triggered");
                }
            }
        } catch (Exception e) {
            System.err.println("An error occurred while generating alert data for patient " + patientId);
            e.printStackTrace();
        }
    }
}
