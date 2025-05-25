package test;
import com.alerts.*;
import java.util.*;


public class AlertCapture {
    private List<Alert> capturedAlerts = new ArrayList<>();

    public void captureAlert (Alert alert) {
        capturedAlerts.add(alert);
        System.out.println("TEST ALERT: " + alert.getCondition() + 
                " for Patient ID: " + alert.getPatientId());
    }

    public List<Alert> getCapturedAlerts() {
        return capturedAlerts;
    }

    public void clearAlerts() {
        capturedAlerts.clear();
    }

    public boolean containsAlertWithKeyword(String keyword) {
        for (Alert alert : capturedAlerts) {
            if (alert.getCondition().contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
