package com.data_management;

import java.io.IOException;

/**
 * Important: This interface is not modified for the assignment for week 5.
 * The reason for this is that modifying this interface would also require modifying the {@link FileDataReader} class.
 * This violates the open closed principle.
 * Therefore, a new interface {@link ContinuousDataReader} is created that extends the DataReader interface.
 * This ensures that the {@link FileDataReader} class does not need to be modified and will still work as before.
 */

public interface DataReader {
    /**
     * Reads data from a specified source and stores it in the data storage.
     * 
     * @param dataStorage the storage where data will be stored
     * @throws IOException if there is an error reading the data
     */
    void readData(DataStorage dataStorage) throws IOException;
}
