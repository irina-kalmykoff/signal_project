package com.cardio_generator.outputs;

/**
 * Defines an output strategy for handling patient health data.
 * <p>
 * Implementations of this interface specify how generated medical data should be
 * persisted or transmitted. Common strategies include:
 * <ul>
 *   <li>Console output</li>
 *   <li>File storage</li>
 *   <li>Network transmission (TCP/WebSocket)</li>
 *   <li>Database storage</li>
 * </ul>
 * </p>
 */

public interface OutputStrategy {
    /**
     * Handles the output of generated patient data.
     * <p>
     * This method is called whenever new health data is generated, and implementations
     * should define how to process and store/transmit the data elements.
     * </p>
     *
     * @param patientId  the unique identifier of the patient (positive integer)
     * @param timestamp  the exact time the measurement was taken, in milliseconds since epoch
     * @param label      the type of data being recorded (e.g., "HeartRate", "BloodPressure", "Alert")
     * @param data       the actual measurement value or message (format depends on label)
     */
    void output(int patientId, long timestamp, String label, String data);
}
