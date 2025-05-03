package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility class for generating comprehensive test data files for alert validation
 */
public class TestDataGenerator {
    private static final Random random = new Random();
    private final String outputDirectory;
    
    public TestDataGenerator(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    /**
     * Generate test data files with various scenarios to test all alert conditions
     */
    public void generateTestData() throws IOException {
        // Create directory if it doesn't exist
        Path directoryPath = Paths.get(outputDirectory);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }
        
        // Create test patients with different scenarios
        generateNormalPatient(0);                                 // No alerts
        generateIncreasingTrendPatient(1, 12);          // Increasing trend > 10mmHg (alert)
        generateIncreasingTrendPatient(2, 10);          // Increasing trend = 10mmHg (no alert)
        generateDecreasingTrendPatient(3, 12);          // Decreasing trend > 10mmHg (alert)
        generateDecreasingTrendPatient(4, 10);          // Decreasing trend = 10mmHg (no alert)
        generateHighBloodPressurePatient(5);                      // BP > thresholds (alert)
        generateLowBloodPressurePatient(6);                       // BP < thresholds (alert)
        generateBorderlineHighBPPatient(7);                       // BP = high thresholds (no alert)
        generateBorderlineLowBPPatient(8);                        // BP = low thresholds (no alert)
        generateBorderlineSaturationPatient(9);                   // O2 = 92% (no alert)
        generateLowSaturationPatient(10);                         // O2 < 92% (alert)
        generateRapidSaturationDropPatient(11);                   // O2 drop ≥ 5% in 10min (alert)
        generateExtendedSaturationDropPatient(12);                // O2 drop ≥ 5% over >10min (no alert)
        generateHypotensiveHypoxemiaPatient(13);                  // Low BP & Low O2 together (alert)
        generateSeparatedHypotensiveHypoxemiaPatient(14);         // Low BP & Low O2 far apart (no alert)
        generateAbnormalECGPatient(15);                           // Abnormal ECG (alert)
        generateBorderlineECGPatient(16);                         // Borderline ECG (no alert)
        generateTriggeredAlertPatient(17);                        // Manual alert (alert)
    }
    
    /**
     * Generates data for a patient with normal vital signs
     */
    private void generateNormalPatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        // Generate 10 minutes of stable, very normal data
        for (int i = 0; i < 600; i++) {
            long timestamp = baseTime - (600 - i) * 1000; // Last 10 minutes
            
            // Very stable normal blood pressure (minimal variation)
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", 120.0 + random.nextDouble() * 2 - 1));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 2 - 1));
            
            // Stable normal oxygen
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", 98.0 + random.nextDouble() - 0.5));
            
            // Very stable ECG
            records.add(String.format("%d,%d,%s,%.3f", patientId, timestamp, "ECG", 0.2 + random.nextDouble() * 0.02 - 0.01));
        }
        
