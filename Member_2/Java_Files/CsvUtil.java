import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CsvUtil {
    private CsvUtil() {
    }

    public static void exportProfiles(List<Profile> profiles, String path) {
        List<String> rows = profiles.stream()
            .map(profile -> line(
                profile.getName(), profile.getEmail(),
                profile.getDepartment(), profile.getRole()
            ))
            .toList();
        write(path, "name,email,department,role", rows);
    }

    public static int importProfiles(Store store, String path) {
        int count = 0;
        try {
            List<String> lines = Files.readAllLines(Path.of(path));
            for (int i = 1; i < lines.size(); i++) {
                String[] values = parse(lines.get(i));
                if (values.length < 4 || values[1].isBlank()) {
                    continue;
                }
                String role = values[3].trim().toUpperCase();
                if (!role.equals("STUDENT") && !role.equals("FACULTY")) {
                    continue;
                }
                boolean userExists = store.users.stream().anyMatch(user ->
                    user.getEmail().equalsIgnoreCase(values[1].trim()));
                boolean exists = store.profiles.stream().anyMatch(profile ->
                    profile.getEmail().equalsIgnoreCase(values[1].trim()));
                if (userExists && !exists) {
                    store.profiles.add(new Profile(
                        values[0].trim(), values[1].trim(), values[2].trim(), role
                    ));
                    count++;
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Could not read CSV: " + exception.getMessage()
            );
        }
        return count;
    }

    public static void exportAssignments(List<Assignment> assignments, String path) {
        List<String> rows = assignments.stream()
            .map(assignment -> line(
                assignment.getOwner(), assignment.getTitle(),
                assignment.getDescription(), assignment.getDueDate().toString(),
                assignment.getCreatedBy(), String.valueOf(assignment.isSubmitted())
            ))
            .toList();
        write(path,
            "owner,title,description,dueDate,createdBy,submitted", rows);
    }

    public static int importAssignments(Store store, String path) {
        int count = 0;
        try {
            List<String> lines = Files.readAllLines(Path.of(path));
            for (int i = 1; i < lines.size(); i++) {
                String[] values = parse(lines.get(i));
                if (values.length < 6 || values[0].isBlank()
                    || values[1].isBlank()) {
                    continue;
                }
                String ownerEmail = values[0].trim().toLowerCase(java.util.Locale.ROOT);
                String creatorEmail = values[4].trim().toLowerCase(java.util.Locale.ROOT);
                boolean studentExists = store.users.stream().anyMatch(user ->
                    user.getEmail().equalsIgnoreCase(ownerEmail)
                        && user.getRole().equals("STUDENT"));
                boolean facultyExists = store.users.stream().anyMatch(user ->
                    user.getEmail().equalsIgnoreCase(creatorEmail)
                        && user.getRole().equals("FACULTY"));
                if (!studentExists || !facultyExists) {
                    continue;
                }

                Assignment assignment = new Assignment(
                    store.nextAssignmentId++, ownerEmail, values[1].trim(), values[2].trim(),
                    LocalDate.parse(values[3].trim()), creatorEmail
                );
                assignment.setSubmitted(Boolean.parseBoolean(values[5]));
                store.assignments.add(assignment);
                count++;
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                "CSV import failed: " + exception.getMessage()
            );
        }
        return count;
    }

    private static void write(String path, String header, List<String> rows) {
        try {
            List<String> allRows = new ArrayList<>();
            allRows.add(header);
            allRows.addAll(rows);
            Files.write(Path.of(path), allRows);
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Could not write CSV: " + exception.getMessage()
            );
        }
    }

    private static String line(String... values) {
        return Arrays.stream(values)
            .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
            .reduce((first, second) -> first + "," + second)
            .orElse("");
    }

    private static String[] parse(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < line.length()
                    && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(character);
            }
        }
        values.add(value.toString());
        return values.toArray(new String[0]);
    }
}
