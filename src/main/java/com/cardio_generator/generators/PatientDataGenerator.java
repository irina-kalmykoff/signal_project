package com.cardio_generator.generators;

import com.cardio_generator.outputs.OutputStrategy;

/**
 * Defines the contract for generating patient health data.
 */

public interface PatientDataGenerator {
    /**
     * Generates patient health data and outputs it using the specified strategy.
     * <p>
     * Concrete implementations should define:
     * <ul>
     *   <li>The type of medical data being generated</li>
     *   <li>The generation frequency/pattern</li>
     *   <li>Any data-specific calculation logic</li>
     * </ul>
     * </p>
     *
     * @param patientId      the unique identifier of the patient for whom data is being generated
     * @param outputStrategy the output mechanism to use for delivering the generated data
     *                       (e.g., console, file, or network output)
     */
    void generate(int patientId, OutputStrategy outputStrategy);
}
