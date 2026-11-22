import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationDAO {
    public void save(String email, String message) {
        if (email == null || email.isBlank() || message == null || message.isBlank()) {
            return;
        }
        String sql = "INSERT INTO notifications(recipient_email, message) VALUES(?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim().toLowerCase(Locale.ROOT));
            statement.setString(2, message);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public List<Notice> findByRecipient(String email) {
        List<Notice> list = new ArrayList<>();
        if (email == null || email.isBlank()) {
            return list;
        }
        String sql = "SELECT recipient_email, message, is_read, created_at FROM notifications "
            + "WHERE LOWER(recipient_email) = LOWER(?) ORDER BY is_read ASC, created_at DESC";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Timestamp ts = result.getTimestamp("created_at");
                    LocalDateTime time = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();
                    list.add(new Notice(
                        result.getString("recipient_email"),
                        result.getString("message"),
                        result.getBoolean("is_read"),
                        time
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
        return list;
    }

    public long unreadCount(String email) {
        if (email == null || email.isBlank()) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM notifications "
            + "WHERE LOWER(recipient_email) = LOWER(?) AND is_read = FALSE";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public void markAllRead(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        String sql = "UPDATE notifications SET is_read = TRUE "
            + "WHERE LOWER(recipient_email) = LOWER(?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
