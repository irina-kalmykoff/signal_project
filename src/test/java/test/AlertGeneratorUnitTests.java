package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.alerts.*;
import com.data_management.*;
import com.strategy.*;

import java.util.*;
import java.util.stream.*;
import java.lang.reflect.Field;

public class AlertGeneratorUnitTests {
        /*
         * These tests focus on the individual alert strategies directly, testing them in isolation with mock data. 
         * They verify that each strategy correctly identifies conditions that should trigger alerts. 
         * This class tests the underlying logic of alert detection.
         */

    private DataStorage mockDataStorage;
    private Patient mockPatient;
    //private AlertGenerator alertGenerator;
    private TestAlertFactoryManager testManager;
    // Alert capture utility for individual strategies
    private AlertCapture alertCapture;
    
    // Strategy instances for direct testing
    private BloodPressureStrategy bloodPressureStrategy;
    private OxygenSaturationStrategy oxygenSaturationStrategy;
    private HeartRateStrategy heartRateStrategy;
    private CallButtonAlertStrategy callButtonStrategy;    


    @BeforeEach
    public void setUp() {
        // Reset DataStorage singleton to prevent state leakage between tests
        resetDataStorage();
        // Create mocks for dependencies
        mockDataStorage = Mockito.mock(DataStorage.class);
        mockPatient = Mockito.mock(Patient.class);

        // Create alert capture utility
        alertCapture = new AlertCapture();
        
        // Create test factories manager
        testManager = new TestAlertFactoryManager(mockDataStorage);
        
        // Create strategy instances for direct testing
        bloodPressureStrategy = new BloodPressureStrategy();
        oxygenSaturationStrategy = new OxygenSaturationStrategy();
        heartRateStrategy = new HeartRateStrategy();
        callButtonStrategy = new CallButtonAlertStrategy();
    }

    @AfterEach
    public void cleanUp() {
        testManager.clearAllAlerts();
        alertCapture.clearAlerts();
    }

    /**
     * Reset DataStorage singleton between tests
     */
    private void resetDataStorage() {
        try {
            Field instance = DataStorage.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            System.err.println("Failed to reset DataStorage singleton: " + e.getMessage());
        }
    }

    @Test
    public void testBloodPressureTrend() throws Exception {

        // Create test records for increasing trend > 10mmHg
        List<PatientRecord> increasingRecords = Arrays.asList(
                new PatientRecord(1, 120.0, "SystolicPressure", 1000000),
                new PatientRecord(1, 135.0, "SystolicPressure", 1001000),
                new PatientRecord(1, 150.0, "SystolicPressure", 1002000)
        );

        // Test using the strategy directly
        Alert increasingAlert = bloodPressureStrategy.checkAlert(mockPatient, increasingRecords);
        // Verify that an alert was created
        assertNotNull(increasingAlert, "Increasing trend should trigger an alert");
        assertTrue(increasingAlert.getCondition().contains("Increasing"), 
                "Alert should be for increasing trend");
        

        // Create test records for decreasing trend > 10mmHg
        List<PatientRecord> decreasingRecords = Arrays.asList(
                new PatientRecord(1, 150.0, "SystolicPressure", 1000000),
                new PatientRecord(1, 135.0, "SystolicPressure", 1001000),
                new PatientRecord(1, 120.0, "SystolicPressure", 1002000)
        );

        // Test using the strategy directly
        Alert decreasingAlert = bloodPressureStrategy.checkAlert(mockPatient, decreasingRecords);
        
        // Verify that an alert was created
        assertNotNull(decreasingAlert, "Decreasing trend should trigger an alert");
        assertTrue(decreasingAlert.getCondition().contains("Decreasing"), 
                "Alert should be for decreasing trend");
    }

