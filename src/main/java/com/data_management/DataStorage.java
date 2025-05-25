package com.data_management;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicLong;
import com.alerts.AlertGenerator;


/**
 * Manages storage and retrieval of patient data within a healthcare monitoring
 * system with support for real-time data integration.
 * This class serves as a repository for all patient records, organized by
 * patient IDs, with thread-safe operations for concurrent data updates.
 */
public class DataStorage {
    // Thread-safe map to store patient objects indexed by their unique patient ID
    private final Map<Integer, Patient> patientMap;
    
    // Read-write locks for each patient to allow concurrent reads while ensuring exclusive writes
    private final Map<Integer, ReadWriteLock> patientLocks;
    
    // Counter for tracking total number of records processed
    private final AtomicLong totalRecordsProcessed;
    
    // Cache for frequently accessed patient records (last 1000 records per patient)
    private final Map<Integer, List<PatientRecord>> recentRecordsCache;
    private static final int CACHE_SIZE = 1000;
	
	private static DataStorage instance;        //a private static variable to hold the single instance of the class

    /**
     * Constructs a new instance of EnhancedDataStorage, initializing the underlying storage
     * structure with thread-safe collections for real-time data handling.
	 * Gets the singleton instance of DataStorage.
     * Creates a new instance if one doesn't exist yet.
	 @return The singleton instance of DataStorage
    */
    private DataStorage() {
        this.patientMap = new ConcurrentHashMap<>();
        this.patientLocks = new ConcurrentHashMap<>();
        this.totalRecordsProcessed = new AtomicLong(0);
        this.recentRecordsCache = new ConcurrentHashMap<>();
    }
	
	// Thread-safe getInstance method
	public static DataStorage getInstance() {
		if (instance == null) {
			synchronized (DataStorage.class) {
				if (instance == null) {
					instance = new DataStorage();
				}
			}
		}
		return instance;
	}

