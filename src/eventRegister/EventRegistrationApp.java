package eventRegister;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

class DBConnection {
    static final String DB_URL = "jdbc:mysql://localhost:3306/eventdb";
    static final String USER = "root";
    static final String PASS = "";

    public static Connection connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found.");
            e.printStackTrace();
        }
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }
}

class User {
    int userId;
    String name, email, password;

    void register(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter name: ");
        name = sc.nextLine();
        System.out.print("Enter email: ");
        email = sc.nextLine();
        System.out.print("Enter password: ");
        password = sc.nextLine();

        String checkSql = "SELECT userId FROM user WHERE name = ? AND email = ? AND password = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, name);
            checkStmt.setString(2, email);
            checkStmt.setString(3, password);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                System.out.println("Error: User is already registered with the same name, email, and password.");
                return;
            }
        }

        String insertSql = "INSERT INTO user (name, email, password) VALUES (?, ?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, name);
            insertStmt.setString(2, email);
            insertStmt.setString(3, password);
            insertStmt.executeUpdate();
            System.out.println("User registered successfully!");
        }
    }

    boolean login(Connection conn, String email, String password) throws SQLException {
        String sql = "SELECT * FROM user WHERE email=? AND password=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("userId");
                name = rs.getString("name");
                System.out.println("Login successful. Welcome, " + name);
                return true;
            } else {
                System.out.println("Invalid credentials.");
                return false;
            }
        }
    }

    void viewEvent(Connection conn) throws SQLException {
        String sql = "SELECT * FROM event WHERE approved = 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            boolean hasEvents = false;
            while (rs.next()) {
                hasEvents = true;
                System.out.println("Event ID: " + rs.getInt("eventId") + ", Title: " + rs.getString("title") +
                                   ", Date: " + rs.getDate("date") + ", Price: " + rs.getDouble("price"));
            }
            if (!hasEvents) {
                System.out.println("No approved events available.");
            }
        }
    }

    void bookEvent(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter Event ID to book: ");
        if (sc.hasNextInt()) {
            int eventId = sc.nextInt();
            sc.nextLine();

            String checkSql = "SELECT eventId, title, price FROM event WHERE eventId = ? AND approved = 1";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, eventId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    double price = rs.getDouble("price");
                    String eventTitle = rs.getString("title");

                    if (price > 0) {
                        System.out.println("Event '" + eventTitle + "' costs " + price);
                        System.out.println("Payment Instructions:");
                        System.out.println("Please pay " + price + " for your registered Event.");
                        //System.out.println("Cash/Check: Submit at Event Office, 123 Main St, City.");
                        //System.out.println("Bank Transfer: Account #123456789, Bank of Events, include Event ID " + eventId + " and User ID " + userId);
                        System.out.println("Payment will be marked as PENDING until confirmed by admin.");

                        String paymentSql = "INSERT INTO payment (userId, eventId, amount, status) VALUES (?, ?, ?, 'PENDING')";
                        try (PreparedStatement paymentStmt = conn.prepareStatement(paymentSql)) {
                            paymentStmt.setInt(1, userId);
                            paymentStmt.setInt(2, eventId);
                            paymentStmt.setDouble(3, price);
                            paymentStmt.executeUpdate();
                        }

                        String insertSql = "INSERT INTO registration (eventId, userId, registrationDate, paymentStatus) VALUES (?, ?, NOW(), 'PENDING')";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, eventId);
                            insertStmt.setInt(2, userId);
                            insertStmt.executeUpdate();
                            System.out.println("Event booked successfully! Payment status: PENDING");
                        }
                    } else {
                        String insertSql = "INSERT INTO registration (eventId, userId, registrationDate, paymentStatus) VALUES (?, ?, NOW(), 'COMPLETED')";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, eventId);
                            insertStmt.setInt(2, userId);
                            insertStmt.executeUpdate();
                            System.out.println("Event booked successfully! (Free event)");
                        }
                    }
                } else {
                    System.out.println("Error: Event ID " + eventId + " does not exist or is not approved.");
                }
            }
        } else {
            System.out.println("Invalid Event ID. Please enter a number.");
            sc.nextLine();
        }
    }
}

class Admin {
    void manageUsers(Connection conn) {
        System.out.println("User management functionality to be implemented.");
    }

    void manageEvent(Connection conn) {
        System.out.println("Event management functionality to be implemented.");
    }

