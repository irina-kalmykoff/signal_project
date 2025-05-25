package test;
import java.lang.reflect.Field;
import com.data_management.*;

public class TestFixture {
    /**
     * Resets the DataStorage singleton between tests
     */
    public static void resetDataStorage() {
        try {
            Field instance = DataStorage.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            System.err.println("Failed to reset DataStorage singleton: " + e.getMessage());
        }
    }
}
