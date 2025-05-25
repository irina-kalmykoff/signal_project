package com.data_management;

import com.cardio_generator.outputs.WebSocketDataReader;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Main application for monitoring real-time patient data via WebSocket.
 * This class demonstrates how to use the WebSocketDataReader to receive
 * and process streaming health data.
 */
public class RealTimeDataMonitor {
    private static DataStorage dataStorage;
    private static WebSocketDataReader webSocketReader;
    
    public static void main(String[] args) {
        System.out.println("=== Real-Time Patient Data Monitor ===");
        System.out.println("Starting WebSocket client to receive patient data...\n");
        
        // Initialize data storage
        dataStorage = DataStorage.getInstance();
        
        // Create WebSocket reader (connecting to localhost:8080 by default)
        String hostname = "localhost";
        int port = 8080;
        
        // Allow command line arguments to specify different host/port
        if (args.length >= 2) {
            hostname = args[0];
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default: 8080");
            }
        }
        
        webSocketReader = new WebSocketDataReader(hostname, port);
        
        try {
            // Start receiving real-time data
            System.out.println("Connecting to WebSocket server at " + hostname + ":" + port + "...");
            webSocketReader.startRealtimeReading(dataStorage);
            
            // Start monitoring thread to display statistics
            startMonitoringThread();
            
            // Interactive console for user commands
            handleUserInput();
            
        } catch (IOException e) {
            System.err.println("Error connecting to WebSocket server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    /**
     * Starts a background thread that periodically displays system statistics.
     */
    private static void startMonitoringThread() {
        Thread monitorThread = new Thread(() -> {
            while (webSocketReader.isReading()) {
                try {
                    Thread.sleep(5000); // Display stats every 5 seconds
                    displaySystemStats();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
        System.out.println("Monitoring thread started - statistics will be displayed every 5 seconds.\n");
    }
    
    /**
     * Displays current system statistics.
     */
    private static void displaySystemStats() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println(dataStorage.getSystemStatistics());
        System.out.println("Connection Status: " + (webSocketReader.isReading() ? "CONNECTED" : "DISCONNECTED"));
        System.out.println("=".repeat(50) + "\n");
    }
    
    /**
     * Handles user input for interactive commands.
     */
    private static void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Available commands:");
        System.out.println("  'stats' - Show current statistics");
        System.out.println("  'patients' - List all patients");
        System.out.println("  'records <patientId>' - Show recent records for a patient");
        System.out.println("  'help' - Show this help message");
        System.out.println("  'quit' - Exit the application");
        System.out.println();
        
        String input;
        while (!(input = getUserInput(scanner, "Enter command: ")).equalsIgnoreCase("quit")) {
            processCommand(input);
        }
        
        scanner.close();
    }
    
    /**
     * Gets user input with a prompt.
     */
    private static String getUserInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    
    /**
     * Processes user commands.
     */
    private static void processCommand(String command) {
        String[] parts = command.split("\\s+");
        String cmd = parts[0].toLowerCase();
        
        switch (cmd) {
            case "stats":
                displaySystemStats();
                break;
                
            case "patients":
                displayAllPatients();
                break;
                
            case "records":
                if (parts.length > 1) {
                    try {
                        int patientId = Integer.parseInt(parts[1]);
                        displayPatientRecords(patientId);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid patient ID. Please enter a valid number.");
                    }
                } else {
                    System.out.println("Please specify a patient ID. Usage: records <patientId>");
                }
                break;
                
            case "help":
                System.out.println("Available commands:");
                System.out.println("  'stats' - Show current statistics");
                System.out.println("  'patients' - List all patients");
                System.out.println("  'records <patientId>' - Show recent records for a patient");
                System.out.println("  'help' - Show this help message");
                System.out.println("  'quit' - Exit the application");
                break;
                
            default:
                System.out.println("Unknown command: " + command + ". Type 'help' for available commands.");
        }
        System.out.println();
    }
    
    /**
     * Displays information about all patients in the system.
     */
    private static void displayAllPatients() {
        List<Patient> patients = dataStorage.getAllPatients();
        
        if (patients.isEmpty()) {
            System.out.println("No patients found in the system.");
            return;
        }
        
        System.out.println("Current patients in the system:");
        System.out.println("-".repeat(30));
        
        for (Patient patient : patients) {
            List<PatientRecord> recentRecords = dataStorage.getRecentRecords(patient.getPatientId(), 5);
            System.out.printf("Patient ID: %d (Recent records: %d)%n", 
                            patient.getPatientId(), recentRecords.size());
        }
    }
    
    /**
     * Displays recent records for a specific patient.
     */
    private static void displayPatientRecords(int patientId) {
        Patient patient = dataStorage.getPatient(patientId);
        
        if (patient == null) {
            System.out.println("Patient ID " + patientId + " not found in the system.");
            return;
        }
        
        List<PatientRecord> recentRecords = dataStorage.getRecentRecords(patientId, 10);
        
        if (recentRecords.isEmpty()) {
            System.out.println("No records found for Patient ID " + patientId);
            return;
        }
        
        System.out.println("Recent records for Patient ID " + patientId + ":");
        System.out.println("-".repeat(80));
        System.out.printf("%-15s %-15s %-20s %-15s%n", "Timestamp", "Record Type", "Value", "Time (Human)");
        System.out.println("-".repeat(80));
        
        for (PatientRecord record : recentRecords) {
            String humanTime = new java.util.Date(record.getTimestamp()).toString();
            System.out.printf("%-15d %-15s %-20.2f %-15s%n", 
                            record.getTimestamp(), 
                            record.getRecordType(), 
                            record.getMeasurementValue(),
                            humanTime.substring(11, 19)); // Just show time portion
        }
    }
    
    /**
     * Performs cleanup operations when shutting down.
     */
    private static void cleanup() {
        System.out.println("\nShutting down...");
        if (webSocketReader != null) {
            webSocketReader.stopRealtimeReading();
        }
        System.out.println("Application terminated.");
    }
}