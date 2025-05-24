package com.cardio_generator.outputs;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements the OutputStrategy interface to write patient data to files.
 * <p>
 * This strategy organizes output by creating separate files for each data type (label)
 * within a specified base directory. All patient data for a particular type is appended
 * to the corresponding file.
 * </p>
 * <p>
 * For thread safety and performance, this implementation:
 * <ul>
 *   <li>Uses ConcurrentHashMap to cache file paths</li>
 *   <li>Creates the base directory structure if it doesn't exist</li>
 *   <li>Appends data to existing files rather than overwriting</li>
 * </ul>
 * </p>
 * <p>
 * Each line in the output files follows the format:
 * {@code Patient ID: [id], Timestamp: [timestamp], Label: [label], Data: [data]}
 * </p>
 */
public class FileOutputStrategy implements OutputStrategy { //Class name not matching the file name

    // The base directory where output files will be created
    private String baseDirectory; // Change variable name to camelCase (was intially BaseDirectory)

    /** 
     * Thread-safe mapping of data labels to their corresponding file paths.
     * Used to cache file paths and avoid recalculating them for each output operation.
    */
    public final ConcurrentHashMap<String, String> file_map = new ConcurrentHashMap<>();

    /**
     * Creates a new FileOutputStrategy that will write files to the specified directory.
     * 
     * @param baseDirectory The directory where output files should be created
    */
    public FileOutputStrategy(String baseDirectory) { // Adjust to match the updated class name

        this.baseDirectory = baseDirectory; // BaseDirectory is changed into baseDirectory to give the variable a name in camelCase.
    }

    /**
     * Writes patient data to a file specific to the data label.
     * <p>
     * This method:
     * <ol>
     *   <li>Ensures the base directory exists, creating it if necessary</li>
     *   <li>Determines the appropriate file path for the data label, caching it for future use</li>
     *   <li>Appends the formatted data to the file</li>
     * </ol>
     * </p>
     * <p>
     * All output for a specific label (e.g., "ECG", "BloodPressure") is written to its own file
     * named "[label].txt" within the base directory.
     * </p>
     *
     * @param patientId The ID of the patient the data belongs to
     * @param timestamp The time the data was recorded (milliseconds since epoch)
     * @param label The type of data being recorded (e.g., "ECG", "BloodPressure")
     * @param data The value of the recorded data
    */
    @Override
    public void output(int patientId, long timestamp, String label, String data) {
        
        boolean directoryCreated = true;

        try {
            // Create the directory
            Files.createDirectories(Paths.get(baseDirectory));
        } catch (IOException e) {
            System.err.println("Error creating base directory: " + e.getMessage());
            directoryCreated = false;
        }
        // Set the FilePath variable
        String FilePath = file_map.computeIfAbsent(label, k -> Paths.get(baseDirectory, label + ".txt").toString());

        // Write the data to the file
        try (PrintWriter out = new PrintWriter(
                Files.newBufferedWriter(Paths.get(FilePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
            out.printf("Patient ID: %d, Timestamp: %d, Label: %s, Data: %s%n", patientId, timestamp, label, data);
        } catch (Exception e) {
            System.err.println("Error writing to file " + FilePath + ": " + e.getMessage());
        }
    }
}