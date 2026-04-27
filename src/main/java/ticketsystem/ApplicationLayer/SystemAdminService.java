package ticketsystem.ApplicationLayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;

public class SystemAdminService {

    private final ISystemAdminRepository adminRepository;
    private final IPaymentService paymentService;
    private final ISecureBarcode barcodeService;
    private static final String EVENT_LOG_PATH = "logs/event.log";
    private static final String ERROR_LOG_PATH = "logs/error.log";
    private final ISystemLogger logger;

    public SystemAdminService(ISystemAdminRepository adminRepository,
            IPaymentService paymentService,
            ISecureBarcode barcodeService, ISystemLogger logger) {
        this.adminRepository = adminRepository;
        this.paymentService = paymentService;
        this.barcodeService = barcodeService;
        this.logger = logger;
    }

//Use Case: Ticket System Initialization
    public boolean initSystem() {
        try {
            if (adminRepository.countAdmins() == 0) {
                //System.out.println("No system admins found. Please create an admin account to initialize the system.");
                logger.logError("initSystem", "No system admins found. Please create an admin account to initialize the system.", null);
                return false;
            }

            boolean paymentConnected = paymentService.connect();
            if (!paymentConnected) {
                // Alternative Flow: Connection failure
                //System.out.println("Failed to connect to payment services. Please check your configuration.");
                logger.logError("initSystem", "Failed to connect to payment services. Please check your configuration.", null);
                return false;
            }

            boolean barcodeConnected = barcodeService.connect();
            if (!barcodeConnected) {
                // Alternative Flow: Connection failure
                //System.out.println("Failed to connect to secure barcode services.");
                logger.logError("initSystem", "Failed to connect to secure barcode services.", null);
                return false;
            }

            //System.out.println("System initialized successfully by System Admin.");
            logger.logEvent("initSystem", "System initialized successfully by System Admin.");
            return true;

        } catch (Exception e) {
            //System.out.println("Unexpected error during initialization: " + e.getMessage());
            logger.logError("initSystem", "Unexpected error during initialization: " + e.getMessage(), null);
            return false;
        }
    }

// View System Logs, documentation
    public List<String> viewEventLogs() {
        logger.logEvent("viewEventLogs", "Viewing event logs.");
        return readLastLogLines(EVENT_LOG_PATH, 200);
    }

    public List<String> viewErrorLogs() {
        logger.logEvent("viewErrorLogs", "Viewing error logs.");
        return readLastLogLines(ERROR_LOG_PATH, 200);
    }

    private List<String> readLastLogLines(String filePath, int maxLines) {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) { // if log file doesn't exist
            return Collections.singletonList("Log file not found or empty: " + filePath);
        }

        try {
            List<String> allLines = Files.readAllLines(path);
            if (allLines.size() <= maxLines) {
                return allLines;
            } else {
                return allLines.subList(allLines.size() - maxLines, allLines.size());
            }

        } catch (IOException e) {
            return Collections.singletonList("Failed to read logs: " + e.getMessage());
        }
    }

}
