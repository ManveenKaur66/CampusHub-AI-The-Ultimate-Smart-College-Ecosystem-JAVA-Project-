import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RegistrationDAO {
    public boolean register(int eventId, String email) {
        String sql = "INSERT INTO registrations(event_id, user_email) VALUES(?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setString(2, email);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                "Registration insert failed: " + exception.getMessage(), exception
            );
        }
    }

    public boolean unregister(int eventId, String email) {
        String sql = "DELETE FROM registrations WHERE event_id = ? AND user_email = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setString(2, email);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public List<String> findByUser(String email) {
        List<String> eventNames = new ArrayList<>();
        String sql = "SELECT e.event_name FROM registrations r "
            + "JOIN events e ON e.event_id = r.event_id "
            + "WHERE r.user_email = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    eventNames.add(result.getString(1));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
        return eventNames;
    }
}
