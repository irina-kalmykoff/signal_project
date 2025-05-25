package com.decorator;
import com.alerts.*;

/**
        * A decorator that adds priority level information to alerts.
 * This allows for dynamic prioritization of alerts based on their severity.
        */
public class PriorityAlertDecorator extends AlertDecorator {
    /**
     * Enum representing different priority levels for medical alerts.
     */
    public enum Priority {
        LOW(1, "LOW"),
        MEDIUM(2, "MEDIUM"),
        HIGH(3, "HIGH"),
        CRITICAL(4, "CRITICAL");

        private final int level;
        private final String label;

        Priority(int level, String label) {
            this.level = level;
            this.label = label;
        }

        public int getLevel() {
            return level;
        }

        public String getLabel() {
            return label;
        }
    }

    private Priority priority;

    /**
     * Constructor for the PriorityAlertDecorator.
     *
     * @param decoratedAlert the alert to be prioritized
     * @param priority the priority level to assign
     */
    public PriorityAlertDecorator(Alert decoratedAlert, Priority priority) {
        super(decoratedAlert);
        this.priority = priority;
    }

    /**
     * Gets the condition with the priority prefix.
     *
     * @return the prioritized condition
     */
    @Override
    public String getCondition() {
        return "[" + priority.getLabel() + "] " + super.getCondition();
    }

    /**
     * Gets the current priority level.
     *
     * @return the priority
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * Sets a new priority level.
     *
     * @param priority the new priority level
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * Determines if this alert is high priority (HIGH or CRITICAL).
     *
     * @return true if the alert is high priority
     */
    public boolean isHighPriority() {
        return priority == Priority.HIGH || priority == Priority.CRITICAL;
    }

    /**
     * Determines if this alert is critical priority.
     *
     * @return true if the alert is critical priority
     */
    public boolean isCritical() {
        return priority == Priority.CRITICAL;
    }
}