import java.io.Serializable;
import java.time.LocalDate;

public class Event implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final String category;
    private final String venue;
    private final LocalDate date;

    public Event(
        int id, String name, String category, String venue, LocalDate date
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.venue = venue;
        this.date = date;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getVenue() { return venue; }
    public LocalDate getDate() { return date; }

    @Override
    public String toString() {
        return id + " | " + name + " | " + category + " | " + date + " | " + venue;
    }
}