        writeToFile("normal_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with increasing blood pressure trend
     */
    private void generateIncreasingTrendPatient(int patientId, double increment) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        // Generate data with normal values except for the last 3 readings
        for (int i = 0; i < 60; i++) {
            long timestamp = baseTime - (60 - i) * 1000; // Last 60 seconds
            
            double systolic;
            if (i < 57) {
                // Normal range values with random variation
                systolic = 120.0 + random.nextDouble() * 10 - 5;
            } else {
                // Create increasing trend in last 3 readings with exact increments
                if (i == 57) {
                    // First reading in the trend sequence - base value
                    systolic = 120.0;
                } else if (i == 58) {
                    // Second reading - exactly increment higher than the first
                    systolic = 120.0 + increment;
                } else { // i == 59
                    // Third reading - exactly increment higher than the second
                    systolic = 120.0 + (2 * increment);
                }
            }
            
            // Format with exactly one decimal place to avoid precision issues
            String formattedSystolic = String.format("%.1f", systolic);
            
            records.add(String.format("%d,%d,%s,%s", patientId, timestamp, "SystolicPressure", formattedSystolic));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 6 - 3));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", 98.0 + random.nextDouble() * 2 - 1));
        }
        
        writeToFile("increasing_trend_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with decreasing blood pressure trend
     */
    private void generateDecreasingTrendPatient(int patientId, double decrement) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        // Generate data with normal values except for the last 3 readings
        for (int i = 0; i < 60; i++) {
            long timestamp = baseTime - (60 - i) * 1000; // Last 60 seconds
            
            double systolic;
            if (i < 57) {
                systolic = 120.0 + random.nextDouble() * 10 - 5;
            } else {
                // Create decreasing trend in last 3 readings with exact decrements
                if (i == 57) {
                    // First reading - high value
                    systolic = 150.0;
                } else if (i == 58) {
                    // Second reading - exactly decrement lower
                    systolic = 150.0 - decrement;
                } else { // i == 59
                    // Third reading - exactly decrement lower again
                    systolic = 150.0 - (2 * decrement);
                }
            }
            
            // Format with exactly one decimal place
            String formattedSystolic = String.format("%.1f", systolic);
            
            records.add(String.format("%d,%d,%s,%s", patientId, timestamp, "SystolicPressure", formattedSystolic));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 6 - 3));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", 98.0 + random.nextDouble() * 2 - 1));
        }
        
        writeToFile("decreasing_trend_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with high blood pressure above critical thresholds
     */
    private void generateHighBloodPressurePatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        for (int i = 0; i < 60; i++) {
            long timestamp = baseTime - (60 - i) * 1000;
            
            // High blood pressure
            double systolic = i < 55 ? 160.0 + random.nextDouble() * 10 : 185.0 + random.nextDouble() * 5;
            double diastolic = i < 55 ? 90.0 + random.nextDouble() * 10 : 125.0 + random.nextDouble() * 5;
            
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", systolic));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", diastolic));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", 98.0 + random.nextDouble() * 2 - 1));
        }
        
        writeToFile("high_bp_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with low blood pressure below critical thresholds
     */
    private void generateLowBloodPressurePatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        for (int i = 0; i < 60; i++) {
            long timestamp = baseTime - (60 - i) * 1000;
            
            // Low blood pressure
            double systolic = i < 55 ? 100.0 + random.nextDouble() * 10 - 5 : 85.0 + random.nextDouble() * 4;
            double diastolic = i < 55 ? 70.0 + random.nextDouble() * 10 - 5 : 55.0 + random.nextDouble() * 4;
            
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", systolic));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", diastolic));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", 98.0 + random.nextDouble() * 2 - 1));
        }
        
        writeToFile("low_bp_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with borderline high blood pressure at critical thresholds
     */
    private void generateBorderlineHighBPPatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        for (int i = 0; i < 60; i++) {
            long timestamp = baseTime - (60 - i) * 1000;
            
            // High blood pressure at exactly the threshold
            double systolic = i < 55 ? 160.0 + random.nextDouble() * 10 : 180.0;
            double diastolic = i < 55 ? 90.0 + random.nextDouble() * 10 : 120.0;
            
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", systolic));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", diastolic));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", 98.0 + random.nextDouble() * 2 - 1));
        }
        
        writeToFile("borderline_high_bp_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with borderline low blood pressure at critical thresholds
     */
    private void generateBorderlineLowBPPatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        for (int i = 0; i < 60; i++) {
            long timestamp = baseTime - (60 - i) * 1000;
            
            // Low blood pressure at exactly the threshold
            double systolic = i < 55 ? 100.0 + random.nextDouble() * 10 - 5 : 90.0;
            double diastolic = i < 55 ? 70.0 + random.nextDouble() * 10 - 5 : 60.0;
            
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", systolic));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", diastolic));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", 98.0 + random.nextDouble() * 2 - 1));
        }
        
        writeToFile("borderline_low_bp_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with borderline oxygen saturation
     */
    private void generateBorderlineSaturationPatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        for (int i = 0; i < 600; i++) {
            long timestamp = baseTime - (600 - i) * 1000;
            
            // Normal BP
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", 120.0 + random.nextDouble() * 10 - 5));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 6 - 3));
            
            // Saturation at exactly 92%
            double saturation = i < 550 ? 98.0 + random.nextDouble() * 2 - 1 : 92.0;
            
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", saturation));
        }
        
        writeToFile("borderline_saturation_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with low oxygen saturation
     */
    private void generateLowSaturationPatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        for (int i = 0; i < 600; i++) {
            long timestamp = baseTime - (600 - i) * 1000;
            
            // Normal BP
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", 120.0 + random.nextDouble() * 10 - 5));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 6 - 3));
            
            // Low oxygen
            double saturation = i < 550 ? 98.0 + random.nextDouble() * 2 - 1 : 
                               (91.0 + random.nextDouble() * 0.9); // Between 91.0 and 91.9
            
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", saturation));
        }
        
        writeToFile("low_oxygen_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with a rapid saturation drop within 10 minutes
     */
    private void generateRapidSaturationDropPatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        // 10 minutes with rapid drop in the middle
        for (int i = 0; i < 600; i++) {
            long timestamp = baseTime - (600 - i) * 1000;
            
            // Normal BP
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", 120.0 + random.nextDouble() * 10 - 5));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 6 - 3));
            
            // Oxygen high, then rapid drop, then stabilizes at lower level
            double saturation;
            if (i < 200) {
                saturation = 99.0 + random.nextDouble() - 0.5;
            } else if (i < 300) {
                saturation = 99.0 - ((i - 200) * 0.06); // Drops from ~99% to ~93% over 100 seconds
            } else {
                saturation = 93.0 + random.nextDouble() * 1 - 0.5;
            }
            
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", saturation));
        }
        
        writeToFile("rapid_drop_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with a saturation drop that happens over more than 10 minutes
     */
    private void generateExtendedSaturationDropPatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        // 15 minutes with slow drop over the entire period
        for (int i = 0; i < 900; i++) {
            long timestamp = baseTime - (900 - i) * 1000; // Last 15 minutes
            
            // Normal BP
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", 120.0 + random.nextDouble() * 10 - 5));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 6 - 3));
            
            // Oxygen slowly dropping over 15 minutes
            double saturation = 99.0 - ((i / 900.0) * 6.0); // Drops from 99% to 93% over 15 minutes
            
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", saturation));
        }
        
        writeToFile("extended_drop_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient exhibiting hypotensive hypoxemia
     */
    private void generateHypotensiveHypoxemiaPatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        // 10 minutes with both low BP and low oxygen at the same time
        for (int i = 0; i < 600; i++) {
            long timestamp = baseTime - (600 - i) * 1000;
            
            // BP drops at the end
            double systolic = i < 550 ? 120.0 + random.nextDouble() * 10 - 5 : 
                             (88.0 + random.nextDouble() * 1);
            
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", systolic));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 6 - 3));
            
            // Oxygen drops at the same time
            double saturation = i < 550 ? 98.0 + random.nextDouble() * 2 - 1 : 
                               (91.0 + random.nextDouble() * 0.9);
            
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", saturation));
        }
        
        writeToFile("hypotensive_hypoxemia_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with both low BP and low oxygen but not at the same time
     */
    private void generateSeparatedHypotensiveHypoxemiaPatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        // 10 minutes with low BP at start and low oxygen at end (more than 2 minutes apart)
        for (int i = 0; i < 600; i++) {
            long timestamp = baseTime - (600 - i) * 1000;
            
            // Low BP at the beginning
            double systolic = i < 50 ? 88.0 + random.nextDouble() * 1 :
                             120.0 + random.nextDouble() * 10 - 5;
            
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", systolic));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 6 - 3));
            
            // Low oxygen at the end
            double saturation = i > 550 ? 91.0 + random.nextDouble() * 0.9 :
                               98.0 + random.nextDouble() * 2 - 1;
            
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", saturation));
        }
        
        writeToFile("separated_hypotensive_hypoxemia_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with abnormal ECG readings
     */
    private void generateAbnormalECGPatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        // Generate 600 readings (at least 10 minutes worth)
        for (int i = 0; i < 600; i++) {
            long timestamp = baseTime - (600 - i) * 1000;
            
            // Normal vital signs
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", 120.0 + random.nextDouble() * 10 - 5));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 6 - 3));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", 98.0 + random.nextDouble() * 2 - 1));
            
            // Generate many normal ECG values followed by EXTREMELY abnormal values
            double ecg;
            if (i < 550) { // Normal values for most readings
                ecg = 0.2 + random.nextDouble() * 0.02 - 0.01; // Very tight normal range (0.19-0.21)
            } else { // Last 50 readings are extremely abnormal
                // Make values at least 20 standard deviations outside normal range
                ecg = random.nextBoolean() ? 
                      2.0 + random.nextDouble() :  // Very high (~2.0-3.0)
                     -1.0 - random.nextDouble();   // Very low (~ -1.0 to -2.0)
            }
            
            records.add(String.format("%d,%d,%s,%.3f", patientId, timestamp, "ECG", ecg));
        }
        
        writeToFile("abnormal_ecg_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient with borderline ECG readings
     */
    private void generateBorderlineECGPatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        // Generate 30 normal ECG readings, then 30 borderline readings just around 3 standard deviations
        for (int i = 0; i < 600; i++) {
            long timestamp = baseTime - (600 - i) * 1000;
            
            // Normal vital signs
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", 120.0 + random.nextDouble() * 10 - 5));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 6 - 3));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", 98.0 + random.nextDouble() * 2 - 1));
            
            // Generate many normal ECG values followed by borderline values
            double ecg;
            if (i < 570) {
                // Normal values around 0.2 ± 0.05
                ecg = 0.2 + random.nextDouble() * 0.1 - 0.05;
            } else {
                // Assuming std dev of ~0.03-0.05, values just around 3 standard deviations
                // (which should be approximately 0.35 to 0.05 on the high/low ends)
                ecg = random.nextBoolean() ? 
                      0.35 + random.nextDouble() * 0.03 :  // Just at high threshold
                      0.05 - random.nextDouble() * 0.03;   // Just at low threshold
            }
            
            records.add(String.format("%d,%d,%s,%.3f", patientId, timestamp, "ECG", ecg));
        }
        
        writeToFile("borderline_ecg_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Generates data for a patient who has triggered an alert
     */
    private void generateTriggeredAlertPatient(int patientId) throws IOException {
        long baseTime = System.currentTimeMillis();
        List<String> records = new ArrayList<>();
        
        // 10 minutes of normal data with a triggered alert
        for (int i = 0; i < 600; i++) {
            long timestamp = baseTime - (600 - i) * 1000;
            
            // Normal vital signs
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "SystolicPressure", 120.0 + random.nextDouble() * 10 - 5));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "DiastolicPressure", 80.0 + random.nextDouble() * 6 - 3));
            records.add(String.format("%d,%d,%s,%.1f", patientId, timestamp, "Saturation", 98.0 + random.nextDouble() * 2 - 1));
            records.add(String.format("%d,%d,%s,%.3f", patientId, timestamp, "ECG", 0.2 + random.nextDouble() * 0.1 - 0.05));
        }
        
        // Add a manually triggered alert near the end
        long alertTime = baseTime - 30 * 1000; // 30 seconds ago
        records.add(String.format("%d,%d,%s,%s", patientId, alertTime, "Alert", "1"));
        
        writeToFile("alert_triggered_patient_" + patientId + ".txt", records);
    }
    
    /**
     * Writes records to a file in the output directory
     */
    private void writeToFile(String filename, List<String> records) throws IOException {
        Path filePath = Paths.get(outputDirectory, filename);
        Files.write(filePath, records, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}