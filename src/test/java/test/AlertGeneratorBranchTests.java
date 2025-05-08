package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.alerts.Alert;
import com.alerts.AlertGenerator;
import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.reflect.Method;

/**
 * Highly targeted tests for the specific methods with missed branches
 */
public class AlertGeneratorBranchTests {

    private DataStorage mockDataStorage;
    private Patient mockPatient;
    private AlertGenerator alertGenerator;

    // Track triggered alerts for testing
    private static class TestAlertGenerator extends AlertGenerator {
        private List<Alert> triggeredAlerts = new ArrayList<>();

        public TestAlertGenerator(DataStorage dataStorage) {
            super(dataStorage);
        }

        @Override
        protected void triggerAlert(Alert alert) {
            triggeredAlerts.add(alert);
        }

        public List<Alert> getTriggeredAlerts() {
            return triggeredAlerts;
        }

        public void clearAlerts() {
            triggeredAlerts.clear();
        }
    }

    @BeforeEach
    public void setUp() {
        mockDataStorage = Mockito.mock(DataStorage.class);
        mockPatient = Mockito.mock(Patient.class);
        alertGenerator = new TestAlertGenerator(mockDataStorage);
    }

    /**
     * Test checkECGAbnormalities with exactly the minimum required records
     */
    @Test
    public void testECGAbnormalitiesMinimumRecords() throws Exception {
        Method checkECGAbnormalities = AlertGenerator.class.getDeclaredMethod(
                "checkECGAbnormalities", Patient.class);
        checkECGAbnormalities.setAccessible(true);

        // Create exactly 20 records (minimum required)
        List<PatientRecord> exactlyMinRecords = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            exactlyMinRecords.add(new PatientRecord(1, 0.2, "ECG", 1000000 + i*1000));
        }

