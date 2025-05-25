package com.decorator;
import com.alerts.*;
import java.util.*;

/**
 * A decorator that adds the ability to repeat alerts at specified intervals.
 * This is useful for critical conditions that require continuous attention.
 */
public class RepeatedAlertDecorator extends AlertDecorator {
    private final long repeatInterval; // interval in milliseconds
    private final int maxRepetitions;
    private int currentRepetition = 0;
    private Timer timer;
    private AlertHandler alertHandler;

    /**
     * Interface for handling the repeated alert notifications.
     * This allows for flexibility in how the repeated alerts are processed.
     */
    public interface AlertHandler {
        void handleAlert(Alert alert);
    }

    /**
     * Constructor for the RepeatedAlertDecorator.
     *
     * @param decoratedAlert the alert to be repeated
     * @param repeatInterval the interval between repetitions in milliseconds
     * @param maxRepetitions the maximum number of times to repeat the alert (0 for indefinite)
     * @param alertHandler the handler to process each repeated alert
     */
    public RepeatedAlertDecorator(Alert decoratedAlert, long repeatInterval, int maxRepetitions, AlertHandler alertHandler) {
        super(decoratedAlert);
        this.repeatInterval = repeatInterval;
        this.maxRepetitions = maxRepetitions;
        this.alertHandler = alertHandler;
    }

    /**
     * Starts the alert repetition schedule.
     */
    public void startRepetition() {
        timer = new Timer(true); // Create a daemon timer
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (maxRepetitions > 0 && currentRepetition >= maxRepetitions) {
                    timer.cancel();
                    return;
                }

                currentRepetition++;
                if (alertHandler != null) {
                    // Create a new alert with updated timestamp for each repetition
                    Alert repeatedAlert = new Alert(
                            getPatientId(),
                            "[REPEATED] " + getCondition(),
                            System.currentTimeMillis()
                    );
                    alertHandler.handleAlert(repeatedAlert);
                }
            }
        }, repeatInterval, repeatInterval);
    }

    /**
     * Stops the alert repetition.
     */
    public void stopRepetition() {
        if (timer != null) {
            timer.cancel();
        }
    }

    /**
     * Gets the current repetition count.
     *
     * @return the number of times the alert has been repeated
     */
    public int getCurrentRepetition() {
        return currentRepetition;
    }

    /**
     * Gets the maximum number of repetitions.
     *
     * @return the maximum repetition count (0 for indefinite)
     */
    public int getMaxRepetitions() {
        return maxRepetitions;
    }

    /**
     * Gets the repeat interval in milliseconds.
     *
     * @return the interval between repetitions
     */
    public long getRepeatInterval() {
        return repeatInterval;
    }

    /**
     * Sets a new alert handler.
     *
     * @param alertHandler the new handler for repeated alerts
     */
    public void setAlertHandler(AlertHandler alertHandler) {
        this.alertHandler = alertHandler;
    }
}