    /**
     * Adds or updates patient data in the storage with thread-safe operations.
     * If the patient does not exist, a new Patient object is created and added to
     * the storage. This method is optimized for concurrent access in real-time scenarios.
     *
     * @param patientId        the unique identifier of the patient
     * @param measurementValue the value of the health metric being recorded
     * @param recordType       the type of record, e.g., "HeartRate", "BloodPressure"
     * @param timestamp        the time at which the measurement was taken, in
     *                         milliseconds since the Unix epoch
     */
    public void addPatientData(int patientId, double measurementValue, String recordType, long timestamp) {
        // Validate input parameters
        if (patientId < 0) {
            System.err.println("Invalid patient ID (negative): " + patientId + " - skipping record");
            return;
        }
        
        if (recordType == null || recordType.trim().isEmpty()) {
            System.err.println("Invalid record type (null or empty) for patient " + patientId + " - skipping record");
            return;
        }
        
        if (timestamp < 0) {
            System.err.println("Invalid timestamp (negative) for patient " + patientId + " - skipping record");
            return;
        }

        // Get or create patient with thread-safe operations
        Patient patient = patientMap.computeIfAbsent(patientId, id -> {
            patientLocks.put(id, new ReentrantReadWriteLock());
            recentRecordsCache.put(id, new ArrayList<>());
            return new Patient(id);
        });

        // Get the lock for this specific patient
        ReadWriteLock lock = patientLocks.computeIfAbsent(patientId, id -> new ReentrantReadWriteLock());
        
        // Acquire write lock for adding data
        lock.writeLock().lock();
        try {
            // Check for duplicate records (same patient, type, and timestamp)
            if (!isDuplicateRecord(patientId, recordType, timestamp)) {
                // Add the record to the patient
                patient.addRecord(measurementValue, recordType, timestamp);
                
                // Update the recent records cache
                updateRecentRecordsCache(patientId, measurementValue, recordType, timestamp);
                
                // Increment the total records counter
                totalRecordsProcessed.incrementAndGet();
                
                // Log successful data addition for monitoring
                if (totalRecordsProcessed.get() % 1000 == 0) {
                    System.out.println("Processed " + totalRecordsProcessed.get() + " total records");
                }
            } else {
                System.out.println("Duplicate record detected for patient " + patientId + 
                                 ", type: " + recordType + ", timestamp: " + timestamp + " - skipping");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Checks if a record with the same patient ID, record type, and timestamp already exists.
     * This helps prevent duplicate data insertion during real-time streaming.
     *
     * @param patientId  the patient ID
     * @param recordType the type of record
     * @param timestamp  the timestamp of the record
     * @return true if a duplicate record exists, false otherwise
     */
    private boolean isDuplicateRecord(int patientId, String recordType, long timestamp) {
        List<PatientRecord> recentRecords = recentRecordsCache.get(patientId);
        if (recentRecords == null) {
            return false;
        }
        
        // Check the last few records for duplicates (more efficient than checking all records)
        int startIndex = Math.max(0, recentRecords.size() - 10); // Check last 10 records
        for (int i = startIndex; i < recentRecords.size(); i++) {
            PatientRecord record = recentRecords.get(i);
            if (record.getRecordType().equals(recordType) && record.getTimestamp() == timestamp) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the recent records cache for efficient duplicate detection and quick access.
     *
     * @param patientId        the patient ID
     * @param measurementValue the measurement value
     * @param recordType       the record type
     * @param timestamp        the timestamp
     */
    private void updateRecentRecordsCache(int patientId, double measurementValue, String recordType, long timestamp) {
        List<PatientRecord> recentRecords = recentRecordsCache.get(patientId);
        if (recentRecords != null) {
            PatientRecord newRecord = new PatientRecord(patientId, measurementValue, recordType, timestamp);
            recentRecords.add(newRecord);
            
            // Maintain cache size limit
            if (recentRecords.size() > CACHE_SIZE) {
                recentRecords.remove(0); // Remove oldest record
            }
        }
    }

    /**
     * Retrieves a list of PatientRecord objects for a specific patient, filtered by
     * a time range with thread-safe read operations.
     *
     * @param patientId the unique identifier of the patient whose records are to be
     *                  retrieved
     * @param startTime the start of the time range, in milliseconds since the Unix
     *                  epoch
     * @param endTime   the end of the time range, in milliseconds since the Unix
     *                  epoch
     * @return a list of PatientRecord objects that fall within the specified time
     *         range
     */
    public List<PatientRecord> getRecords(int patientId, long startTime, long endTime) {
        Patient patient = patientMap.get(patientId);
        if (patient == null) {
            return new ArrayList<>(); // return an empty list if no patient is found
        }

        ReadWriteLock lock = patientLocks.get(patientId);
        if (lock == null) {
            return new ArrayList<>();
        }

        // Acquire read lock for thread-safe access
        lock.readLock().lock();
        try {
            return patient.getRecords(startTime, endTime);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Retrieves the most recent records for a patient (useful for real-time monitoring).
     *
     * @param patientId the unique identifier of the patient
     * @param count     the maximum number of recent records to retrieve
     * @return a list of the most recent PatientRecord objects
     */
    public List<PatientRecord> getRecentRecords(int patientId, int count) {
        List<PatientRecord> recentRecords = recentRecordsCache.get(patientId);
        if (recentRecords == null || recentRecords.isEmpty()) {
            return new ArrayList<>();
        }

        ReadWriteLock lock = patientLocks.get(patientId);
        if (lock == null) {
            return new ArrayList<>();
        }

        lock.readLock().lock();
        try {
            int size = recentRecords.size();
            int startIndex = Math.max(0, size - count);
            return new ArrayList<>(recentRecords.subList(startIndex, size));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Retrieves a collection of all patients stored in the data storage.
     *
     * @return a list of all patients
     */
    public List<Patient> getAllPatients() {
        return new ArrayList<>(patientMap.values());
    }

    /**
     * Retrieves a patient by their ID.
     *
     * @param patientId the unique identifier of the patient
     * @return the patient object, or null if not found
     */
    public Patient getPatient(int patientId) {
        return patientMap.get(patientId);
    }

    /**
     * Gets the total number of records processed since the system started.
     * Useful for monitoring real-time data ingestion performance.
     *
     * @return the total number of records processed
     */
    public long getTotalRecordsProcessed() {
        return totalRecordsProcessed.get();
    }

    /**
     * Gets the number of unique patients currently in the system.
     *
     * @return the number of patients
     */
    public int getPatientCount() {
        return patientMap.size();
    }

    /**
     * Clears all patient data from the storage.
     * This method should be used with caution and primarily for testing purposes.
     */
    public synchronized void clearAllData() {
        patientMap.clear();
        patientLocks.clear();
        recentRecordsCache.clear();
        totalRecordsProcessed.set(0);
        System.out.println("All patient data cleared from storage");
    }

    /**
     * Provides real-time statistics about the data storage system.
     *
     * @return a formatted string with system statistics
     */
    public String getSystemStatistics() {
        return String.format("DataStorage Statistics:\n" +
                           "- Total Patients: %d\n" +
                           "- Total Records Processed: %d\n" +
                           "- Average Records per Patient: %.2f",
                           getPatientCount(),
                           getTotalRecordsProcessed(),
                           getPatientCount() > 0 ? (double) getTotalRecordsProcessed() / getPatientCount() : 0.0);
    }

    /**
     * The main method for the DataStorage class.
     * Initializes the system, reads data into storage, and continuously monitors
     * and evaluates patient data.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // DataReader is not defined in this scope, should be initialized appropriately.
        // DataReader reader = new SomeDataReaderImplementation("path/to/data");
        DataStorage storage = new DataStorage();

        // Assuming the reader has been properly initialized and can read data into the
        // storage
        // reader.readData(storage);

        // Example of using DataStorage to retrieve and print records for a patient
        List<PatientRecord> records = storage.getRecords(1, 1700000000000L, 1800000000000L);
        for (PatientRecord record : records) {
            System.out.println("Record for Patient ID: " + record.getPatientId() +
                    ", Type: " + record.getRecordType() +
                    ", Data: " + record.getMeasurementValue() +
                    ", Timestamp: " + record.getTimestamp());
        }

        // Initialize the AlertGenerator with the storage
        AlertGenerator alertGenerator = new AlertGenerator(storage);

        // Evaluate all patients' data to check for conditions that may trigger alerts
        for (Patient patient : storage.getAllPatients()) {
            alertGenerator.evaluateData(patient);
        }
    }
 
}
