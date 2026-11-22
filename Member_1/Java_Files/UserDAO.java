import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class UserDAO implements GenericRepository<User, String> {
    @Override
    public void save(User user) {
        String sql = "INSERT INTO users(email, full_name, password_hash, role) "
            + "VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
            + "full_name=VALUES(full_name), password_hash=VALUES(password_hash), "
            + "role=VALUES(role)";
        String cleanEmail = user.getEmail().trim().toLowerCase(Locale.ROOT);
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cleanEmail);
            statement.setString(2, user.getName());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getRole());
            statement.executeUpdate();

            if (user instanceof Student student
                && student.getStudentId() != null
                && !student.getStudentId().isBlank()) {
                try (PreparedStatement linkStmt = connection.prepareStatement(
                    "UPDATE students SET account_email = ? WHERE roll_number = ?"
                )) {
                    linkStmt.setString(1, cleanEmail);
                    linkStmt.setString(2, student.getStudentId().trim());
                    linkStmt.executeUpdate();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                "MySQL save failed: " + exception.getMessage(), exception
            );
        }
    }

    @Override
    public Optional<User> findById(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT u.email, u.full_name, u.password_hash, u.role, s.roll_number "
            + "FROM users u LEFT JOIN students s ON s.account_email = u.email "
            + "WHERE LOWER(u.email) = LOWER(?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapUser(result));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.email, u.full_name, u.password_hash, u.role, s.roll_number "
            + "FROM users u LEFT JOIN students s ON s.account_email = u.email";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                users.add(mapUser(result));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
        return users;
    }

    @Override
    public boolean deleteById(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String sql = "DELETE FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private User mapUser(ResultSet result) throws SQLException {
        String name = result.getString("full_name");
        String email = result.getString("email");
        String hash = result.getString("password_hash");
        String role = result.getString("role");
        if ("STUDENT".equalsIgnoreCase(role)) {
            String roll = result.getString("roll_number");
            return new Student(name, email, hash, roll == null ? "" : roll, "Not specified");
        }
        return new Faculty(name, email, hash, "Not specified");
    }
}
