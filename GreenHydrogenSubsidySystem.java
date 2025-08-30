import java.sql.*;
import java.util.Scanner;
import java.util.UUID;

public class GreenHydrogenSubsidySystem {

    // --- Database & System Configuration ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/green_ddbb";
    private static final String DB_USER = "root";
    private static final String DB_PASS = ""; // Your MySQL password
    private static final double SUBSIDY_RATE = 2.5; // $2.5 per kg

    private Connection con;

    // Constructor to hold the database connection
    public GreenHydrogenSubsidySystem(Connection con) {
        this.con = con;
    }

    public static void main(String[] args) {
        Connection con = null;
        try {
            // 1. Establish the database connection
            con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            if (con != null) {
                System.out.println("Connected to the database successfully.");
                GreenHydrogenSubsidySystem system = new GreenHydrogenSubsidySystem(con);

                // 2. Setup database tables if they don't exist
                system.setupDatabaseSchema();

                // 3. Start the main application logic
                system.run();
            } else {
                System.out.println("Failed to connect to the database.");
            }
        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
            // A common error is the database 'green_ddbb' not existing.
            if (e.getMessage().contains("Unknown database")) {
                System.err.println("Please create the database 'green_ddbb' first using a MySQL client.");
            }
        } finally {
            // 4. Close the connection when the program ends
            if (con != null) {
                try {
                    con.close();
                    System.out.println("Database connection closed.");
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Checks for and creates all necessary tables if they do not already exist.
     */
    public void setupDatabaseSchema() throws SQLException {
        System.out.println("Verifying database schema...");
        try (Statement stmt = con.createStatement()) {

            // Use CREATE TABLE IF NOT EXISTS to prevent errors on subsequent runs
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "email VARCHAR(255) UNIQUE NOT NULL, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(50) NOT NULL, " +
                    "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";

            String createProjectsTable = "CREATE TABLE IF NOT EXISTS projects (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "producerId VARCHAR(36) NOT NULL, " +
                    "estimatedProduction DOUBLE NOT NULL, " +
                    "totalSubsidyAllocated DOUBLE DEFAULT 0, " +
                    "status VARCHAR(50) NOT NULL, " +
                    "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (producerId) REFERENCES users(id)" +
                    ");";

            String createMilestonesTable = "CREATE TABLE IF NOT EXISTS milestones (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "projectId VARCHAR(36) NOT NULL, " +
                    "targetProduction DOUBLE NOT NULL, " +
                    "verifiedProduction DOUBLE DEFAULT 0, " +
                    "subsidyAmount DOUBLE DEFAULT 0, " +
                    "status VARCHAR(50) NOT NULL, " +
                    "verificationTimestamp TIMESTAMP NULL, " +
                    "paymentTimestamp TIMESTAMP NULL, " +
                    "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (projectId) REFERENCES projects(id)" +
                    ");";

            String createPaymentsTable = "CREATE TABLE IF NOT EXISTS subsidy_payments (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "milestoneId VARCHAR(36) NOT NULL, " +
                    "projectId VARCHAR(36) NOT NULL, " +
                    "amount DOUBLE NOT NULL, " +
                    "status VARCHAR(50) NOT NULL, " +
                    "transactionId VARCHAR(255) NOT NULL, " +
                    "paymentDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (milestoneId) REFERENCES milestones(id), " +
                    "FOREIGN KEY (projectId) REFERENCES projects(id)" +
                    ");";

            stmt.executeUpdate(createUsersTable);
            stmt.executeUpdate(createProjectsTable);
            stmt.executeUpdate(createMilestonesTable);
            stmt.executeUpdate(createPaymentsTable);

            System.out.println("Database schema is ready.");
        }
    }

    /**
     * Main application loop to handle user interaction.
     */
    public void run() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- Green Hydrogen Subsidy Portal ---");
            System.out.println("1. Register as a new Producer");
            System.out.println("2. Login for existing Producer");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            if ("1".equals(choice)) {
                System.out.print("Enter company name: ");
                String name = scanner.nextLine();
                System.out.print("Enter email: ");
                String email = scanner.nextLine();
                System.out.print("Enter password: ");
                String password = scanner.nextLine();
                registerProducer(name, email, password);
            } else if ("2".equals(choice)) {
                System.out.print("Enter email: ");
                String email = scanner.nextLine();
                System.out.print("Enter password: ");
                String password = scanner.nextLine();
                String producerId = loginProducer(email, password);
                if (producerId != null) {
                    producerMenu(scanner, producerId);
                }
            } else if ("3".equals(choice)) {
                System.out.println("Exiting...");
                break;
            } else {
                System.out.println("Invalid option. Please try again.");
            }
        }
        scanner.close();
    }

    /**
     * Handles the menu for a logged-in producer.
     */
    private void producerMenu(Scanner scanner, String producerId) throws SQLException {
        while (true) {
            System.out.println("\n--- Producer Dashboard ---");
            System.out.println("1. Register a new Project");
            System.out.println("2. Submit a Milestone for a Project");
            System.out.println("3. (Demo) Verify a Milestone");
            System.out.println("4. (Demo) Disburse Subsidy for Verified Milestone");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            if ("1".equals(choice)) {
                System.out.print("Enter project name: ");
                String projectName = scanner.nextLine();
                registerProject(projectName, producerId);
            } else if ("2".equals(choice)) {
                System.out.print("Enter Project ID to submit milestone for: ");
                String projectId = scanner.nextLine();
                System.out.print("Enter verified production volume (kg): ");
                double volume = Double.parseDouble(scanner.nextLine());
                submitMilestone(projectId, producerId, volume);
            } else if ("3".equals(choice)) {
                System.out.print("Enter Milestone ID to verify: ");
                String milestoneId = scanner.nextLine();
                verifyMilestone(milestoneId);
            } else if ("4".equals(choice)) {
                System.out.print("Enter Milestone ID to disburse subsidy for: ");
                String milestoneId = scanner.nextLine();
                disburseSubsidy(milestoneId);
            } else if ("5".equals(choice)) {
                System.out.println("Logging out...");
                break;
            } else {
                System.out.println("Invalid option.");
            }
        }
    }


    // --- Backend Logic Methods ---

    public void registerProducer(String name, String email, String password) throws SQLException {
        String checkSql = "SELECT id FROM users WHERE email = ?";
        try (PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
            checkStmt.setString(1, email);
            if (checkStmt.executeQuery().next()) {
                System.out.println("Error: A user with this email already exists.");
                return;
            }
        }

        String sql = "INSERT INTO users (id, name, email, password, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, name);
            pstmt.setString(3, email);
            pstmt.setString(4, password); // In a real app, hash the password!
            pstmt.setString(5, "PRODUCER");
            if (pstmt.executeUpdate() > 0) {
                System.out.println("Producer registered successfully!");
            }
        }
    }

    public String loginProducer(String email, String password) throws SQLException {
        String sql = "SELECT id FROM users WHERE email = ? AND password = ? AND role = 'PRODUCER'";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("Login successful!");
                return rs.getString("id");
            } else {
                System.out.println("Login failed. Check your email and password.");
                return null;
            }
        }
    }

    public void registerProject(String name, String producerId) throws SQLException {
        String sql = "INSERT INTO projects (id, name, producerId, estimatedProduction, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, name);
            pstmt.setString(3, producerId);
            pstmt.setDouble(4, 100000.0); // Default estimated production
            pstmt.setString(5, "PENDING_APPROVAL");
            pstmt.executeUpdate();
            System.out.println("Project '" + name + "' registered and is pending approval.");
        }
    }

    public void submitMilestone(String projectId, String producerId, double volume) throws SQLException {
        String checkSql = "SELECT id FROM projects WHERE id = ? AND producerId = ?";
        try (PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
            checkStmt.setString(1, projectId);
            checkStmt.setString(2, producerId);
            if (!checkStmt.executeQuery().next()) {
                System.out.println("Error: Project not found or you do not have permission.");
                return;
            }
        }

        String sql = "INSERT INTO milestones (id, projectId, targetProduction, verifiedProduction, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, projectId);
            pstmt.setDouble(3, volume);
            pstmt.setDouble(4, volume);
            pstmt.setString(5, "PENDING_VERIFICATION");
            pstmt.executeUpdate();
            System.out.println("Milestone submitted for Project " + projectId + " and is pending verification.");
        }
    }

    public void verifyMilestone(String milestoneId) throws SQLException {
        String sql = "UPDATE milestones SET status = 'VERIFIED', verificationTimestamp = NOW() WHERE id = ? AND status = 'PENDING_VERIFICATION'";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, milestoneId);
            if (pstmt.executeUpdate() > 0) {
                System.out.println("Milestone " + milestoneId + " has been verified.");
            } else {
                System.out.println("Error: Milestone not found or not in a state that can be verified.");
            }
        }
    }

    public void disburseSubsidy(String milestoneId) throws SQLException {
        String sqlSelect = "SELECT projectId, verifiedProduction FROM milestones WHERE id = ? AND status = 'VERIFIED'";
        try (PreparedStatement pstmt = con.prepareStatement(sqlSelect)) {
            pstmt.setString(1, milestoneId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String projectId = rs.getString("projectId");
                double volume = rs.getDouble("verifiedProduction");
                double amount = volume * SUBSIDY_RATE;

                con.setAutoCommit(false); // Start transaction
                try {
                    String paymentSql = "INSERT INTO subsidy_payments (id, milestoneId, projectId, amount, status, transactionId) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pStmt = con.prepareStatement(paymentSql)) {
                        pStmt.setString(1, UUID.randomUUID().toString());
                        pStmt.setString(2, milestoneId);
                        pStmt.setString(3, projectId);
                        pStmt.setDouble(4, amount);
                        pStmt.setString(5, "COMPLETED");
                        pStmt.setString(6, "TXN-" + System.currentTimeMillis());
                        pStmt.executeUpdate();
                    }

                    String updateSql = "UPDATE milestones SET status = 'PAID', paymentTimestamp = NOW() WHERE id = ?";
                    try (PreparedStatement uStmt = con.prepareStatement(updateSql)) {
                        uStmt.setString(1, milestoneId);
                        uStmt.executeUpdate();
                    }
                    con.commit(); // Commit transaction
                    System.out.printf("Subsidy of $%.2f disbursed for milestone %s.\n", amount, milestoneId);

                } catch (SQLException e) {
                    con.rollback(); // Rollback on error
                    System.err.println("Transaction failed, rolling back changes.");
                    throw e;
                } finally {
                    con.setAutoCommit(true); // Reset auto-commit
                }
            } else {
                System.out.println("Error: Milestone cannot be paid. It must be in 'VERIFIED' status.");
            }
        }
    }
}