    void manageVenue(Connection conn, Scanner sc) throws SQLException {
        System.out.println("1. Add Venue\n2. View Venues\n3. Update Venue");
        if (sc.hasNextInt()) {
            int choice = sc.nextInt();
            sc.nextLine();
            if (choice == 1) {
                System.out.print("Enter venue name: ");
                String name = sc.nextLine();
                System.out.print("Enter location: ");
                String location = sc.nextLine();
                System.out.print("Enter capacity: ");
                if (sc.hasNextInt()) {
                    int capacity = sc.nextInt();
                    sc.nextLine();
                    String sql = "INSERT INTO venue (name, location, capacity) VALUES (?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, name);
                        stmt.setString(2, location);
                        stmt.setInt(3, capacity);
                        stmt.executeUpdate();
                        System.out.println("Venue added successfully!");
                    }
                } else {
                    System.out.println("Invalid capacity. Please enter a number.");
                    sc.nextLine();
                }
            } else if (choice == 2) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM venue")) {
                    boolean hasVenues = false;
                    while (rs.next()) {
                        hasVenues = true;
                        System.out.println("Venue ID: " + rs.getInt("venueId") + ", Name: " + rs.getString("name") +
                                           ", Location: " + rs.getString("location") + ", Capacity: " + rs.getInt("capacity"));
                    }
                    if (!hasVenues) {
                        System.out.println("No venues available.");
                    }
                }
            } else if (choice == 3) {
                System.out.print("Enter venue ID to update: ");
                if (sc.hasNextInt()) {
                    int id = sc.nextInt();
                    sc.nextLine();
                    System.out.print("Enter new name: ");
                    String name = sc.nextLine();
                    System.out.print("Enter new location: ");
                    String location = sc.nextLine();
                    System.out.print("Enter new capacity: ");
                    if (sc.hasNextInt()) {
                        int capacity = sc.nextInt();
                        sc.nextLine();
                        String sql = "UPDATE venue SET name=?, location=?, capacity=? WHERE venueId=?";
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, name);
                            stmt.setString(2, location);
                            stmt.setInt(3, capacity);
                            stmt.setInt(4, id);
                            stmt.executeUpdate();
                            System.out.println("Venue updated.");
                        }
                    } else {
                        System.out.println("Invalid capacity. Please enter a number.");
                        sc.nextLine();
                    }
                } else {
                    System.out.println("Invalid Venue ID. Please enter a number.");
                    sc.nextLine();
                }
            } else {
                System.out.println("Invalid choice.");
            }
        } else {
            System.out.println("Invalid input. Please enter a number.");
            sc.nextLine();
        }
    }

    void approveEvent(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter event ID to approve: ");
        if (sc.hasNextInt()) {
            int eventId = sc.nextInt();
            sc.nextLine();
            String sql = "UPDATE event SET approved=1 WHERE eventId=?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, eventId);
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("Event approved successfully.");
                } else {
                    System.out.println("Event approval failed. Event ID may not exist.");
                }
            }
        } else {
            System.out.println("Invalid Event ID. Please enter a number.");
            sc.nextLine();
        }
    }

    void viewPayments(Connection conn) throws SQLException {
        String sql = "SELECT p.paymentId, p.userId, u.name, p.eventId, e.title, p.amount, p.status " +
                     "FROM payment p JOIN user u ON p.userId = u.userId JOIN event e ON p.eventId = e.eventId";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            boolean hasPayments = false;
            while (rs.next()) {
                hasPayments = true;
                System.out.println("Payment ID: " + rs.getInt("paymentId") +
                                   ", User: " + rs.getString("name") +
                                   ", Event: " + rs.getString("title") +
                                   ", Amount: " + rs.getDouble("amount") +
                                   ", Status: " + rs.getString("status"));
            }
            if (!hasPayments) {
                System.out.println("No payment records available.");
            }
        }
    }

    void updatePaymentStatus(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter Payment ID to update: ");
        if (sc.hasNextInt()) {
            int paymentId = sc.nextInt();
            sc.nextLine();
            System.out.println("Select new status: \n1. PENDING\n2. COMPLETED\n3. FAILED");
            System.out.print("Enter choice (1-3): ");
            if (sc.hasNextInt()) {
                int statusChoice = sc.nextInt();
                sc.nextLine();
                String newStatus;
                switch (statusChoice) {
                    case 1: newStatus = "PENDING"; break;
                    case 2: newStatus = "COMPLETED"; break;
                    case 3: newStatus = "FAILED"; break;
                    default:
                        System.out.println("Invalid status choice.");
                        return;
                }

                String paymentSql = "UPDATE payment SET status = ? WHERE paymentId = ?";
                try (PreparedStatement paymentStmt = conn.prepareStatement(paymentSql)) {
                    paymentStmt.setString(1, newStatus);
                    paymentStmt.setInt(2, paymentId);
                    int rows = paymentStmt.executeUpdate();
                    if (rows > 0) {
                        String registrationSql = "UPDATE registration r JOIN payment p ON r.eventId = p.eventId AND r.userId = p.userId " +
                                                "SET r.paymentStatus = ? WHERE p.paymentId = ?";
                        try (PreparedStatement regStmt = conn.prepareStatement(registrationSql)) {
                            regStmt.setString(1, newStatus);
                            regStmt.setInt(2, paymentId);
                            regStmt.executeUpdate();
                        }
                        System.out.println("Payment status updated to " + newStatus + ".");
                    } else {
                        System.out.println("Payment ID " + paymentId + " not found.");
                    }
                }
            } else {
                System.out.println("Invalid input. Please enter a number (1-3).");
                sc.nextLine();
            }
        } else {
            System.out.println("Invalid Payment ID. Please enter a number.");
            sc.nextLine();
        }
    }
}

