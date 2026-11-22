import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public class AuthService {
    private final Store store;
    private User current;

    public AuthService(Store store) {
        this.store = store;
    }

    public static String hash(String password) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : bytes) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to secure password.", exception);
        }
    }

    public synchronized boolean registerStudent(
        String rollNumber, String name, String email, String password
    ) {
        String cleanRoll = (rollNumber == null) ? "" : rollNumber.trim();
        String approvedName = store.studentRoster.get(cleanRoll);
        if (approvedName == null) {
            throw new IllegalArgumentException("Roll number is not in the approved class list.");
        }
        if (name == null || !approvedName.equalsIgnoreCase(name.trim())) {
            throw new IllegalArgumentException("Name does not match the supplied roll number.");
        }
        boolean rollTaken = store.users.stream().anyMatch(
            u -> u instanceof Student st && cleanRoll.equals(st.getStudentId())
        );
        if (rollTaken) {
            throw new IllegalArgumentException("An account for this roll number already exists.");
        }
        return createAccount(approvedName, email, password, "STUDENT", cleanRoll);
    }

    public synchronized boolean registerFaculty(
        String name, String email, String password
    ) {
        return createAccount(name, email, password, "FACULTY", null);
    }

    private boolean createAccount(
        String name, String email, String password, String role, String rollNumber
    ) {
        if (name == null || name.isBlank()
            || email == null || email.isBlank()
            || password == null || password.length() < 6) {
            throw new IllegalArgumentException(
                "Enter a valid name/email and a password of at least 6 characters."
            );
        }
        String cleanName = name.trim();
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!cleanEmail.matches("[^@ ]+@[^@ ]+[^@ ]+")) {
            throw new IllegalArgumentException(
                "Enter a valid name/email and a password of at least 6 characters."
            );
        }
        if (store.users.stream().anyMatch(
            user -> user.getEmail().equalsIgnoreCase(cleanEmail)
        )) {
            return false;
        }

        User user;
        if ("STUDENT".equals(role)) {
            user = new Student(cleanName, cleanEmail, hash(password),
                rollNumber, "Not specified");
        } else {
            user = new Faculty(cleanName, cleanEmail, hash(password), "Not specified");
        }
        Profile profile = new Profile(cleanName, cleanEmail, "Not specified", role);

        store.users.add(user);
        store.profiles.add(profile);
        try {
            store.save();
        } catch (RuntimeException exception) {
            store.users.remove(user);
            store.profiles.remove(profile);
            throw exception;
        }
        return true;
    }

    public synchronized boolean login(String email, String password) {
        if (email == null || password == null) {
            return false;
        }
        String cleanEmail = email.trim();
        String passwordHash = hash(password);
        current = store.users.stream()
            .filter(user -> user.getEmail().equalsIgnoreCase(cleanEmail))
            .filter(user -> user.getPasswordHash().equals(passwordHash))
            .findFirst()
            .orElse(null);
        return current != null;
    }

    public synchronized void logout() {
        current = null;
    }

    public synchronized User getCurrent() {
        return current;
    }
}
