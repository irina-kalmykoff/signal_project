package com.alerts;


import com.data_management.DataStorage;
import com.decorator.*;

/**
 * DecoratedAlertFactory extends AlertFactory to produce decorated alerts.
 * This factory uses the Decorator pattern to dynamically add functionality to alerts.
 */
public class DecoratedAlertFactory extends AlertFactory {

    /**
     * Constructor that initializes the factory with data storage.
     *
     * @param dataStorage the data storage system that provides access to patient data
     */
    public DecoratedAlertFactory(DataStorage dataStorage) {
        super(dataStorage);
    }

    /**
     * Creates and triggers a decorated alert based on the provided patient data.
     * This implementation decorates alerts with priority and/or repetition
     * based on the alert condition.
     *
     * @param patientId The unique identifier for the patient
     * @param condition The medical condition or reading that triggered the alert
     * @param timestamp The time when the alert condition was detected
     */
    @Override
    public void createAlert(String patientId, String condition, long timestamp) {
        // Create the base alert
        if (condition != null && !condition.isEmpty()) {
            // Create the base alert
            Alert alert = new Alert(patientId, condition, timestamp);

        // Apply decorators based on the alert condition
        Alert decoratedAlert = decorateAlert(alert);

        // Trigger the decorated alert
        triggerAlert(decoratedAlert);
    } else {
        // If no condition is detected, do nothing
    }}

    /**
     * Applies appropriate decorators to an alert based on its condition.
     *
     * @param alert the original alert to decorate
     * @return the decorated alert
     */
    protected Alert decorateAlert(Alert alert) {
        Alert decoratedAlert = alert;

        // Only apply decorators if there's an actual condition
        if (alert.getCondition() != null && !alert.getCondition().isEmpty()) {
            // Apply priority decorator based on condition
            if (alert.getCondition().contains("CRITICAL")) {
                decoratedAlert = new PriorityAlertDecorator(decoratedAlert, PriorityAlertDecorator.Priority.CRITICAL);
            } else if (alert.getCondition().contains("Hypotensive") ||
                    alert.getCondition().contains("Abnormality") ||
                    alert.getCondition().contains("extremely low")) {
                decoratedAlert = new PriorityAlertDecorator(decoratedAlert, PriorityAlertDecorator.Priority.HIGH);
            } else if (alert.getCondition().contains("Trend") ||
                    alert.getCondition().contains("Drop")) {
                decoratedAlert = new PriorityAlertDecorator(decoratedAlert, PriorityAlertDecorator.Priority.MEDIUM);
            } else {
                decoratedAlert = new PriorityAlertDecorator(decoratedAlert, PriorityAlertDecorator.Priority.LOW);
            }

            // Apply repeated alert decorator for high priority alerts
            if (decoratedAlert instanceof PriorityAlertDecorator) {
                PriorityAlertDecorator priorityAlert = (PriorityAlertDecorator) decoratedAlert;
                if (priorityAlert.isHighPriority()) {
                    RepeatedAlertDecorator repeatedAlert = new RepeatedAlertDecorator(
                            decoratedAlert,
                            60000, // Repeat every minute
                            5,     // Repeat up to 5 times
                            (repeatedAlertInstance) -> {
                                // This lambda will be called for each repetition
                                System.out.println("REPEATED ALERT: " + repeatedAlertInstance.getCondition() +
                                        " for Patient ID: " + repeatedAlertInstance.getPatientId() +
                                        " at " + new java.util.Date(repeatedAlertInstance.getTimestamp()));
                            }
                    );

                    repeatedAlert.startRepetition();
                    decoratedAlert = repeatedAlert;
                }
            }
        }
            return decoratedAlert;
    }