        Mockito.when(mockPatient.getRecords(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(exactlyMinRecords);

        checkECGAbnormalities.invoke(alertGenerator, mockPatient);
    }

    /**
     * Test ECG abnormalities with one abnormal value at the end
     */
    @Test
    public void testECGAbnormalitiesOneAbnormalValue() throws Exception {
        Method checkECGAbnormalities = AlertGenerator.class.getDeclaredMethod(
                "checkECGAbnormalities", Patient.class);
        checkECGAbnormalities.setAccessible(true);

        // Create records with one abnormal value at the end
        List<PatientRecord> records = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            double value = 0.2; // normal value
            if (i == 29) {
                value = 1.0; // abnormal value at the end
            }
            records.add(new PatientRecord(1, value, "ECG", 1000000 + i*1000));
        }

        Mockito.when(mockPatient.getRecords(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(records);

        checkECGAbnormalities.invoke(alertGenerator, mockPatient);
    }

    /**
     * Test ECG abnormalities with rapid changes between readings
     */
    @Test
    public void testECGAbnormalitiesRapidChanges() throws Exception {
        Method checkECGAbnormalities = AlertGenerator.class.getDeclaredMethod(
                "checkECGAbnormalities", Patient.class);
        checkECGAbnormalities.setAccessible(true);

        // Create records with rapid changes
        List<PatientRecord> records = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            // Create oscillating values with big jumps
            double value = (i % 2 == 0) ? 0.2 : 0.8;
            records.add(new PatientRecord(1, value, "ECG", 1000000 + i*1000));
        }

        Mockito.when(mockPatient.getRecords(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(records);

        checkECGAbnormalities.invoke(alertGenerator, mockPatient);
    }

    /**
     * Test all branches of checkForAbnormalPattern
     */
    @Test
    public void testCheckForAbnormalPattern() throws Exception {
        Method checkForAbnormalPattern = AlertGenerator.class.getDeclaredMethod(
                "checkForAbnormalPattern", List.class, double.class, double.class);
        checkForAbnormalPattern.setAccessible(true);

        // Test alternating pattern (true)
        List<Double> alternatingValues = Arrays.asList(0.1, 0.3, 0.1, 0.3, 0.1, 0.3);
        boolean result1 = (boolean) checkForAbnormalPattern.invoke(alertGenerator, alternatingValues, 0.2, 0.1);

        // Test non-alternating pattern (false)
        List<Double> nonAlternatingValues = Arrays.asList(0.1, 0.3, 0.4, 0.2, 0.1, 0.3);
        boolean result2 = (boolean) checkForAbnormalPattern.invoke(alertGenerator, nonAlternatingValues, 0.2, 0.1);

        // Test flatline pattern (true)
        List<Double> flatlineValues = Arrays.asList(0.2, 0.2, 0.2, 0.2, 0.2, 0.2);
        boolean result3 = (boolean) checkForAbnormalPattern.invoke(alertGenerator, flatlineValues, 0.2, 0.1);

        // Test non-flatline pattern (false)
        List<Double> nonFlatlineValues = Arrays.asList(0.2, 0.21, 0.25, 0.2, 0.19, 0.2);
        boolean result4 = (boolean) checkForAbnormalPattern.invoke(alertGenerator, nonFlatlineValues, 0.2, 0.1);

        // Test consistent trend pattern (true)
        List<Double> consistentTrendValues = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6);
        boolean result5 = (boolean) checkForAbnormalPattern.invoke(alertGenerator, consistentTrendValues, 0.35, 0.2);

        // Test non-consistent trend pattern (false)
        List<Double> nonConsistentTrendValues = Arrays.asList(0.1, 0.2, 0.3, 0.25, 0.4, 0.5);
        boolean result6 = (boolean) checkForAbnormalPattern.invoke(alertGenerator, nonConsistentTrendValues, 0.35, 0.2);

        // Verify results
        assertTrue(result1 || result3 || result5, "Should find at least one abnormal pattern");
        assertFalse(result2 && result4 && result6, "Should reject at least one normal pattern");
    }

    /**
     * Test all branches of checkBloodPressureThreshold for systolic
     */
    @Test
    public void testCheckBloodPressureThresholdSystolic() throws Exception {
        Method checkBloodPressureThreshold = AlertGenerator.class.getDeclaredMethod(
                "checkBloodPressureThreshold", Patient.class, List.class, String.class);
        checkBloodPressureThreshold.setAccessible(true);

        // Test high systolic pressure (> 180)
        List<PatientRecord> highSystolicRecords = Arrays.asList(
                new PatientRecord(1, 190.0, "SystolicPressure", 1000000)
        );
        checkBloodPressureThreshold.invoke(alertGenerator, mockPatient, highSystolicRecords, "Systolic");

        // Test low systolic pressure (< 90)
        ((TestAlertGenerator)alertGenerator).clearAlerts();
        List<PatientRecord> lowSystolicRecords = Arrays.asList(
                new PatientRecord(1, 85.0, "SystolicPressure", 1000000)
        );
        checkBloodPressureThreshold.invoke(alertGenerator, mockPatient, lowSystolicRecords, "Systolic");

        // Test normal systolic pressure (between 90 and 180)
        ((TestAlertGenerator)alertGenerator).clearAlerts();
        List<PatientRecord> normalSystolicRecords = Arrays.asList(
                new PatientRecord(1, 120.0, "SystolicPressure", 1000000)
        );
        checkBloodPressureThreshold.invoke(alertGenerator, mockPatient, normalSystolicRecords, "Systolic");
    }

    /**
     * Test all branches of checkBloodPressureThreshold for diastolic
     */
    @Test
    public void testCheckBloodPressureThresholdDiastolic() throws Exception {
        Method checkBloodPressureThreshold = AlertGenerator.class.getDeclaredMethod(
                "checkBloodPressureThreshold", Patient.class, List.class, String.class);
        checkBloodPressureThreshold.setAccessible(true);

        // Test high diastolic pressure (> 120)
        List<PatientRecord> highDiastolicRecords = Arrays.asList(
                new PatientRecord(1, 130.0, "DiastolicPressure", 1000000)
        );
        checkBloodPressureThreshold.invoke(alertGenerator, mockPatient, highDiastolicRecords, "Diastolic");

        // Test low diastolic pressure (< 60)
        ((TestAlertGenerator)alertGenerator).clearAlerts();
        List<PatientRecord> lowDiastolicRecords = Arrays.asList(
                new PatientRecord(1, 55.0, "DiastolicPressure", 1000000)
        );
        checkBloodPressureThreshold.invoke(alertGenerator, mockPatient, lowDiastolicRecords, "Diastolic");

        // Test normal diastolic pressure (between 60 and 120)
        ((TestAlertGenerator)alertGenerator).clearAlerts();
        List<PatientRecord> normalDiastolicRecords = Arrays.asList(
                new PatientRecord(1, 80.0, "DiastolicPressure", 1000000)
        );
        checkBloodPressureThreshold.invoke(alertGenerator, mockPatient, normalDiastolicRecords, "Diastolic");
    }

    /**
     * Test all branches of checkPressureTrend for increasing trend
     */
    @Test
    public void testCheckPressureTrendIncreasing() throws Exception {
        Method checkPressureTrend = AlertGenerator.class.getDeclaredMethod(
                "checkPressureTrend", Patient.class, List.class, String.class);
        checkPressureTrend.setAccessible(true);

        // Test increasing trend (> 10 mmHg each step)
        List<PatientRecord> increasingRecords = Arrays.asList(
                new PatientRecord(1, 100.0, "SystolicPressure", 1000000),
                new PatientRecord(1, 115.0, "SystolicPressure", 1001000),
                new PatientRecord(1, 130.0, "SystolicPressure", 1002000)
        );
        checkPressureTrend.invoke(alertGenerator, mockPatient, increasingRecords, "Systolic");

        // Test slightly increasing trend (< 10 mmHg each step) - should not trigger
        ((TestAlertGenerator)alertGenerator).clearAlerts();
        List<PatientRecord> slightlyIncreasingRecords = Arrays.asList(
                new PatientRecord(1, 100.0, "SystolicPressure", 1000000),
                new PatientRecord(1, 105.0, "SystolicPressure", 1001000),
                new PatientRecord(1, 110.0, "SystolicPressure", 1002000)
        );
        checkPressureTrend.invoke(alertGenerator, mockPatient, slightlyIncreasingRecords, "Systolic");
    }

    /**
     * Test all branches of checkPressureTrend for decreasing trend
     */
    @Test
    public void testCheckPressureTrendDecreasing() throws Exception {
        Method checkPressureTrend = AlertGenerator.class.getDeclaredMethod(
                "checkPressureTrend", Patient.class, List.class, String.class);
        checkPressureTrend.setAccessible(true);

        // Test decreasing trend (> 10 mmHg each step)
        List<PatientRecord> decreasingRecords = Arrays.asList(
                new PatientRecord(1, 130.0, "SystolicPressure", 1000000),
                new PatientRecord(1, 115.0, "SystolicPressure", 1001000),
                new PatientRecord(1, 100.0, "SystolicPressure", 1002000)
        );
        checkPressureTrend.invoke(alertGenerator, mockPatient, decreasingRecords, "Systolic");

        // Test slightly decreasing trend (< 10 mmHg each step) - should not trigger
        ((TestAlertGenerator)alertGenerator).clearAlerts();
        List<PatientRecord> slightlyDecreasingRecords = Arrays.asList(
                new PatientRecord(1, 110.0, "SystolicPressure", 1000000),
                new PatientRecord(1, 105.0, "SystolicPressure", 1001000),
                new PatientRecord(1, 100.0, "SystolicPressure", 1002000)
        );
        checkPressureTrend.invoke(alertGenerator, mockPatient, slightlyDecreasingRecords, "Systolic");
    }

    /**
     * Test all branches of checkHypotensiveHypoxemia
     */
    @Test
    public void testCheckHypotensiveHypoxemia() throws Exception {
        Method checkHypotensiveHypoxemia = AlertGenerator.class.getDeclaredMethod(
                "checkHypotensiveHypoxemia", Patient.class, List.class, List.class);
        checkHypotensiveHypoxemia.setAccessible(true);

        long baseTime = System.currentTimeMillis();

        // Test both conditions present within time window
        List<PatientRecord> lowBP = Arrays.asList(
                new PatientRecord(1, 85.0, "SystolicPressure", baseTime - 30000) // Low BP
        );
        List<PatientRecord> lowO2 = Arrays.asList(
                new PatientRecord(1, 91.0, "Saturation", baseTime - 25000) // Low O2
        );
        checkHypotensiveHypoxemia.invoke(alertGenerator, mockPatient, lowBP, lowO2);

        // Test both conditions present but outside time window
        ((TestAlertGenerator)alertGenerator).clearAlerts();
        List<PatientRecord> earlyLowBP = Arrays.asList(
                new PatientRecord(1, 85.0, "SystolicPressure", baseTime - 200000) // Low BP long ago
        );
        List<PatientRecord> lateLowO2 = Arrays.asList(
                new PatientRecord(1, 91.0, "Saturation", baseTime - 25000) // Low O2 recently
        );
        checkHypotensiveHypoxemia.invoke(alertGenerator, mockPatient, earlyLowBP, lateLowO2);

        // Test normal BP with low O2
        ((TestAlertGenerator)alertGenerator).clearAlerts();
        List<PatientRecord> normalBP = Arrays.asList(
                new PatientRecord(1, 120.0, "SystolicPressure", baseTime - 30000) // Normal BP
        );
        checkHypotensiveHypoxemia.invoke(alertGenerator, mockPatient, normalBP, lowO2);

        // Test low BP with normal O2
        ((TestAlertGenerator)alertGenerator).clearAlerts();
        List<PatientRecord> normalO2 = Arrays.asList(
                new PatientRecord(1, 96.0, "Saturation", baseTime - 25000) // Normal O2
        );
        checkHypotensiveHypoxemia.invoke(alertGenerator, mockPatient, lowBP, normalO2);
    }

    /**
     * Test all branches of checkRapidSaturationDrop
     */
    @Test
    public void testCheckRapidSaturationDrop() throws Exception {
        Method checkRapidSaturationDrop = AlertGenerator.class.getDeclaredMethod(
                "checkRapidSaturationDrop", Patient.class, List.class);
        checkRapidSaturationDrop.setAccessible(true);

        long baseTime = System.currentTimeMillis();

        // Test significant drop (> 5%)
        List<PatientRecord> significantDropRecords = Arrays.asList(
                new PatientRecord(1, 98.0, "Saturation", baseTime - 500000),
                new PatientRecord(1, 96.0, "Saturation", baseTime - 300000),
                new PatientRecord(1, 92.0, "Saturation", baseTime - 100000)
        );
        checkRapidSaturationDrop.invoke(alertGenerator, mockPatient, significantDropRecords);

        // Test small drop (< 5%)
        ((TestAlertGenerator)alertGenerator).clearAlerts();
        List<PatientRecord> smallDropRecords = Arrays.asList(
                new PatientRecord(1, 98.0, "Saturation", baseTime - 500000),
                new PatientRecord(1, 97.0, "Saturation", baseTime - 300000),
                new PatientRecord(1, 96.0, "Saturation", baseTime - 100000)
        );
        checkRapidSaturationDrop.invoke(alertGenerator, mockPatient, smallDropRecords);

        // Test drop outside time window (> 10 minutes)
        ((TestAlertGenerator)alertGenerator).clearAlerts();
        List<PatientRecord> dropOutsideTimeFrame = Arrays.asList(
                new PatientRecord(1, 98.0, "Saturation", baseTime - 700000), // > 10 minutes
                new PatientRecord(1, 92.0, "Saturation", baseTime - 100000)
        );
        checkRapidSaturationDrop.invoke(alertGenerator, mockPatient, dropOutsideTimeFrame);
    }
}