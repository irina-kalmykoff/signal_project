package data_management;

import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
//import org.mockito.Mock;

import com.data_management.DataStorage;
import com.data_management.PatientRecord;
//import com.data_management.DataReader;

import java.util.List;

class DataStorageTest {

    // ik added mock data reader to satisfy TODO below
    //    @Mock
    //    private DataReader reader;  // Mock reader instead of using a real one

    @Test
    void testAddAndGetRecords() {
        // TODO Perhaps you can implement a mock data reader to mock the test data?
        // ik - added mock data deader
        //        reader = mock(DataReader.class); //check later if needed at all
        // DataReader reader
        //DataStorage storage = new DataStorage(reader);
        DataStorage storage = DataStorage.getInstance();

        storage.addPatientData(1, 100.0, "WhiteBloodCells", 1714376789050L);
        storage.addPatientData(1, 200.0, "WhiteBloodCells", 1714376789051L);

        List<PatientRecord> records = storage.getRecords(1, 1714376789050L, 1714376789051L);
        assertEquals(2, records.size()); // Check if two records are retrieved
        assertEquals(100.0, records.get(0).getMeasurementValue()); // Validate first record
    }
}