class EventOrganizer {
    int organizerId;
    String organizationName;

    void registerOrganizer(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter Organizer ID: ");
        if (sc.hasNextInt()) {
            organizerId = sc.nextInt();
            sc.nextLine();
            System.out.print("Enter Organization Name: ");
            organizationName = sc.nextLine();

            String checkSql = "SELECT organizerId FROM eventorganizer WHERE organizerId = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, organizerId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    System.out.println("Organizer ID " + organizerId + " already exists.");
                    return;
                }
            }

            String insertSql = "INSERT INTO eventorganizer (organizerId, organizationName) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, organizerId);
                insertStmt.setString(2, organizationName);
                insertStmt.executeUpdate();
                System.out.println("Organizer registered successfully!");
            }
        } else {
            System.out.println("Invalid Organizer ID. Please enter a number.");
            sc.nextLine();
        }
    }

    void createEvent(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter title: ");
        String title = sc.nextLine();
        System.out.print("Enter description: ");
        String description = sc.nextLine();
        System.out.print("Enter date (YYYY-MM-DD): ");
        String date = sc.nextLine();
        System.out.print("Enter time (HH:MM:SS): ");
        String time = sc.nextLine();
        System.out.print("Enter venue ID: ");
        if (sc.hasNextInt()) {
            int venueId = sc.nextInt();
            sc.nextLine();
            System.out.print("Enter event price (0 for free): ");
            if (sc.hasNextDouble()) {
                double price = sc.nextDouble();
                sc.nextLine();

                String checkVenueSql = "SELECT venueId FROM venue WHERE venueId = ?";
                try (PreparedStatement checkVenueStmt = conn.prepareStatement(checkVenueSql)) {
                    checkVenueStmt.setInt(1, venueId);
                    ResultSet rsVenue = checkVenueStmt.executeQuery();
                    if (!rsVenue.next()) {
                        System.out.println("Error: Venue ID " + venueId + " does not exist.");
                        return;
                    }
                }

                String checkOrganizerSql = "SELECT organizerId FROM eventorganizer WHERE organizerId = ?";
                try (PreparedStatement checkOrganizerStmt = conn.prepareStatement(checkOrganizerSql)) {
                    checkOrganizerStmt.setInt(1, organizerId);
                    ResultSet rsOrganizer = checkOrganizerStmt.executeQuery();
                    if (rsOrganizer.next()) {
                        String insertSql = "INSERT INTO event (title, description, date, time, venueId, organizerId, approved, price) VALUES (?, ?, ?, ?, ?, ?, 0, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, title);
                            insertStmt.setString(2, description);
                            insertStmt.setString(3, date);
                            insertStmt.setString(4, time);
                            insertStmt.setInt(5, venueId);
                            insertStmt.setInt(6, organizerId);
                            insertStmt.setDouble(7, price);
                            insertStmt.executeUpdate();
                            System.out.println("Event created successfully! Awaiting admin approval.");
                        }
                    } else {
                        System.out.println("Error: Organizer ID " + organizerId + " does not exist. Please register the organizer first.");
                    }
                }
            } else {
                System.out.println("Invalid price. Please enter a number .");
                sc.nextLine();
            }
        } else {
            System.out.println("Invalid Venue ID. Please enter a number.");
            sc.nextLine();
        }
    }

    void viewRegistrations(Connection conn, int eventId) throws SQLException {
        String sql = "SELECT r.registrationId, r.userId, u.name, r.registrationDate, r.paymentStatus " +
                     "FROM registration r JOIN user u ON r.userId = u.userId WHERE r.eventId = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();
            boolean hasRegistrations = false;
            while (rs.next()) {
                hasRegistrations = true;
                System.out.println("Registration ID: " + rs.getInt("registrationId") +
                                   ", User: " + rs.getString("name") +
                                   ", Registered on: " + rs.getTimestamp("registrationDate") +
                                   ", Payment Status: " + rs.getString("paymentStatus"));
            }
            if (!hasRegistrations) {
                System.out.println("No registrations found for Event ID " + eventId);
            }
        }
    }
}

