import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

public final class DBConnection {
    private static final String DEFAULT_HOST_URL =
        "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_DB_URL =
        "jdbc:mysql://localhost:3306/campushub?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean initialized = false;

    private DBConnection() {
    }

    private static String ensureParams(String url) {
        if (!url.contains("allowPublicKeyRetrieval=")) {
            url += (url.contains("?") ? "&" : "?") + "allowPublicKeyRetrieval=true";
        }
        if (!url.contains("useSSL=")) {
            url += (url.contains("?") ? "&" : "?") + "useSSL=false";
        }
        if (!url.contains("serverTimezone=")) {
            url += (url.contains("?") ? "&" : "?") + "serverTimezone=UTC";
        }
        return url;
    }

    private static void promptCredentials(Scanner fallbackScanner) {
        Console console = System.console();
        if (console != null) {
            String u = console.readLine("Enter MySQL username [root]: ");
            dbUser = (u == null || u.trim().isEmpty()) ? "root" : u.trim();
            char[] pwd = console.readPassword("Enter MySQL password: ");
            dbPassword = (pwd == null) ? "" : new String(pwd);
        } else {
            System.out.print("Enter MySQL username [root]: ");
            String u = fallbackScanner.nextLine();
            dbUser = (u == null || u.trim().isEmpty()) ? "root" : u.trim();
            System.out.print("Enter MySQL password: ");
            String p = fallbackScanner.nextLine();
            dbPassword = (p == null) ? "" : p;
        }
    }

    private static synchronized void initAndBootstrap() throws SQLException {
        if (initialized) {
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found in lib/mysql-connector-j.jar", e);
        }

        String envUrl = System.getenv("CAMPUSHUB_DB_URL");
        boolean customUrl = (envUrl != null && !envUrl.isBlank());
        dbUrl = customUrl ? ensureParams(envUrl.trim()) : DEFAULT_DB_URL;

        String envUser = System.getenv("CAMPUSHUB_DB_USER");
        String envPass = System.getenv("CAMPUSHUB_DB_PASSWORD");

        Scanner sc = new Scanner(System.in);
        if (envUser != null && !envUser.isBlank()) {
            dbUser = envUser.trim();
            dbPassword = (envPass != null) ? envPass : "";
        } else {
            promptCredentials(sc);
        }

        int attempts = 0;
        while (true) {
            attempts++;
            try {
                System.out.println("Connecting to MySQL...");
                if (!customUrl) {
                    try (Connection serverConn = DriverManager.getConnection(DEFAULT_HOST_URL, dbUser, dbPassword);
                         Statement stmt = serverConn.createStatement()) {
                        bootstrapSchema(stmt);
                    }
                } else {
                    try (Connection testConn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                         Statement stmt = testConn.createStatement()) {
                        bootstrapSchema(stmt);
                    }
                }
                initialized = true;
                return;
            } catch (SQLException e) {
                boolean authError = "28000".equals(e.getSQLState()) || e.getErrorCode() == 1045;
                if (authError && attempts < 3) {
                    System.out.println("[MySQL Error] Access denied for user '" + dbUser + "'. Please re-enter credentials.");
                    promptCredentials(sc);
                } else {
                    throw e;
                }
            }
        }
    }

    private static Set<String> getColumns(Statement stmt, String tableName) throws SQLException {
        Set<String> cols = new HashSet<>();
        try (ResultSet rs = stmt.executeQuery(
            "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + tableName + "'"
        )) {
            while (rs.next()) {
                cols.add(rs.getString(1).toLowerCase(Locale.ROOT));
            }
        }
        return cols;
    }

