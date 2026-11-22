import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class DirectoryService {
    private final Store store;

    public DirectoryService(Store store) {
        this.store = store;
    }

    public void add(String name, String email, String department, String role) {
        if (name.isBlank() || email.isBlank() || department.isBlank()) {
            throw new IllegalArgumentException("All fields are required.");
        }
        if (!email.matches("[^@ ]+@[^@ ]+\\.[^@ ]+")) {
            throw new IllegalArgumentException("Invalid email.");
        }
        if (store.users.stream().noneMatch(
            user -> user.getEmail().equalsIgnoreCase(email.trim())
        )) {
            throw new IllegalArgumentException("Email must belong to a registered user account.");
        }
        if (store.profiles.stream().anyMatch(
            profile -> profile.getEmail().equalsIgnoreCase(email.trim())
        )) {
            throw new IllegalArgumentException("Profile for this email already exists.");
        }

        String normalizedRole = role.toUpperCase(Locale.ROOT);
        if (!Set.of("STUDENT", "FACULTY").contains(normalizedRole)) {
            throw new IllegalArgumentException("Role must be STUDENT or FACULTY.");
        }
        store.profiles.add(new Profile(name, email, department, normalizedRole));
        store.save();
    }

    public List<Profile> search(String query, String department, String role) {
        return store.profiles.stream()
            .filter(profile -> query.isBlank()
                || profile.getName().toLowerCase().contains(query.toLowerCase())
                || profile.getEmail().toLowerCase().contains(query.toLowerCase()))
            .filter(profile -> department.equalsIgnoreCase("All")
                || profile.getDepartment().equalsIgnoreCase(department))
            .filter(profile -> role.equalsIgnoreCase("All")
                || profile.getRole().equalsIgnoreCase(role))
            .sorted(Comparator.comparing(
                Profile::getName, String.CASE_INSENSITIVE_ORDER
            ))
            .collect(Collectors.toList());
    }

    public List<Profile> sortDepartment() {
        return store.profiles.stream()
            .sorted(Comparator.comparing(
                Profile::getDepartment, String.CASE_INSENSITIVE_ORDER
            ).thenComparing(Profile::getName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    public Map<String, Long> departmentReport() {
        return store.profiles.stream().collect(Collectors.groupingBy(
            Profile::getDepartment, TreeMap::new, Collectors.counting()
        ));
    }

    public Map<String, Long> roleReport() {
        return store.profiles.stream().collect(Collectors.groupingBy(
            Profile::getRole, TreeMap::new, Collectors.counting()
        ));
    }

    public Set<String> departments() {
        return store.profiles.stream()
            .map(Profile::getDepartment)
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