public class EventRegistrationApp {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.connect();
             Scanner sc = new Scanner(System.in)) {
            User user = new User();
            Admin admin = new Admin();
            EventOrganizer organizer = new EventOrganizer();

            while (true) {
            	System.out.println("------------ EVENT REGISTRATION SYSTEM ------------");
                System.out.println("1. Register\n2. Login\n3. Admin\n4. Event Organizer\n5. Exit");
                System.out.print("Enter your choice: ");
                if (sc.hasNextInt()) {
                    int choice = sc.nextInt();
                    sc.nextLine();
                    switch (choice) {
                        case 1:
                            user.register(conn, sc);
                            break;
                        case 2:
                            System.out.print("Enter email: ");
                            String email = sc.nextLine();
                            System.out.print("Enter password: ");
                            String password = sc.nextLine();
                            if (user.login(conn, email, password)) {
                                while (true) {
                                    System.out.println("1. View Events\n2. Book Event\n3. Logout");
                                    System.out.print("Enter your choice: ");
                                    if (sc.hasNextInt()) {
                                        int option = sc.nextInt();
                                        sc.nextLine();
                                        if (option == 1) {
                                            user.viewEvent(conn);
                                        } else if (option == 2) {
                                            user.bookEvent(conn, sc);
                                        } else if (option == 3) {
                                            break;
                                        } else {
                                            System.out.println("Invalid option.");
                                        }
                                    } else {
                                        System.out.println("Invalid input. Please enter a number.");
                                        sc.nextLine();
                                    }
                                }
                            }
                            break;
                        case 3:
                            System.out.println("1. Approve Event\n2. Manage Venue\n3. View Payments\n4. Update Payment Status");
                            System.out.print("Enter your choice: ");
                            if (sc.hasNextInt()) {
                                int adminOp = sc.nextInt();
                                sc.nextLine();
                                if (adminOp == 1) {
                                    admin.approveEvent(conn, sc);
                                } else if (adminOp == 2) {
                                    admin.manageVenue(conn, sc);
                                } else if (adminOp == 3) {
                                    admin.viewPayments(conn);
                                } else if (adminOp == 4) {
                                    admin.updatePaymentStatus(conn, sc);
                                } else {
                                    System.out.println("Invalid option.");
                                }
                            } else {
                                System.out.println("Invalid input. Please enter a number.");
                                sc.nextLine();
                            }
                            break;
                        case 4:
                            while (true) {
                                System.out.println("1. Register Organizer\n2. Create Event\n3. View Event Registrations\n4. Back");
                                System.out.print("Enter your choice: ");
                                if (sc.hasNextInt()) {
                                    int op = sc.nextInt();
                                    sc.nextLine();
                                    if (op == 1) {
                                        organizer.registerOrganizer(conn, sc);
                                    } else if (op == 2) {
                                        System.out.print("Enter Organizer ID: ");
                                        if (sc.hasNextInt()) {
                                            organizer.organizerId = sc.nextInt();
                                            sc.nextLine();
                                            organizer.createEvent(conn, sc);
                                        } else {
                                            System.out.println("Invalid Organizer ID. Please enter a number.");
                                            sc.nextLine();
                                        }
                                    } else if (op == 3) {
                                        System.out.print("Enter Event ID: ");
                                        if (sc.hasNextInt()) {
                                            int eId = sc.nextInt();
                                            sc.nextLine();
                                            organizer.viewRegistrations(conn, eId);
                                        } else {
                                            System.out.println("Invalid Event ID. Please enter a number.");
                                            sc.nextLine();
                                        }
                                    } else if (op == 4) {
                                        break;
                                    } else {
                                        System.out.println("Invalid option.");
                                    }
                                } else {
                                    System.out.println("Invalid input. Please enter a number.");
                                    sc.nextLine();
                                }
                            }
                            break;
                        case 5:
                            System.out.println("Exiting...");
                            return;
                        default:
                            System.out.println("Invalid choice. Please try again.");
                    }
                } else {
                    System.out.println("Invalid input. Please enter a number.");
                    sc.nextLine();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}