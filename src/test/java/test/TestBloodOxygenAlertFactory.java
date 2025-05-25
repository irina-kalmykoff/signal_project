package test;
import com.data_management.*;
import com.alerts.*;
import java.util.*;

public class TestBloodOxygenAlertFactory extends BloodOxygenAlertFactory {
    private AlertCapture alertCapture = new AlertCapture();

    public TestBloodOxygenAlertFactory(DataStorage dataStorage) {
        super(dataStorage);
    }

    @Override
    protected void triggerAlert(Alert alert) {
        //System.out.println("DEBUG: TestBloodOxygenAlertFactory captured alert: " + alert.getCondition());
        alertCapture.captureAlert(alert);
    }

    public List<Alert> getCapturedAlerts() {
        return alertCapture.getCapturedAlerts();
    }
    
    public void clearAlerts() {
        alertCapture.clearAlerts();
    }
    
    public boolean containsAlertWithKeyword(String keyword) {
        return alertCapture.containsAlertWithKeyword(keyword);
    }

}