    @Test
    public void testBloodPressureThreshold() throws Exception {
        // Create test records for high systolic pressure
        List<PatientRecord> highSystolicRecords = Arrays.asList(
                new PatientRecord(1, 170.0, "SystolicPressure", 1000000),
                new PatientRecord(1, 185.0, "SystolicPressure", 1001000)
        );
        System.out.println("DEBUG - Testing blood pressure threshold:");
        System.out.println("  Systolic values: 170.0, 185.0 mmHg");
        System.out.println("DEBUG - Examining TestBloodPressureAlertFactory.createAlert method:");
        
        // Mock the records again with both types
        Mockito.when(mockPatient.getRecords(Mockito.anyLong(), Mockito.anyLong()))
               .thenReturn(highSystolicRecords);

        Alert systolicAlert = bloodPressureStrategy.checkAlert(mockPatient, highSystolicRecords, Collections.emptyList());
        System.out.println("  High systolic direct result: " + 
        (systolicAlert != null ? "Alert triggered: " + systolicAlert.getCondition() : "No alert"));
        
        assertNotNull(systolicAlert, "High systolic pressure should trigger an alert");
        assertTrue(systolicAlert.getCondition().toLowerCase().contains("high") &&
                        systolicAlert.getCondition().toLowerCase().contains("systolic"),
                "Alert should be for high systolic pressure");

        resetDataStorage();

        // Clear alerts for next test
        TestBloodPressureAlertFactory factory = testManager.getPressureAlertFactory();
        factory.clearAlerts();

        // Create test records for low diastolic pressure
        List<PatientRecord> lowDiastolicRecords = Arrays.asList(
                new PatientRecord(1, 70.0, "DiastolicPressure", 1000000),
                new PatientRecord(1, 55.0, "DiastolicPressure", 1001000)        );

        // Test low diastolic with the BloodPressureFactory
        List<PatientRecord> combinedRecords = new ArrayList<>();
        combinedRecords.addAll(highSystolicRecords);
        combinedRecords.addAll(lowDiastolicRecords);
        
        // Mock the records again with both types
        Mockito.when(mockPatient.getRecords(Mockito.anyLong(), Mockito.anyLong()))
               .thenReturn(combinedRecords);        
        
        Alert diastolicAlert = bloodPressureStrategy.checkAlert(
        mockPatient, Collections.emptyList(),  lowDiastolicRecords);

        System.out.println("  Low diastolic direct result: " + 
                     (diastolicAlert != null ? "Alert triggered: " + diastolicAlert.getCondition() : "No alert"));
        
        // Verify alert was created
        assertNotNull(diastolicAlert, "Low diastolic should trigger an alert");
        assertTrue(diastolicAlert.getCondition().toLowerCase().contains("low") &&
                diastolicAlert.getCondition().toLowerCase().contains("diastolic"),
                "Alert should be for low diastolic pressure");       
    }

    @Test
    public void testOxygenSaturation() throws Exception {

        // Create test records for low saturation
        List<PatientRecord> lowSaturationRecords = Arrays.asList(
                new PatientRecord(1, 94.0, "Saturation", 1000000),
                new PatientRecord(1, 91.0, "Saturation", 1001000)
        );

        // Test directly with strategy
        Alert lowSatAlert = oxygenSaturationStrategy.checkAlert(mockPatient, lowSaturationRecords);
        
        // Verify alert was created
        assertNotNull(lowSatAlert, "Low saturation should trigger an alert");
        assertTrue(lowSatAlert.getCondition().toLowerCase().contains("low") &&
                  lowSatAlert.getCondition().toLowerCase().contains("oxygen"),
                "Alert should be for low blood oxygen");

        // Create test records for normal saturation
        List<PatientRecord> normalSaturationRecords = Arrays.asList(
                new PatientRecord(1, 95.0, "Saturation", 1000000),
                new PatientRecord(1, 96.0, "Saturation", 1001000)
        );

       // Test directly with strategy
        Alert normalSatAlert = oxygenSaturationStrategy.checkAlert(mockPatient, normalSaturationRecords);
        
        // Verify no alert for normal values
        assertNull(normalSatAlert, "Normal saturation should not trigger an alert");
    }

