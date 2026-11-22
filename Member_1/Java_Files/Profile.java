import java.io.Serializable;

public class Profile implements Serializable, Comparable<Profile> {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String email;
    private final String department;
    private final String role;

    public Profile(String name, String email, String department, String role) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.role = role;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }

    @Override
    public int compareTo(Profile other) {
        return name.compareToIgnoreCase(other.name);
    }

    @Override
    public String toString() {
        return name + " | " + email + " | " + department + " | " + role;
    }
}
