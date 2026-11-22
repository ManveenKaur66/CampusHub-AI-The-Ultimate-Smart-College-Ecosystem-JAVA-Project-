import java.util.List;
import java.util.Optional;

public interface GenericRepository<T, ID> {
    void save(T value);
    Optional<T> findById(ID id);
    List<T> findAll();
    boolean deleteById(ID id);
}
