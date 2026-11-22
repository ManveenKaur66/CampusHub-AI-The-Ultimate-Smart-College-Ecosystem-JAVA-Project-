public class Faculty extends User {
    private final String department;

    public Faculty(String name, String email, String passwordHash, String department) {
        super(name, email, passwordHash, "FACULTY");
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }
}
