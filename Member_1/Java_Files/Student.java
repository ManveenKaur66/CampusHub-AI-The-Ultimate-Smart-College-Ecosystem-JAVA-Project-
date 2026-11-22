public class Student extends User {
    private final String studentId;
    private final String department;

    public Student(
        String name, String email, String passwordHash,
        String studentId, String department
    ) {
        super(name, email, passwordHash, "STUDENT");
        this.studentId = studentId;
        this.department = department;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getDepartment() {
        return department;
    }
}
