import java.io.Serializable;
import java.time.LocalDate;

public class Assignment implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String owner;
    private String title;
    private String description;
    private final String createdBy;
    private LocalDate dueDate;
    private boolean submitted;

    public Assignment(
        int id, String owner, String title, String description,
        LocalDate dueDate, String createdBy
    ) {
        this.id = id;
        this.owner = owner;
        this.title = title;
        this.description = (description == null) ? "" : description;
        this.dueDate = dueDate;
        this.createdBy = createdBy;
    }

    public int getId() { return id; }
    public String getOwner() { return owner; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isSubmitted() { return submitted; }
    public String getCreatedBy() { return createdBy; }

    public void update(String title, String description, LocalDate dueDate) {
        this.title = title;
        this.description = (description == null) ? "" : description;
        this.dueDate = dueDate;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    @Override
    public String toString() {
        String descPart = (description != null && !description.isBlank())
            ? " | Desc: " + description
            : "";
        return id + " | " + title + descPart + " | For: " + owner
            + " | Due: " + dueDate + " | "
            + (submitted ? "SUBMITTED" : "PENDING")
            + " | By: " + (createdBy == null ? "Unknown" : createdBy);
    }
}
