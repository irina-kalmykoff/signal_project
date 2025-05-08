package com.cardio_generator.generators;

import java.util.Random;

import com.cardio_generator.outputs.OutputStrategy;

/**
 * Simulates blood oxygen saturation (SpO2) data for patients.
 * <p>
 * This generator creates realistic blood oxygen saturation values that:
 * <ul>
 *   <li>Stay within medically realistic ranges (90-100%)</li>
 *   <li>Show small variations over time</li>
 *   <li>Maintain patient-specific baseline values</li>
 * </ul>
 * </p>
 * <p>
 * SpO2 (peripheral oxygen saturation) represents the percentage of hemoglobin
 * binding sites in the bloodstream occupied by oxygen. Normal values for healthy
 * individuals typically range from 95% to 100%.
 * </p>
 * <p>
 * This class implements the PatientDataGenerator interface to generate data and send it using a specified output stragety.
 */
public class BloodSaturationDataGenerator implements PatientDataGenerator {

    // Random number generator for creating variations in saturation values
    private static final Random random = new Random();

    /**
     * Stores the most recently generated saturation value for each patient.
     * Used to create realistic transitions between subsequent readings.
    */
    private int[] lastSaturationValues;

    /**
     * Creates a new BloodSaturationDataGenerator for the specified number of patients.
     * <p>
     * Initializes each patient with a random baseline saturation value between 95% and 100%,
     * representing normal oxygen saturation levels in healthy individuals.
     * </p>
     * 
     * @param patientCount The maximum number of patients to generate data for
    */
    public BloodSaturationDataGenerator(int patientCount) {
        lastSaturationValues = new int[patientCount + 1];

        // Initialize with baseline saturation values for each patient
        for (int i = 1; i <= patientCount; i++) {
            lastSaturationValues[i] = 95 + random.nextInt(6); // Initializes with a value between 95 and 100
        }
    }

    /**
     * Generates blood oxygen saturation data for a specific patient.
     * <p>
     * This method:
     * <ol>
     *   <li>Creates a small random variation from the patient's last saturation value</li>
     *   <li>Ensures the new value stays within the medically realistic range (90-100%)</li>
     *   <li>Updates the stored value for the patient</li>
     *   <li>Outputs the new value through the provided strategy</li>
     * </ol>
     * </p>
     * <p>
     * The small fluctuations (-1, 0, or +1) simulate the minor changes that occur in real-time
     * monitoring of oxygen saturation levels.
     * </p>
     *
     * @param patientId The ID of the patient to generate data for
     * @param outputStrategy The strategy to use for outputting the generated data
    */
    @Override
    public void generate(int patientId, OutputStrategy outputStrategy) {
        try {
            // Simulate blood saturation values
            int variation = random.nextInt(3) - 1; // -1, 0, or 1 to simulate small fluctuations
            int newSaturationValue = lastSaturationValues[patientId] + variation;

            // Ensure the saturation stays within a realistic and healthy range
            newSaturationValue = Math.min(Math.max(newSaturationValue, 90), 100);
            lastSaturationValues[patientId] = newSaturationValue;
            outputStrategy.output(patientId, System.currentTimeMillis(), "Saturation",
                    Double.toString(newSaturationValue) + "%");
        } catch (Exception e) {
            System.err.println("An error occurred while generating blood saturation data for patient " + patientId);
            e.printStackTrace(); // This will print the stack trace to help identify where the error occurred.
        }
    }
}
