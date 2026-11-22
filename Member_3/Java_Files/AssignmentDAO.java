import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssignmentDAO implements GenericRepository<Assignment, Integer> {
    @Override
    public void save(Assignment assignment) {
        String sql = "INSERT INTO assignments "
            + "(assignment_id, owner_email, created_by, title, description, due_date, status) "
            + "VALUES(?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
            + "owner_email=VALUES(owner_email), created_by=VALUES(created_by), "
            + "title=VALUES(title), description=VALUES(description), "
            + "due_date=VALUES(due_date), status=VALUES(status)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, assignment.getId());
            statement.setString(2, assignment.getOwner());
            if (assignment.getCreatedBy() != null && !assignment.getCreatedBy().isBlank()) {
                statement.setString(3, assignment.getCreatedBy());
            } else {
                statement.setNull(3, Types.VARCHAR);
            }
            statement.setString(4, assignment.getTitle());
            statement.setString(5, assignment.getDescription() == null ? "" : assignment.getDescription());
            statement.setDate(6, java.sql.Date.valueOf(assignment.getDueDate()));
            statement.setString(7,
                assignment.isSubmitted() ? "SUBMITTED" : "PENDING");
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public Optional<Assignment> findById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM assignments WHERE assignment_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapAssignment(result));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
        return Optional.empty();
    }

    @Override
    public List<Assignment> findAll() {
        List<Assignment> assignments = new ArrayList<>();
        String sql = "SELECT * FROM assignments";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                assignments.add(mapAssignment(result));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
        return assignments;
    }

    @Override
    public boolean deleteById(Integer id) {
        if (id == null) {
            return false;
        }
        String sql = "DELETE FROM assignments WHERE assignment_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Assignment mapAssignment(ResultSet result) throws SQLException {
        java.sql.Date sqlDate = result.getDate("due_date");
        LocalDate dueDate = (sqlDate != null) ? sqlDate.toLocalDate() : LocalDate.now();
        Assignment assignment = new Assignment(
            result.getInt("assignment_id"),
            result.getString("owner_email"),
            result.getString("title"),
            result.getString("description"),
            dueDate,
            result.getString("created_by")
        );
        assignment.setSubmitted(
            "SUBMITTED".equals(result.getString("status"))
        );
        return assignment;
    }
}
