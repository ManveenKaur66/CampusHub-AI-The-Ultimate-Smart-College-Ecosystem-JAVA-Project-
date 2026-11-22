import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class NotificationService {
    private final Store store;

    public NotificationService(Store store) {
        this.store = store;
    }

    public List<Notice> forUser(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        String cleanEmail = email.trim();
        return store.notices.stream()
            .filter(notice -> notice.getRecipient() != null
                && notice.getRecipient().equalsIgnoreCase(cleanEmail))
            .sorted(Comparator.comparing(Notice::isRead)
                .thenComparing(Notice::getTime, Comparator.reverseOrder()))
            .collect(Collectors.toList());
    }

    public long unreadCount(String email) {
        return forUser(email).stream().filter(notice -> !notice.isRead()).count();
    }

    public synchronized void markAllRead(String email) {
        List<Notice> userNotices = forUser(email);
        boolean changed = false;
        for (Notice notice : userNotices) {
            if (!notice.isRead()) {
                notice.markRead();
                changed = true;
            }
        }
        if (changed) {
            store.save();
        }
    }

    public synchronized void checkDeadlines() {
        LocalDate today = LocalDate.now();
        boolean addedAny = false;

        for (Assignment assignment : store.assignments) {
            if (!assignment.isSubmitted()
                && !assignment.getDueDate().isBefore(today)
                && !assignment.getDueDate().isAfter(today.plusDays(3))) {
                String message = "Reminder: '" + assignment.getTitle()
                    + "' is due on " + assignment.getDueDate();
                if (queueReminderIfMissing(assignment.getOwner(), message)) {
                    addedAny = true;
                }
            }
        }

        for (Event event : store.events) {
            if (!event.getDate().isBefore(today)
                && !event.getDate().isAfter(today.plusDays(2))) {
                for (Registration registration : store.registrations) {
                    if (registration.getEventId() == event.getId()) {
                        String message = "Reminder: event '" + event.getName()
                            + "' is on " + event.getDate();
                        if (queueReminderIfMissing(registration.getEmail(), message)) {
                            addedAny = true;
                        }
                    }
                }
            }
        }

        if (addedAny) {
            store.save();
        }
    }

    private boolean queueReminderIfMissing(String email, String message) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        boolean userExists = store.users.stream().anyMatch(
            u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(cleanEmail)
        );
        if (!userExists) {
            return false;
        }
        boolean exists = store.notices.stream().anyMatch(notice ->
            notice.getRecipient() != null
                && notice.getRecipient().equalsIgnoreCase(cleanEmail)
                && notice.getMessage().equals(message));
        if (!exists) {
            store.notices.add(new Notice(cleanEmail, message));
            return true;
        }
        return false;
    }
}
