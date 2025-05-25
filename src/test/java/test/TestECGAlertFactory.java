package test;
import java.util.*;
import com.data_management.*;
import com.alerts.*;

public class TestECGAlertFactory extends ECGAlertFactory {
    private AlertCapture alertCapture = new AlertCapture();
    
    public TestECGAlertFactory(DataStorage dataStorage) {
        super(dataStorage);
    }

    @Override
    protected void triggerAlert(Alert alert) {
        //System.out.println("DEBUG: TestECGAlertFactory captured alert: " + alert.getCondition());
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