    private static void bootstrapSchema(Statement stmt) throws SQLException {
        stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS campushub");
        stmt.executeUpdate("USE campushub");
        stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");

        try {
            // 1. users table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users ("
                + "email VARCHAR(150) PRIMARY KEY, "
                + "full_name VARCHAR(100) NOT NULL, "
                + "password_hash CHAR(64) NOT NULL, "
                + "role ENUM('STUDENT', 'FACULTY') NOT NULL)"
            );
            Set<String> userCols = getColumns(stmt, "users");
            if (!userCols.contains("full_name")) {
                if (userCols.contains("name")) {
                    stmt.executeUpdate("ALTER TABLE users CHANGE COLUMN name full_name VARCHAR(100) NOT NULL");
                } else {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN full_name VARCHAR(100) NOT NULL DEFAULT '' AFTER email");
                }
            } else if (userCols.contains("name")) {
                stmt.executeUpdate("UPDATE users SET full_name = name WHERE full_name IS NULL OR full_name = ''");
                stmt.executeUpdate("ALTER TABLE users DROP COLUMN name");
            }
            if (!userCols.contains("password_hash")) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN password_hash CHAR(64) NOT NULL DEFAULT ''");
            }
            if (!userCols.contains("role")) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN role ENUM('STUDENT', 'FACULTY') NOT NULL DEFAULT 'STUDENT'");
            }

            // 2. students table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS students ("
                + "roll_number VARCHAR(20) PRIMARY KEY, "
                + "name VARCHAR(100) NOT NULL, "
                + "account_email VARCHAR(150) NULL UNIQUE, "
                + "CONSTRAINT fk_student_account FOREIGN KEY (account_email) REFERENCES users(email) "
                + "ON UPDATE CASCADE ON DELETE SET NULL)"
            );
            Set<String> studentCols = getColumns(stmt, "students");
            if (!studentCols.contains("account_email")) {
                if (studentCols.contains("email")) {
                    stmt.executeUpdate("ALTER TABLE students CHANGE COLUMN email account_email VARCHAR(150) NULL UNIQUE");
                } else {
                    stmt.executeUpdate("ALTER TABLE students ADD COLUMN account_email VARCHAR(150) NULL UNIQUE");
                }
            }

            // 3. profiles table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS profiles ("
                + "profile_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "name VARCHAR(100) NOT NULL, "
                + "email VARCHAR(150) NOT NULL UNIQUE, "
                + "department VARCHAR(100) NOT NULL, "
                + "role ENUM('STUDENT', 'FACULTY') NOT NULL, "
                + "CONSTRAINT fk_profile_user FOREIGN KEY (email) REFERENCES users(email) "
                + "ON UPDATE CASCADE ON DELETE CASCADE)"
            );
            Set<String> profileCols = getColumns(stmt, "profiles");
            if (!profileCols.contains("profile_id")) {
                if (profileCols.contains("id")) {
                    stmt.executeUpdate("ALTER TABLE profiles CHANGE COLUMN id profile_id INT AUTO_INCREMENT");
                } else {
                    stmt.executeUpdate("ALTER TABLE profiles ADD COLUMN profile_id INT AUTO_INCREMENT PRIMARY KEY FIRST");
                }
            }
            if (!profileCols.contains("department")) {
                stmt.executeUpdate("ALTER TABLE profiles ADD COLUMN department VARCHAR(100) NOT NULL DEFAULT 'Not specified'");
            }
            if (!profileCols.contains("role")) {
                stmt.executeUpdate("ALTER TABLE profiles ADD COLUMN role ENUM('STUDENT', 'FACULTY') NOT NULL DEFAULT 'STUDENT'");
            }

            // 4. assignments table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS assignments ("
                + "assignment_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "owner_email VARCHAR(150) NOT NULL, "
                + "created_by VARCHAR(150), "
                + "title VARCHAR(150) NOT NULL, "
                + "description TEXT, "
                + "due_date DATE NOT NULL, "
                + "status ENUM('PENDING', 'SUBMITTED') DEFAULT 'PENDING', "
                + "CONSTRAINT fk_assignment_owner FOREIGN KEY (owner_email) REFERENCES users(email) "
                + "ON UPDATE CASCADE ON DELETE CASCADE, "
                + "CONSTRAINT fk_assignment_creator FOREIGN KEY (created_by) REFERENCES users(email) "
                + "ON UPDATE CASCADE ON DELETE SET NULL)"
            );
            Set<String> assignCols = getColumns(stmt, "assignments");
            if (!assignCols.contains("assignment_id")) {
                if (assignCols.contains("id")) {
                    stmt.executeUpdate("ALTER TABLE assignments CHANGE COLUMN id assignment_id INT AUTO_INCREMENT");
                } else {
                    stmt.executeUpdate("ALTER TABLE assignments ADD COLUMN assignment_id INT AUTO_INCREMENT PRIMARY KEY FIRST");
                }
            }
            if (!assignCols.contains("owner_email")) {
                if (assignCols.contains("student_email")) {
                    stmt.executeUpdate("ALTER TABLE assignments CHANGE COLUMN student_email owner_email VARCHAR(150) NOT NULL");
                } else if (assignCols.contains("email")) {
                    stmt.executeUpdate("ALTER TABLE assignments CHANGE COLUMN email owner_email VARCHAR(150) NOT NULL");
                } else {
                    stmt.executeUpdate("ALTER TABLE assignments ADD COLUMN owner_email VARCHAR(150) NOT NULL");
                }
            }
            if (!assignCols.contains("created_by")) {
                stmt.executeUpdate("ALTER TABLE assignments ADD COLUMN created_by VARCHAR(150) NULL AFTER owner_email");
            }
            if (!assignCols.contains("title")) {
                stmt.executeUpdate("ALTER TABLE assignments ADD COLUMN title VARCHAR(150) NOT NULL DEFAULT ''");
            }
            if (!assignCols.contains("description")) {
                stmt.executeUpdate("ALTER TABLE assignments ADD COLUMN description TEXT NULL AFTER title");
            }
            if (!assignCols.contains("due_date")) {
                if (assignCols.contains("deadline")) {
                    stmt.executeUpdate("ALTER TABLE assignments CHANGE COLUMN deadline due_date DATE NOT NULL");
                } else {
                    stmt.executeUpdate("ALTER TABLE assignments ADD COLUMN due_date DATE NOT NULL");
                }
            }
            if (!assignCols.contains("status")) {
                stmt.executeUpdate("ALTER TABLE assignments ADD COLUMN status ENUM('PENDING', 'SUBMITTED') DEFAULT 'PENDING'");
                if (assignCols.contains("submitted")) {
                    stmt.executeUpdate("UPDATE assignments SET status = IF(submitted, 'SUBMITTED', 'PENDING')");
                }
            }

            // 5. events table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS events ("
                + "event_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "event_name VARCHAR(150) NOT NULL, "
                + "category VARCHAR(60) NOT NULL, "
                + "venue VARCHAR(150) NOT NULL, "
                + "event_date DATE NOT NULL)"
            );
            Set<String> eventCols = getColumns(stmt, "events");
            if (!eventCols.contains("event_id")) {
                if (eventCols.contains("id")) {
                    stmt.executeUpdate("ALTER TABLE events CHANGE COLUMN id event_id INT AUTO_INCREMENT");
                } else {
                    stmt.executeUpdate("ALTER TABLE events ADD COLUMN event_id INT AUTO_INCREMENT PRIMARY KEY FIRST");
                }
            }
            if (!eventCols.contains("event_name")) {
                if (eventCols.contains("name")) {
                    stmt.executeUpdate("ALTER TABLE events CHANGE COLUMN name event_name VARCHAR(150) NOT NULL");
                } else if (eventCols.contains("title")) {
                    stmt.executeUpdate("ALTER TABLE events CHANGE COLUMN title event_name VARCHAR(150) NOT NULL");
                } else {
                    stmt.executeUpdate("ALTER TABLE events ADD COLUMN event_name VARCHAR(150) NOT NULL DEFAULT ''");
                }
            }
            if (!eventCols.contains("category")) {
                stmt.executeUpdate("ALTER TABLE events ADD COLUMN category VARCHAR(60) NOT NULL DEFAULT 'General'");
            }
            if (!eventCols.contains("venue")) {
                if (eventCols.contains("location")) {
                    stmt.executeUpdate("ALTER TABLE events CHANGE COLUMN location venue VARCHAR(150) NOT NULL");
                } else {
                    stmt.executeUpdate("ALTER TABLE events ADD COLUMN venue VARCHAR(150) NOT NULL DEFAULT ''");
                }
            }
            if (!eventCols.contains("event_date")) {
                if (eventCols.contains("date")) {
                    stmt.executeUpdate("ALTER TABLE events CHANGE COLUMN date event_date DATE NOT NULL");
                } else {
                    stmt.executeUpdate("ALTER TABLE events ADD COLUMN event_date DATE NOT NULL");
                }
            }

            // 6. registrations table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS registrations ("
                + "registration_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "event_id INT NOT NULL, "
                + "user_email VARCHAR(150) NOT NULL, "
                + "UNIQUE KEY uq_event_user (event_id, user_email), "
                + "CONSTRAINT fk_registration_event FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE, "
                + "CONSTRAINT fk_registration_user FOREIGN KEY (user_email) REFERENCES users(email) "
                + "ON UPDATE CASCADE ON DELETE CASCADE)"
            );
            Set<String> regCols = getColumns(stmt, "registrations");
            if (!regCols.contains("registration_id")) {
                if (regCols.contains("id")) {
                    stmt.executeUpdate("ALTER TABLE registrations CHANGE COLUMN id registration_id INT AUTO_INCREMENT");
                } else {
                    stmt.executeUpdate("ALTER TABLE registrations ADD COLUMN registration_id INT AUTO_INCREMENT PRIMARY KEY FIRST");
                }
            }
            if (!regCols.contains("user_email")) {
                if (regCols.contains("email")) {
                    stmt.executeUpdate("ALTER TABLE registrations CHANGE COLUMN email user_email VARCHAR(150) NOT NULL");
                } else if (regCols.contains("student_email")) {
                    stmt.executeUpdate("ALTER TABLE registrations CHANGE COLUMN student_email user_email VARCHAR(150) NOT NULL");
                } else {
                    stmt.executeUpdate("ALTER TABLE registrations ADD COLUMN user_email VARCHAR(150) NOT NULL");
                }
            }

            // 7. notifications table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS notifications ("
                + "notification_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "recipient_email VARCHAR(150) NOT NULL, "
                + "message TEXT NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "is_read BOOLEAN DEFAULT FALSE, "
                + "CONSTRAINT fk_notification_user FOREIGN KEY (recipient_email) REFERENCES users(email) "
                + "ON UPDATE CASCADE ON DELETE CASCADE)"
            );
            Set<String> notifCols = getColumns(stmt, "notifications");
            if (!notifCols.contains("notification_id")) {
                if (notifCols.contains("id")) {
                    stmt.executeUpdate("ALTER TABLE notifications CHANGE COLUMN id notification_id INT AUTO_INCREMENT");
                } else {
                    stmt.executeUpdate("ALTER TABLE notifications ADD COLUMN notification_id INT AUTO_INCREMENT PRIMARY KEY FIRST");
                }
            }
            if (!notifCols.contains("recipient_email")) {
                if (notifCols.contains("user_email")) {
                    stmt.executeUpdate("ALTER TABLE notifications CHANGE COLUMN user_email recipient_email VARCHAR(150) NOT NULL");
                } else if (notifCols.contains("email")) {
                    stmt.executeUpdate("ALTER TABLE notifications CHANGE COLUMN email recipient_email VARCHAR(150) NOT NULL");
                } else {
                    stmt.executeUpdate("ALTER TABLE notifications ADD COLUMN recipient_email VARCHAR(150) NOT NULL");
                }
            }
            if (!notifCols.contains("created_at")) {
                stmt.executeUpdate("ALTER TABLE notifications ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            }
            if (!notifCols.contains("is_read")) {
                if (notifCols.contains("read_status")) {
                    stmt.executeUpdate("ALTER TABLE notifications CHANGE COLUMN read_status is_read BOOLEAN DEFAULT FALSE");
                } else if (notifCols.contains("isread")) {
                    stmt.executeUpdate("ALTER TABLE notifications CHANGE COLUMN isread is_read BOOLEAN DEFAULT FALSE");
                } else {
                    stmt.executeUpdate("ALTER TABLE notifications ADD COLUMN is_read BOOLEAN DEFAULT FALSE");
                }
            }
        } finally {
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    public static Connection getConnection() throws SQLException {
        initAndBootstrap();
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}
