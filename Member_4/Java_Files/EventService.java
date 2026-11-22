import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class EventService {
    private final Store store;

    public EventService(Store store) {
        this.store = store;
    }

    public synchronized void add(
        String name, String category, String venue, LocalDate date
    ) {
        if (name.isBlank() || category.isBlank() || venue.isBlank()
            || date == null || date.isBefore(LocalDate.now())) {
            throw new InvalidEventDateException(
                "Enter event details and a date today or later."
            );
        }
        Event event = new Event(
            store.nextEventId++, name.trim(), category.trim(), venue.trim(), date
        );
        store.events.add(event);
        for (User user : store.users) {
            store.notices.add(new Notice(user.getEmail(),
                "New campus event: " + name + " on " + date));
        }
        store.save();
    }

    public List<Event> search(String query, String category) {
        return store.events.stream()
            .filter(event -> !event.getDate().isBefore(LocalDate.now()))
            .filter(event -> query.isBlank()
                || event.getName().toLowerCase().contains(query.toLowerCase()))
            .filter(event -> category.equalsIgnoreCase("All")
                || event.getCategory().equalsIgnoreCase(category))
            .sorted(Comparator.comparing(Event::getDate))
            .toList();
    }

    public synchronized boolean register(int id, String email) {
        Event event = store.events.stream()
            .filter(item -> item.getId() == id)
            .findFirst()
            .orElseThrow(() -> new EventNotFoundException("Event not found."));
        if (event.getDate().isBefore(LocalDate.now())) {
            throw new InvalidEventDateException("Cannot register for a past event.");
        }
        if (store.registrations.stream().anyMatch(registration ->
            registration.getEventId() == id
                && registration.getEmail().equalsIgnoreCase(email))) {
            throw new AlreadyRegisteredException(
                "You are already registered for this event."
            );
        }
        store.registrations.add(new Registration(id, email));
        store.notifyUser(email, "Registration confirmed: " + event.getName());
        store.save();
        return true;
    }

    public synchronized boolean cancel(int id, String email) {
        boolean removed = store.registrations.removeIf(registration ->
            registration.getEventId() == id
                && registration.getEmail().equalsIgnoreCase(email));
        if (removed) {
            store.notifyUser(email, "Event registration cancelled: " + id);
            store.save();
        }
        return removed;
    }

    public List<Registration> registrations(String email) {
        return store.registrations.stream()
            .filter(registration ->
                registration.getEmail().equalsIgnoreCase(email))
            .toList();
    }

    public int registrationCount(int id) {
        return (int) store.registrations.stream()
            .filter(registration -> registration.getEventId() == id)
            .count();
    }
}
