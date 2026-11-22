import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Notice implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String recipient;
    private final String message;
    private boolean read;
    private final LocalDateTime time;

    public Notice(String recipient, String message) {
        this(recipient, message, false, LocalDateTime.now());
    }

    public Notice(String recipient, String message, boolean read, LocalDateTime time) {
        this.recipient = recipient;
        this.message = message;
        this.read = read;
        this.time = (time != null) ? time : LocalDateTime.now();
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void markRead() {
        read = true;
    }

    @Override
    public String toString() {
        return (read ? "[READ] " : "[UNREAD] ") + time.format(FORMATTER) + " | " + message;
    }
}
