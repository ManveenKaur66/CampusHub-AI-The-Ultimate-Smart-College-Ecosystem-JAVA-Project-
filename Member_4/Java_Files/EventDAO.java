import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventDAO implements GenericRepository<Event, Integer> {
    @Override
    public void save(Event event) {
        String sql = "INSERT INTO events "
            + "(event_id, event_name, category, venue, event_date) "
            + "VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
            + "event_name=VALUES(event_name), category=VALUES(category), "
            + "venue=VALUES(venue), event_date=VALUES(event_date)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, event.getId());
            statement.setString(2, event.getName());
            statement.setString(3, event.getCategory());
            statement.setString(4, event.getVenue());
            statement.setDate(5, java.sql.Date.valueOf(event.getDate()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public Optional<Event> findById(Integer id) {
        String sql = "SELECT * FROM events WHERE event_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapEvent(result));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
        return Optional.empty();
    }

    @Override
    public List<Event> findAll() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                events.add(mapEvent(result));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
        return events;
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM events WHERE event_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Event mapEvent(ResultSet result) throws SQLException {
        return new Event(
            result.getInt("event_id"),
            result.getString("event_name"),
            result.getString("category"),
            result.getString("venue"),
            result.getDate("event_date").toLocalDate()
        );
    }
}
