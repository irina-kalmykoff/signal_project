package com.decorator;
import com.alerts.*;


/**
 * Abstract decorator class for Alert objects.
 * Follows the Decorator pattern to add additional functionality to alerts dynamically.
 */
public abstract class AlertDecorator extends Alert {
    protected final Alert decoratedAlert;

    /**
     * Constructor for the AlertDecorator.
     *
     * @param decoratedAlert the alert being decorated
     */
    public AlertDecorator(Alert decoratedAlert) {
        // Call the parent constructor with the decorated alert's properties
        super(decoratedAlert.getPatientId(), decoratedAlert.getCondition(), decoratedAlert.getTimestamp());
        this.decoratedAlert = decoratedAlert;
    }

    /**
     * Gets the patient ID from the decorated alert.
     * This method is overridden to ensure we always get the latest value from the decorated alert.
     *
     * @return the patient ID
     */
    @Override
    public String getPatientId() {
        return decoratedAlert.getPatientId();
    }

    /**
     * Gets the condition from the decorated alert.
     * This method is overridden to ensure we always get the latest value from the decorated alert.
     *
     * @return the condition
     */
    @Override
    public String getCondition() {
        return decoratedAlert.getCondition();
    }

    /**
     * Gets the timestamp from the decorated alert.
     * This method is overridden to ensure we always get the latest value from the decorated alert.
     *
     * @return the timestamp
     */
    @Override
    public long getTimestamp() {
        return decoratedAlert.getTimestamp();
    }
}