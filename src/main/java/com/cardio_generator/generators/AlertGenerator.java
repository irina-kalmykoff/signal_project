package com.cardio_generator.generators;

import java.util.Random;

import com.cardio_generator.outputs.OutputStrategy;

public class AlertGenerator implements PatientDataGenerator {

    public static final Random randomGenerator = new Random();
    // 90% chance to resolve
    private static final double RESOLUTION_PROBABILITY = 0.9; //decipher magic number
    // Average rate (alerts per period), adjust based on desired frequency
    //Probability of at least one alert in the period
    private static final double ALERT_RATE_LAMBDA = 0.1; //decipher magic number
    // false = resolved, true = pressed
    private boolean[] alertStates;  // change variable name to camel case

    public AlertGenerator(int patientCount) {
        alertStates = new boolean[patientCount + 1];
    }

    @Override
    public void generate(int patientId, OutputStrategy outputStrategy) {
        try {
            if (alertStates[patientId]) {
                if (randomGenerator.nextDouble() < RESOLUTION_PROBABILITY) {
                    alertStates[patientId] = false;
                    // Output the alert
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "resolved");
                }
            } else {
                //variable names to lower camel case: Lambda >> lambda
                //double lambda = 0.1; remove magic number
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
