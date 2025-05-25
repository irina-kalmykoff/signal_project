package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.alerts.*;
import com.data_management.*;
import com.decorator.*;
import java.util.List;
import java.lang.reflect.Field;

/**
 * Test class focused on testing the decorator pattern implementation for alerts
 */
public class DecoratorPatternTest {

    private DataStorage dataStorage;
    private TestDecoratedAlertFactory decoratedFactory;

    @BeforeEach
    public void setUp() {
        // Reset DataStorage singleton between tests
        resetDataStorage();

        // Get the singleton instance of DataStorage
        dataStorage = DataStorage.getInstance();

        // Create the factory being tested
        decoratedFactory = new TestDecoratedAlertFactory(dataStorage);
    }

    @AfterEach
    public void cleanUp() {
        decoratedFactory.clearAlerts();
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
    public void testBasicAlert() {
        // Create a basic alert
        Alert alert = new Alert("100", "Test Alert", System.currentTimeMillis());

        // Verify basic properties
        assertEquals("100", alert.getPatientId(), "Alert should have correct patient ID");
        assertEquals("Test Alert", alert.getCondition(), "Alert should have correct condition");
    }

    @Test
    public void testPriorityDecorator() {
        // Create a basic alert
        Alert baseAlert = new Alert("101", "Test Alert", System.currentTimeMillis());

        // Decorate with priority
        PriorityAlertDecorator priorityAlert = new PriorityAlertDecorator(
                baseAlert, PriorityAlertDecorator.Priority.HIGH);

        // Verify decorator properties
        assertEquals("101", priorityAlert.getPatientId(), "Decorated alert should preserve patient ID");
        assertEquals("[HIGH] Test Alert", priorityAlert.getCondition(), "Decorated alert should add priority prefix");
        assertEquals(PriorityAlertDecorator.Priority.HIGH, priorityAlert.getPriority(), "Decorated alert should store priority");
        assertTrue(priorityAlert.isHighPriority(), "HIGH priority should be considered high priority");
    }

    @Test
    public void testRepeatedDecorator() {
        // Create a basic alert
        Alert baseAlert = new Alert("102", "Test Alert", System.currentTimeMillis());

        // Setup a counter to track repeated alert calls
        final int[] callCount = {0};

        // Decorate with repetition
        RepeatedAlertDecorator repeatedAlert = new RepeatedAlertDecorator(
                baseAlert, 100, 3, alert -> callCount[0]++);

        // Verify initial state
        assertEquals("102", repeatedAlert.getPatientId(), "Decorated alert should preserve patient ID");
        assertEquals("Test Alert", repeatedAlert.getCondition(), "Condition should be preserved initially");
        assertEquals(0, repeatedAlert.getCurrentRepetition(), "Initial repetition count should be 0");
        assertEquals(3, repeatedAlert.getMaxRepetitions(), "Max repetitions should be preserved");

        // Start repetition and wait for completion
        repeatedAlert.startRepetition();

        try {
            // Wait for repetitions to complete (3 * 100ms = 300ms, plus buffer)
            Thread.sleep(500);
        } catch (InterruptedException e) {
            fail("Test interrupted during sleep");
        }

        // Stop repetition to clean up
        repeatedAlert.stopRepetition();

        // Verify that handler was called for each repetition
        assertEquals(3, callCount[0], "Handler should be called for each repetition");
    }

    @Test
    public void testNestedDecorators() {
        // Create a basic alert
        Alert baseAlert = new Alert("103", "Test Alert", System.currentTimeMillis());

        // Decorate with priority
        PriorityAlertDecorator priorityAlert = new PriorityAlertDecorator(
                baseAlert, PriorityAlertDecorator.Priority.CRITICAL);

        // Decorate the priority alert with repetition
        RepeatedAlertDecorator repeatedPriorityAlert = new RepeatedAlertDecorator(
                priorityAlert, 100, 1, alert -> {});

        // Verify nested properties
        assertEquals("103", repeatedPriorityAlert.getPatientId(), "Nested alert should preserve patient ID");
        assertEquals("[CRITICAL] Test Alert", repeatedPriorityAlert.getCondition(),
                "Nested alert should maintain all decorations");
    }

    @Test
    public void testDecoratedAlertFactory() {
        // Create and trigger an alert
        decoratedFactory.createAlert("104", "Hypotensive Hypoxemia detected", System.currentTimeMillis());

        // Get captured alerts
        List<Alert> alerts = decoratedFactory.getCapturedAlerts();

        // Verify alert was captured
        assertEquals(1, alerts.size(), "Factory should create one alert");

        // Verify that the condition contains both original text and priority decorations
        String condition = alerts.get(0).getCondition();
        assertTrue(condition.contains("Hypotensive Hypoxemia"),
                "Condition should preserve original text");
        assertTrue(condition.contains("[HIGH]"),
                "Factory should add HIGH priority to hypotensive hypoxemia alerts");
    }

    @Test
    public void testDecoratorPriorityLogic() {
        // Test CRITICAL priority
        decoratedFactory.createAlert("105", "CRITICAL condition detected", System.currentTimeMillis());
        assertTrue(decoratedFactory.containsAlertWithKeyword("[CRITICAL]"),
                "CRITICAL keyword should trigger CRITICAL priority");
        decoratedFactory.clearAlerts();

        // Test HIGH priority
        decoratedFactory.createAlert("105", "Hypotensive condition detected", System.currentTimeMillis());
        assertTrue(decoratedFactory.containsAlertWithKeyword("[HIGH]"),
                "Hypotensive keyword should trigger HIGH priority");
        decoratedFactory.clearAlerts();

        // Test MEDIUM priority
        decoratedFactory.createAlert("105", "Blood Pressure Trend detected", System.currentTimeMillis());
        assertTrue(decoratedFactory.containsAlertWithKeyword("[MEDIUM]"),
                "Trend keyword should trigger MEDIUM priority");
        decoratedFactory.clearAlerts();

        // Test LOW priority (default)
        decoratedFactory.createAlert("105", "Minor condition detected", System.currentTimeMillis());
        assertTrue(decoratedFactory.containsAlertWithKeyword("[LOW]"),
                "Other conditions should default to LOW priority");
    }
}