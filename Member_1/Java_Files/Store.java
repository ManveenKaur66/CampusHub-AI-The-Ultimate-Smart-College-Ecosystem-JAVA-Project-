import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** MySQL-backed application store. MySQL is the only persistent storage. */
public class Store {
    public List<User> users = new ArrayList<>();
    public List<Profile> profiles = new ArrayList<>();
    public List<Assignment> assignments = new ArrayList<>();
    public List<Event> events = new ArrayList<>();
    public List<Notice> notices = new ArrayList<>();
    public List<Registration> registrations = new ArrayList<>();
    public Map<String, String> studentRoster = new LinkedHashMap<>();
    public int nextAssignmentId = 1;
    public int nextEventId = 1;

    public static Store load() {
        Store x = new Store();
        try (Connection c = DBConnection.getConnection()) {
            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery("SELECT roll_number, name FROM students")) {
                while (r.next()) {
                    x.studentRoster.put(r.getString(1), r.getString(2));
                }
            }

            if (x.studentRoster.isEmpty()) {
                x.studentRoster.putAll(StudentRoster.records());
                try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO students(roll_number, name) VALUES(?, ?) "
                    + "ON DUPLICATE KEY UPDATE name = VALUES(name)"
                )) {
                    for (Map.Entry<String, String> entry : x.studentRoster.entrySet()) {
                        p.setString(1, entry.getKey());
                        p.setString(2, entry.getValue());
                        p.addBatch();
                    }
                    p.executeBatch();
                }
            }

            Map<String, String> deptByEmail = new HashMap<>();
            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery("SELECT name, email, department, role FROM profiles")) {
                while (r.next()) {
                    Profile profile = new Profile(r.getString(1), r.getString(2), r.getString(3), r.getString(4));
                    x.profiles.add(profile);
                    if (profile.getEmail() != null) {
                        deptByEmail.put(profile.getEmail().toLowerCase(Locale.ROOT), profile.getDepartment());
                    }
                }
            }

            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery("SELECT email, full_name, password_hash, role FROM users")) {
                while (r.next()) {
                    String role = r.getString(4);
                    String fullName = r.getString(2);
                    String email = r.getString(1);
                    String hash = r.getString(3);
                    String dept = deptByEmail.getOrDefault(
                        email == null ? "" : email.toLowerCase(Locale.ROOT),
                        "Not specified"
                    );
                    if ("STUDENT".equals(role)) {
                        String roll = null;
                        try (PreparedStatement q = c.prepareStatement(
                            "SELECT roll_number FROM students WHERE account_email=?"
                        )) {
                            q.setString(1, email);
                            try (ResultSet rr = q.executeQuery()) {
                                if (rr.next()) {
                                    roll = rr.getString(1);
                                }
                            }
                        }
                        x.users.add(new Student(fullName, email, hash, roll == null ? "" : roll, dept));
                    } else {
                        x.users.add(new Faculty(fullName, email, hash, dept));
                    }
                }
            }

            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery(
                     "SELECT assignment_id, owner_email, title, description, due_date, created_by, status FROM assignments"
                 )) {
                while (r.next()) {
                    java.sql.Date sqlDueDate = r.getDate(5);
                    LocalDate dueDate = (sqlDueDate != null) ? sqlDueDate.toLocalDate() : LocalDate.now();
                    Assignment a = new Assignment(
                        r.getInt(1),
                        r.getString(2),
                        r.getString(3),
                        r.getString(4),
                        dueDate,
                        r.getString(6)
                    );
                    a.setSubmitted("SUBMITTED".equals(r.getString(7)));
                    x.assignments.add(a);
                    x.nextAssignmentId = Math.max(x.nextAssignmentId, a.getId() + 1);
                }
            }

            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery(
                     "SELECT event_id, event_name, category, venue, event_date FROM events"
                 )) {
                while (r.next()) {
                    java.sql.Date sqlEventDate = r.getDate(5);
                    LocalDate eventDate = (sqlEventDate != null) ? sqlEventDate.toLocalDate() : LocalDate.now();
                    Event e = new Event(
                        r.getInt(1),
                        r.getString(2),
                        r.getString(3),
                        r.getString(4),
                        eventDate
                    );
                    x.events.add(e);
                    x.nextEventId = Math.max(x.nextEventId, e.getId() + 1);
                }
            }

            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery("SELECT event_id, user_email FROM registrations")) {
                while (r.next()) {
                    x.registrations.add(new Registration(r.getInt(1), r.getString(2)));
                }
            }

            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery(
                     "SELECT recipient_email, message, is_read, created_at FROM notifications"
                 )) {
                while (r.next()) {
                    Timestamp ts = r.getTimestamp(4);
                    LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();
                    Notice n = new Notice(r.getString(1), r.getString(2), r.getBoolean(3), createdAt);
                    x.notices.add(n);
                }
            }

            return x;
        } catch (SQLException e) {
            throw new IllegalStateException(
                "MySQL connection/load failed. Check MySQL service and credentials: " + e.getMessage(), e
            );
        }
    }

    /** Writes the current in-memory working set atomically to MySQL. */
    public synchronized void save() {
        Set<String> validUserEmails = new HashSet<>();
        for (User u : users) {
            if (u.getEmail() != null) {
                validUserEmails.add(u.getEmail().toLowerCase(Locale.ROOT));
            }
        }

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                run(c, "DELETE FROM registrations");
                run(c, "DELETE FROM notifications");
                run(c, "DELETE FROM assignments");
                run(c, "DELETE FROM events");
                run(c, "DELETE FROM profiles");
                run(c, "UPDATE students SET account_email=NULL");
                run(c, "DELETE FROM users");

                try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO users(email, full_name, password_hash, role) VALUES(?, ?, ?, ?)"
                )) {
                    for (User u : users) {
                        p.setString(1, u.getEmail());
                        p.setString(2, u.getName());
                        p.setString(3, u.getPasswordHash());
                        p.setString(4, u.getRole());
                        p.addBatch();
                    }
                    p.executeBatch();
                }

                try (PreparedStatement p = c.prepareStatement(
                    "UPDATE students SET account_email=? WHERE roll_number=?"
                )) {
                    for (User u : users) {
                        if (u instanceof Student st && st.getStudentId() != null && !st.getStudentId().isBlank()) {
                            p.setString(1, u.getEmail());
                            p.setString(2, st.getStudentId());
                            p.addBatch();
                        }
                    }
                    p.executeBatch();
                }

                try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO profiles(name, email, department, role) VALUES(?, ?, ?, ?)"
                )) {
                    for (Profile a : profiles) {
                        if (a.getEmail() != null && validUserEmails.contains(a.getEmail().toLowerCase(Locale.ROOT))) {
                            p.setString(1, a.getName());
                            p.setString(2, a.getEmail());
                            p.setString(3, a.getDepartment());
                            p.setString(4, a.getRole());
                            p.addBatch();
                        }
                    }
                    p.executeBatch();
                }

                try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO assignments(assignment_id, owner_email, created_by, title, description, due_date, status) "
                    + "VALUES(?, ?, ?, ?, ?, ?, ?)"
                )) {
                    for (Assignment a : assignments) {
                        if (a.getOwner() == null || !validUserEmails.contains(a.getOwner().toLowerCase(Locale.ROOT))) {
                            continue;
                        }
                        p.setInt(1, a.getId());
                        p.setString(2, a.getOwner());
                        if (a.getCreatedBy() != null
                            && !a.getCreatedBy().isBlank()
                            && validUserEmails.contains(a.getCreatedBy().toLowerCase(Locale.ROOT))) {
                            p.setString(3, a.getCreatedBy());
                        } else {
                            p.setNull(3, Types.VARCHAR);
                        }
                        p.setString(4, a.getTitle());
                        p.setString(5, a.getDescription() == null ? "" : a.getDescription());
                        p.setDate(6, java.sql.Date.valueOf(a.getDueDate()));
                        p.setString(7, a.isSubmitted() ? "SUBMITTED" : "PENDING");
                        p.addBatch();
                    }
                    p.executeBatch();
                }

                try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO events(event_id, event_name, category, venue, event_date) VALUES(?, ?, ?, ?, ?)"
                )) {
                    for (Event a : events) {
                        p.setInt(1, a.getId());
                        p.setString(2, a.getName());
                        p.setString(3, a.getCategory());
                        p.setString(4, a.getVenue());
                        p.setDate(5, java.sql.Date.valueOf(a.getDate()));
                        p.addBatch();
                    }
                    p.executeBatch();
                }

                Set<Integer> validEventIds = new HashSet<>();
                for (Event ev : events) {
                    validEventIds.add(ev.getId());
                }
                try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO registrations(event_id, user_email) VALUES(?, ?)"
                )) {
                    for (Registration a : registrations) {
                        if (a.getEmail() != null
                            && validUserEmails.contains(a.getEmail().toLowerCase(Locale.ROOT))
                            && validEventIds.contains(a.getEventId())) {
                            p.setInt(1, a.getEventId());
                            p.setString(2, a.getEmail());
                            p.addBatch();
                        }
                    }
                    p.executeBatch();
                }

                try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO notifications(recipient_email, message, created_at, is_read) VALUES(?, ?, ?, ?)"
                )) {
                    for (Notice a : notices) {
                        if (a.getRecipient() != null
                            && validUserEmails.contains(a.getRecipient().toLowerCase(Locale.ROOT))) {
                            p.setString(1, a.getRecipient());
                            p.setString(2, a.getMessage());
                            p.setTimestamp(3, Timestamp.valueOf(a.getTime()));
                            p.setBoolean(4, a.isRead());
                            p.addBatch();
                        }
                    }
                    p.executeBatch();
                }

                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("MySQL save failed: " + e.getMessage(), e);
        }
    }

    private static void run(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        }
    }

    public synchronized void notifyUser(String email, String message) {
        if (email == null || email.isBlank() || message == null || message.isBlank()) {
            return;
        }
        boolean userExists = users.stream().anyMatch(
            u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email.trim())
        );
        if (!userExists) {
            return;
        }
        Notice notice = new Notice(email.trim().toLowerCase(Locale.ROOT), message);
        notices.add(notice);
        try {
            save();
        } catch (RuntimeException exception) {
            notices.remove(notice);
            throw exception;
        }
    }
}
