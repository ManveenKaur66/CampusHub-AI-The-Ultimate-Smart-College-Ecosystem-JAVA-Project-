import java.io.Serializable;

public class Registration implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int eventId;
    private final String email;

    public Registration(int eventId, String email) {
        this.eventId = eventId;
        this.email = email;
    }

    public int getEventId() { return eventId; }
    public String getEmail() { return email; }
}
