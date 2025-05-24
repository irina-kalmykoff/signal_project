package com.data_management;

import java.io.IOException;

/**
    * Interface for reading data from various sources and storing it in the data storage {@link DataStorage}.
    * Supports both batch reading (one-time), by extending the {@link DataReader} interface, and continuous real-time data streaming.
*/
public interface ContinuousDataReader extends DataReader{
    /**
     * Starts continuous reading of data from a stream source.
     * For real-time data sources like WebSockets, this method initiates
     * the connection and begins processing incoming data continuously.
     * 
     * @param dataStorage the storage where streaming data will be stored
     * @throws IOException if there is an error establishing the connection
     */
    void startRealtimeReading(DataStorage dataStorage) throws IOException;
    
    /**
     * Stops the continuous reading process and cleans up resources.
     * This method should properly close any open connections and perform
     * necessary cleanup to prevent resource leaks.
     */
    void stopRealtimeReading();
}
