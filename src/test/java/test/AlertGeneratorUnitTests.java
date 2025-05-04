package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
//import org.mockito.ArgumentCaptor;

import com.alerts.Alert;
import com.alerts.AlertGenerator;
import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AlertGeneratorUnitTests {

    private DataStorage mockDataStorage;
    private Patient mockPatient;
    private AlertGenerator alertGenerator;

    // Test implementation for capturing triggered alerts
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
        // Create mocks for dependencies
        mockDataStorage = Mockito.mock(DataStorage.class);
        mockPatient = Mockito.mock(Patient.class);

        // Create the alert generator with mocked data storage
        alertGenerator = new TestAlertGenerator(mockDataStorage);
    }

    @Test
    public void testBloodPressureTrend() throws Exception {
        // Access the private method using reflection
        Method checkPressureTrend = AlertGenerator.class.getDeclaredMethod(
                "checkPressureTrend", Patient.class, List.class, String.class);
        checkPressureTrend.setAccessible(true);

        // Create test records for increasing trend > 10mmHg
        List<PatientRecord> increasingRecords = Arrays.asList(
                new PatientRecord(1, 120.0, "SystolicPressure", 1000000),
                new PatientRecord(1, 135.0, "SystolicPressure", 1001000),
                new PatientRecord(1, 150.0, "SystolicPressure", 1002000)
        );

        // Invoke the method with increasing trend records
        checkPressureTrend.invoke(alertGenerator, mockPatient, increasingRecords, "Systolic");

        // Verify that an alert was triggered
        List<Alert> alerts = ((TestAlertGenerator)alertGenerator).getTriggeredAlerts();
        assertEquals(1, alerts.size(), "One alert should be triggered");
        assertTrue(alerts.get(0).getCondition().contains("Increasing"),
                "Alert should be for increasing trend");

        // Clear alerts for next test
        ((TestAlertGenerator)alertGenerator).clearAlerts();

        // Create test records for decreasing trend > 10mmHg
        List<PatientRecord> decreasingRecords = Arrays.asList(
                new PatientRecord(1, 150.0, "SystolicPressure", 1000000),
                new PatientRecord(1, 135.0, "SystolicPressure", 1001000),
                new PatientRecord(1, 120.0, "SystolicPressure", 1002000)
        );

        // Invoke the method with decreasing trend records
        checkPressureTrend.invoke(alertGenerator, mockPatient, decreasingRecords, "Systolic");

        // Verify that an alert was triggered
        alerts = ((TestAlertGenerator)alertGenerator).getTriggeredAlerts();
        assertEquals(1, alerts.size(), "One alert should be triggered");
        assertTrue(alerts.get(0).getCondition().contains("Decreasing"),
                "Alert should be for decreasing trend");
    }

    @Test
    public void testBloodPressureThreshold() throws Exception {
        // Access the private method using reflection
        Method checkBloodPressureThreshold = AlertGenerator.class.getDeclaredMethod(
                "checkBloodPressureThreshold", Patient.class, List.class, String.class);
        checkBloodPressureThreshold.setAccessible(true);

        // Create test records for high systolic pressure
        List<PatientRecord> highSystolicRecords = Arrays.asList(
                new PatientRecord(1, 170.0, "SystolicPressure", 1000000),
                new PatientRecord(1, 185.0, "SystolicPressure", 1001000)
        );

        // Invoke the method with high systolic records
        checkBloodPressureThreshold.invoke(alertGenerator, mockPatient, highSystolicRecords, "Systolic");

        // Verify that an alert was triggered
        List<Alert> alerts = ((TestAlertGenerator)alertGenerator).getTriggeredAlerts();
        assertEquals(1, alerts.size(), "One alert should be triggered");
        assertTrue(alerts.get(0).getCondition().contains("high systolic"),
                "Alert should be for high systolic pressure");

        // Clear alerts for next test
        ((TestAlertGenerator)alertGenerator).clearAlerts();

        // Create test records for low diastolic pressure
        List<PatientRecord> lowDiastolicRecords = Arrays.asList(
                new PatientRecord(1, 70.0, "DiastolicPressure", 1000000),
                new PatientRecord(1, 55.0, "DiastolicPressure", 1001000)
        );

        // Invoke the method with low diastolic records
        checkBloodPressureThreshold.invoke(alertGenerator, mockPatient, lowDiastolicRecords, "Diastolic");

        // Verify that an alert was triggered
        alerts = ((TestAlertGenerator)alertGenerator).getTriggeredAlerts();
        assertEquals(1, alerts.size(), "One alert should be triggered");
        assertTrue(alerts.get(0).getCondition().contains("low diastolic"),
                "Alert should be for low diastolic pressure");
    }

    @Test
    public void testOxygenSaturation() throws Exception {
        // Access the private method using reflection
        Method checkLowSaturation = AlertGenerator.class.getDeclaredMethod(
                "checkLowSaturation", Patient.class, List.class);
        checkLowSaturation.setAccessible(true);

        // Create test records for low saturation
        List<PatientRecord> lowSaturationRecords = Arrays.asList(
                new PatientRecord(1, 94.0, "Saturation", 1000000),
                new PatientRecord(1, 91.0, "Saturation", 1001000)
        );

        // Invoke the method with low saturation records
        checkLowSaturation.invoke(alertGenerator, mockPatient, lowSaturationRecords);

        // Verify that an alert was triggered
        List<Alert> alerts = ((TestAlertGenerator)alertGenerator).getTriggeredAlerts();
        assertEquals(1, alerts.size(), "One alert should be triggered");
        assertTrue(alerts.get(0).getCondition().contains("low blood oxygen"),
                "Alert should be for low blood oxygen");

        // Clear alerts for next test
        ((TestAlertGenerator)alertGenerator).clearAlerts();

        // Create test records for normal saturation
        List<PatientRecord> normalSaturationRecords = Arrays.asList(
                new PatientRecord(1, 95.0, "Saturation", 1000000),
                new PatientRecord(1, 96.0, "Saturation", 1001000)
        );

        // Invoke the method with normal saturation records
        checkLowSaturation.invoke(alertGenerator, mockPatient, normalSaturationRecords);

        // Verify that no alert was triggered
        alerts = ((TestAlertGenerator)alertGenerator).getTriggeredAlerts();
        assertEquals(0, alerts.size(), "No alert should be triggered for normal saturation");
    }

    @Test
    public void testRapidSaturationDrop() throws Exception {
        // Access the private method using reflection
        Method checkRapidSaturationDrop = AlertGenerator.class.getDeclaredMethod(
                "checkRapidSaturationDrop", Patient.class, List.class);
        checkRapidSaturationDrop.setAccessible(true);

        // Create test records for rapid saturation drop
        long baseTime = System.currentTimeMillis();
        List<PatientRecord> rapidDropRecords = Arrays.asList(
                new PatientRecord(1, 99.0, "Saturation", baseTime - 500000),
                new PatientRecord(1, 94.0, "Saturation", baseTime - 300000),
                new PatientRecord(1, 93.0, "Saturation", baseTime - 100000)
        );

        // Invoke the method with rapid drop records
        checkRapidSaturationDrop.invoke(alertGenerator, mockPatient, rapidDropRecords);

        // Verify that an alert was triggered
        List<Alert> alerts = ((TestAlertGenerator)alertGenerator).getTriggeredAlerts();
        assertEquals(1, alerts.size(), "One alert should be triggered");
        assertTrue(alerts.get(0).getCondition().contains("Rapid Oxygen Saturation Drop"),
                "Alert should be for rapid saturation drop");

        // Clear alerts for next test
        ((TestAlertGenerator)alertGenerator).clearAlerts();

        // Create test records for small saturation change
        List<PatientRecord> smallChangeRecords = Arrays.asList(
                new PatientRecord(1, 97.0, "Saturation", baseTime - 500000),
                new PatientRecord(1, 96.0, "Saturation", baseTime - 300000),
                new PatientRecord(1, 95.0, "Saturation", baseTime - 100000)
        );

        // Invoke the method with small change records
        checkRapidSaturationDrop.invoke(alertGenerator, mockPatient, smallChangeRecords);

        // Verify that no alert was triggered
        alerts = ((TestAlertGenerator)alertGenerator).getTriggeredAlerts();
        assertEquals(0, alerts.size(), "No alert should be triggered for small saturation change");
    }

    @Test
    public void testHypotensiveHypoxemia() throws Exception {
        // Access the private method using reflection
        Method checkHypotensiveHypoxemia = AlertGenerator.class.getDeclaredMethod(
                "checkHypotensiveHypoxemia", Patient.class, List.class, List.class);
        checkHypotensiveHypoxemia.setAccessible(true);

        // Create test records for low BP and low O2 at the same time
        long baseTime = System.currentTimeMillis();
        List<PatientRecord> lowBPRecords = Arrays.asList(
                new PatientRecord(1, 85.0, "SystolicPressure", baseTime - 30000)
        );

        List<PatientRecord> lowO2Records = Arrays.asList(
                new PatientRecord(1, 91.0, "Saturation", baseTime - 29000)
        );

        // Invoke the method with both conditions present
        checkHypotensiveHypoxemia.invoke(alertGenerator, mockPatient, lowBPRecords, lowO2Records);

        // Verify that an alert was triggered
        List<Alert> alerts = ((TestAlertGenerator)alertGenerator).getTriggeredAlerts();
        assertEquals(1, alerts.size(), "One alert should be triggered");
        assertTrue(alerts.get(0).getCondition().contains("Hypotensive Hypoxemia"),
                "Alert should be for hypotensive hypoxemia");

        // Clear alerts for next test
        ((TestAlertGenerator)alertGenerator).clearAlerts();

        // Create test records for low BP and low O2 far apart in time
        List<PatientRecord> earlyLowBPRecords = Arrays.asList(
                new PatientRecord(1, 85.0, "SystolicPressure", baseTime - 300000)
        );

        List<PatientRecord> lateLowO2Records = Arrays.asList(
                new PatientRecord(1, 91.0, "Saturation", baseTime - 30000)
        );

        // Invoke the method with conditions far apart
        checkHypotensiveHypoxemia.invoke(alertGenerator, mockPatient, earlyLowBPRecords, lateLowO2Records);

        // Verify that no alert was triggered
        alerts = ((TestAlertGenerator)alertGenerator).getTriggeredAlerts();
        assertEquals(0, alerts.size(), "No alert should be triggered when conditions are far apart");
    }

    @Test
    public void testECGAbnormalities() throws Exception {
        // Access the private method using reflection
        Method checkECGAbnormalities = AlertGenerator.class.getDeclaredMethod(
                "checkECGAbnormalities", Patient.class);
        checkECGAbnormalities.setAccessible(true);

        // Create a patient with abnormal ECG records
        List<PatientRecord> abnormalECGRecords = new ArrayList<>();
        long baseTime = System.currentTimeMillis();

        // Create 30 normal records to establish baseline
        for (int i = 0; i < 30; i++) {
            abnormalECGRecords.add(new PatientRecord(1, 0.2 + (Math.random() * 0.02), "ECG", baseTime - (30-i)*1000));
        }

        // Add an outlier record
        abnormalECGRecords.add(new PatientRecord(1, 1.0, "ECG", baseTime));

        // Mock the patient to return these records
        Mockito.when(mockPatient.getRecords(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(abnormalECGRecords);

        // Invoke the method
        checkECGAbnormalities.invoke(alertGenerator, mockPatient);

        // Verify that an alert was triggered
        List<Alert> alerts = ((TestAlertGenerator)alertGenerator).getTriggeredAlerts();
        assertEquals(1, alerts.size(), "One alert should be triggered");
        assertTrue(alerts.get(0).getCondition().contains("ECG Abnormality"),
                "Alert should be for ECG abnormality");
    }

    @Test
    public void testTriggeredAlert() throws Exception {
        // Access the private method using reflection
        Method checkTriggeredAlert = AlertGenerator.class.getDeclaredMethod(
                "checkTriggeredAlert", Patient.class);
        checkTriggeredAlert.setAccessible(true);

        // Create a patient with a triggered alert record
        List<PatientRecord> alertRecords = Arrays.asList(
                new PatientRecord(1, 1.0, "Alert", System.currentTimeMillis())
        );

        // Mock the patient to return these records
        Mockito.when(mockPatient.getRecords(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(alertRecords);

        // Invoke the method
        checkTriggeredAlert.invoke(alertGenerator, mockPatient);

        // Verify that an alert was triggered
        List<Alert> alerts = ((TestAlertGenerator)alertGenerator).getTriggeredAlerts();
        assertEquals(1, alerts.size(), "One alert should be triggered");
        assertTrue(alerts.get(0).getCondition().contains("Call Button"),
                "Alert should be for call button");
    }

    @Test
    public void testEvaluateData() throws Exception {
        // Create the alert generator with mocked data storage
        AlertGenerator generator = new AlertGenerator(mockDataStorage);

        // Set the alertStates field using reflection
        Field alertStatesField = AlertGenerator.class.getDeclaredField("alertStates");
        alertStatesField.setAccessible(true);
        boolean[] alertStates = new boolean[10];
        alertStatesField.set(generator, alertStates);

        // Test the evaluateData method
        assertDoesNotThrow(() -> generator.evaluateData(mockPatient));
    }
}