    @Test
    public void testRapidSaturationDrop() throws Exception {

        // Create test records for rapid saturation drop
        long baseTime = System.currentTimeMillis();
        List<PatientRecord> rapidDropRecords = Arrays.asList(
                new PatientRecord(1, 99.0, "Saturation", baseTime - 500000),
                new PatientRecord(1, 94.0, "Saturation", baseTime - 300000),
                new PatientRecord(1, 93.0, "Saturation", baseTime - 100000)
        );

        // Test directly with strategy
        Alert rapidDropAlert = oxygenSaturationStrategy.checkAlert(mockPatient, rapidDropRecords);
        
        // Verify alert was created
        assertNotNull(rapidDropAlert, "Rapid drop should trigger an alert");
        assertTrue(rapidDropAlert.getCondition().contains("Rapid") &&
                  rapidDropAlert.getCondition().contains("Drop"),
                "Alert should be for rapid saturation drop");

        // Create test records for small saturation change
        List<PatientRecord> smallChangeRecords = Arrays.asList(
                new PatientRecord(1, 97.0, "Saturation", baseTime - 500000),
                new PatientRecord(1, 96.0, "Saturation", baseTime - 300000),
                new PatientRecord(1, 95.0, "Saturation", baseTime - 100000)
        );

         // Test directly with strategy
         Alert smallChangeAlert = oxygenSaturationStrategy.checkAlert(mockPatient, smallChangeRecords);
        
         // Verify no alert for small changes
         assertNull(smallChangeAlert, "Small saturation change should not trigger an alert");
    }

    @Test
    public void testHypotensiveHypoxemia() throws Exception {
        
        // Create test records for low BP and low O2 at the same time
        long baseTime = System.currentTimeMillis();
        List<PatientRecord> lowBPRecords = Arrays.asList(
                new PatientRecord(1, 85.0, "SystolicPressure", baseTime - 30000)
        );

        List<PatientRecord> lowO2Records = Arrays.asList(
                new PatientRecord(1, 91.0, "Saturation", baseTime - 29000)
        );

        // Test directly with strategy
        Alert hypotensiveHypoxemiaAlert = oxygenSaturationStrategy.checkAlert(
            mockPatient, lowO2Records, lowBPRecords);
        
        // Verify alert was created
        assertNotNull(hypotensiveHypoxemiaAlert, "Hypotensive hypoxemia should trigger an alert");
        assertTrue(hypotensiveHypoxemiaAlert.getCondition().contains("Hypotensive") &&
                  hypotensiveHypoxemiaAlert.getCondition().contains("Hypoxemia"),
                "Alert should be for hypotensive hypoxemia");

        // Create test records for low BP and low O2 far apart in time
        List<PatientRecord> earlyLowBPRecords = Arrays.asList(
                new PatientRecord(1, 85.0, "SystolicPressure", baseTime - 300000)
        );

        List<PatientRecord> lateLowO2Records = Arrays.asList(
                new PatientRecord(1, 91.0, "Saturation", baseTime - 30000)
        );

       // Test directly with strategy
       Alert separatedConditionsAlert = oxygenSaturationStrategy.checkAlert(
        mockPatient, lateLowO2Records, earlyLowBPRecords);
    
        // Verify no alert when conditions are separated in time
        assertNull(separatedConditionsAlert, 
        "Conditions far apart in time should not trigger hypotensive hypoxemia alert");
    }



    @Test
    public void testECGAbnormalities() throws Exception {
        // Create a patient with abnormal ECG records
        List<PatientRecord> abnormalECGRecords = new ArrayList<>();
        long baseTime = System.currentTimeMillis();

        // Create 30 normal records to establish baseline
        for (int i = 0; i < 30; i++) {
            abnormalECGRecords.add(new PatientRecord(1, 0.2 + (Math.random() * 0.02), "ECG", baseTime - (30-i)*1000));
        }

        // Add an outlier record
        abnormalECGRecords.add(new PatientRecord(1, 5.0, "ECG", baseTime));

        // System.out.println("DEBUG - testECGAbnormalities:");
        // System.out.println("  Number of baseline records: " + (abnormalECGRecords.size() - 1));
        // System.out.println("  Baseline value: ~0.2");
        // System.out.println("  Abnormal record value: 5.0 (25x normal)");

         // Test directly with strategy
         Alert ecgAlert = heartRateStrategy.checkAlert(mockPatient, abnormalECGRecords);

         System.out.println("  Alert returned: " + (ecgAlert != null));
        if (ecgAlert != null) {
        System.out.println("  Alert condition: " + ecgAlert.getCondition());
        }
        
         // Verify alert was created
         assertNotNull(ecgAlert, "ECG abnormality should trigger an alert");
         assertTrue(ecgAlert.getCondition().contains("ECG") &&
                   ecgAlert.getCondition().contains("Abnormality"),
                 "Alert should be for ECG abnormality");
    }

