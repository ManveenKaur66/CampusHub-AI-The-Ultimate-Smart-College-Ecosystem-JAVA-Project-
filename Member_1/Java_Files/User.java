public class User extends AbstractCampusEntity {
    private static final long serialVersionUID = 1L;

    private String name;
    private final String email;
    private final String passwordHash;
    private String role;

    public User(String name, String email, String passwordHash, String role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }

    public void setName(String name) {
        this.name = name;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String getDisplayLabel() {
        return name + " (" + role + ")";
    }

    @Override
    public String toString() {
        return name + " | " + email + " | " + role;
    }
}
