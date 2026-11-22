import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DirectoryDAO implements GenericRepository<Profile, String> {
    @Override
    public void save(Profile profile) {
        String sql = "INSERT INTO profiles(name, email, department, role) "
            + "VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
            + "name=VALUES(name), department=VALUES(department), role=VALUES(role)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, profile.getName());
            statement.setString(2, profile.getEmail());
            statement.setString(3, profile.getDepartment());
            statement.setString(4, profile.getRole());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public Optional<Profile> findById(String email) {
        String sql = "SELECT * FROM profiles WHERE email = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(new Profile(
                        result.getString("name"),
                        result.getString("email"),
                        result.getString("department"),
                        result.getString("role")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
        return Optional.empty();
    }

    @Override
    public List<Profile> findAll() {
        List<Profile> profiles = new ArrayList<>();
        String sql = "SELECT * FROM profiles";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                profiles.add(new Profile(
                    result.getString("name"),
                    result.getString("email"),
                    result.getString("department"),
                    result.getString("role")
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
        return profiles;
    }

    @Override
    public boolean deleteById(String email) {
        String sql = "DELETE FROM profiles WHERE email = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