    @Test
    public void testTriggeredAlert() throws Exception {
        // Create a patient with a triggered alert record
        List<PatientRecord> alertRecords = Arrays.asList(
                new PatientRecord(1, 1.0, "Alert", System.currentTimeMillis())
        );

        // Test directly with strategy
        Alert callButtonAlert = callButtonStrategy.checkAlert(mockPatient, alertRecords);
        
        // Verify alert was created
        assertNotNull(callButtonAlert, "Call button should trigger an alert");
        assertTrue(callButtonAlert.getCondition().contains("Call Button") ||
                  callButtonAlert.getCondition().contains("Assistance"),
                "Alert should be for call button");
    }

    @Test
    public void testIntegratedFactories() {
        // Create records for multiple alert conditions
        long baseTime = System.currentTimeMillis();
        List<PatientRecord> mixedRecords = new ArrayList<>();
        
        // Add low oxygen saturation record
        mixedRecords.add(new PatientRecord(1, 91.0, "Saturation", baseTime - 1000));
        
        // Add high blood pressure record
        mixedRecords.add(new PatientRecord(1, 190.0, "SystolicPressure", baseTime - 2000));
        
        // Add ECG abnormality records
        for (int i = 0; i < 20; i++) {
            mixedRecords.add(new PatientRecord(1, 0.2, "ECG", baseTime - 10000 + i*100));
        }
        mixedRecords.add(new PatientRecord(1, 5.0, "ECG", baseTime - 500));
        
        // Add call button alert
        mixedRecords.add(new PatientRecord(1, 1.0, "Alert", baseTime - 100));
        
        Patient realPatient = new Patient(1);
        for (PatientRecord record : mixedRecords) {
                realPatient.addRecord(
                record.getMeasurementValue(), 
                record.getRecordType(), 
                record.getTimestamp()
                );
        }
        
        Mockito.when(mockDataStorage.getPatient(1)).thenReturn(realPatient);
        System.out.println("DEBUG - Testing each strategy directly:");
        List<PatientRecord> o2Records = realPatient.getRecords(0, Long.MAX_VALUE)
                .stream()
                .filter(r -> r.getRecordType().equals("Saturation"))
                .collect(Collectors.toList());
        
        Alert o2Alert = oxygenSaturationStrategy.checkAlert(realPatient, o2Records);
                System.out.println("  Oxygen strategy direct result: " + 
                        (o2Alert != null ? "Alert triggered: " + o2Alert.getCondition() : "No alert"));

        // Test each factory type with the patient containing multiple conditions
        TestBloodOxygenAlertFactory oxygenFactory = testManager.getOxygenAlertFactory();
        oxygenFactory.createAlert("1", "", baseTime);
        
        TestBloodPressureAlertFactory pressureFactory = testManager.getPressureAlertFactory();
        pressureFactory.createAlert("1", "", baseTime);
        
        TestECGAlertFactory ecgFactory = testManager.getEcgAlertFactory();
        ecgFactory.createAlert("1", "", baseTime);
        
        TestCallButtonAlertFactory callButtonFactory = testManager.getCallButtonAlertFactory();
        callButtonFactory.createAlert("1", "", baseTime);
        
        // Debug output
        // System.out.println("DEBUG - testIntegratedFactories:");
        // System.out.println("  Oxygen alerts: " + testManager.getOxygenAlertFactory().getCapturedAlerts().size());
        // System.out.println("  Blood pressure alerts: " + testManager.getPressureAlertFactory().getCapturedAlerts().size());
        // System.out.println("  ECG alerts: " + testManager.getEcgAlertFactory().getCapturedAlerts().size());
        // System.out.println("  Call button alerts: " + testManager.getCallButtonAlertFactory().getCapturedAlerts().size());

        for (Alert alert : testManager.getOxygenAlertFactory().getCapturedAlerts()) {
                System.out.println("  Oxygen alert: " + alert.getCondition());
            }
        
        // Verify alerts were captured by different factories
        assertTrue(testManager.getOxygenAlertFactory().getCapturedAlerts().size() > 0,
                "Oxygen factory should capture alerts");
                
        assertTrue(testManager.getPressureAlertFactory().getCapturedAlerts().size() > 0,
                "Blood pressure factory should capture alerts");
                
        assertTrue(testManager.getEcgAlertFactory().getCapturedAlerts().size() > 0,
                "ECG factory should capture alerts");
                
        assertTrue(testManager.getCallButtonAlertFactory().getCapturedAlerts().size() > 0,
                "Call button factory should capture alerts");
    }
}