    /**
     * Overridden to handle decorated alerts properly.
     *
     * @param alert the alert object containing details about the alert condition
     */
    @Override
    protected void triggerAlert(Alert alert) {
        // Print information about the alert with any decorations
        StringBuilder message = new StringBuilder("ALERT: " + alert.getCondition() +
                " for Patient ID: " + alert.getPatientId() +
                " at " + new java.util.Date(alert.getTimestamp()));

        // Add additional information if this is a decorated alert
        if (alert instanceof PriorityAlertDecorator) {
            PriorityAlertDecorator priorityAlert = (PriorityAlertDecorator) alert;
            message.append(" [Priority: ").append(priorityAlert.getPriority().getLabel()).append("]");
        }

        if (alert instanceof RepeatedAlertDecorator) {
            RepeatedAlertDecorator repeatedAlert = (RepeatedAlertDecorator) alert;
            message.append(" [Repetition: ").append(repeatedAlert.getCurrentRepetition())
                    .append("/").append(repeatedAlert.getMaxRepetitions()).append("]");
        }

        System.out.println(message);
    }

    /**
     * Factory method to create a specific type of DecoratedAlertFactory.
     *
     * @param type the type of factory to create (e.g., "bloodPressure", "ecg")
     * @param dataStorage the data storage system
     * @return a specific implementation of DecoratedAlertFactory
     */
    public static DecoratedAlertFactory createFactory(String type, DataStorage dataStorage) {
        switch (type.toLowerCase()) {
            case "bloodoxygen":
                return new DecoratedBloodOxygenAlertFactory(dataStorage);
            case "bloodpressure":
                return new DecoratedBloodPressureAlertFactory(dataStorage);
            case "ecg":
                return new DecoratedECGAlertFactory(dataStorage);
            case "callbutton":
                return new DecoratedCallButtonAlertFactory(dataStorage);
            default:
                return new DecoratedAlertFactory(dataStorage);
        }
    }

    /**
     * Nested class for Blood Oxygen specific decorated alerts.
     */
    public static class DecoratedBloodOxygenAlertFactory extends DecoratedAlertFactory {
        public DecoratedBloodOxygenAlertFactory(DataStorage dataStorage) {
            super(dataStorage);
        }

        @Override
        public void createAlert(String patientId, String condition, long timestamp) {
            // Create the base alert
            Alert alert = new Alert(patientId, condition, timestamp);

            // Apply decorators (could add oxygen-specific decoration logic here)
            Alert decoratedAlert = decorateAlert(alert);

            // Trigger the decorated alert
            triggerAlert(decoratedAlert);
        }
    }

    /**
     * Nested class for Blood Pressure specific decorated alerts.
     */
    public static class DecoratedBloodPressureAlertFactory extends DecoratedAlertFactory {
        public DecoratedBloodPressureAlertFactory(DataStorage dataStorage) {
            super(dataStorage);
        }

        @Override
        public void createAlert(String patientId, String condition, long timestamp) {
            // Create the base alert
            Alert alert = new Alert(patientId, condition, timestamp);

            // Apply decorators (could add pressure-specific decoration logic here)
            Alert decoratedAlert = decorateAlert(alert);

            // Trigger the decorated alert
            triggerAlert(decoratedAlert);
        }
    }

    /**
     * Nested class for ECG specific decorated alerts.
     */
    public static class DecoratedECGAlertFactory extends DecoratedAlertFactory {
        public DecoratedECGAlertFactory(DataStorage dataStorage) {
            super(dataStorage);
        }

        @Override
        public void createAlert(String patientId, String condition, long timestamp) {
            // Create the base alert
            Alert alert = new Alert(patientId, condition, timestamp);

            // ECG alerts often need higher priority, so we could customize here
            Alert decoratedAlert = decorateAlert(alert);

            // Trigger the decorated alert
            triggerAlert(decoratedAlert);
        }
    }

    /**
     * Nested class for Call Button specific decorated alerts.
     */
    public static class DecoratedCallButtonAlertFactory extends DecoratedAlertFactory {
        public DecoratedCallButtonAlertFactory(DataStorage dataStorage) {
            super(dataStorage);
        }

        @Override
        public void createAlert(String patientId, String condition, long timestamp) {
            // Create the base alert
            Alert alert = new Alert(patientId, condition, timestamp);

            // Call button alerts are always high priority
            PriorityAlertDecorator priorityAlert = new PriorityAlertDecorator(
                    alert, PriorityAlertDecorator.Priority.HIGH);

            // Apply additional decorators
            Alert decoratedAlert = decorateAlert(priorityAlert);

            // Trigger the decorated alert
            triggerAlert(decoratedAlert);
        }
    }
}