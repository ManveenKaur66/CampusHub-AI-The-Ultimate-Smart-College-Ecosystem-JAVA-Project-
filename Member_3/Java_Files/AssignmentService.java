import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class AssignmentService {
    private final Store store;
    private final BlockingQueue<String> workQueue = new ArrayBlockingQueue<>(100);
    private final Thread consumer;

    public AssignmentService(Store store) {
        this.store = store;
        consumer = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String msg = workQueue.take();
                    System.out.println("\n[Notification worker] " + msg);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "AssignmentNotificationConsumer");
        consumer.setDaemon(true);
        consumer.start();
    }

    public synchronized void add(
        String owner, String title, String description,
        LocalDate dueDate, String creator
    ) {
        if (owner == null || owner.isBlank()
            || title == null || title.isBlank()
            || dueDate == null || dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                "Choose a student, enter a title and use a due date today or later."
            );
        }
        String cleanOwner = owner.trim().toLowerCase(Locale.ROOT);
        String cleanTitle = title.trim();
        String cleanDesc = (description == null) ? "" : description.trim();
        String cleanCreator = (creator == null) ? null : creator.trim().toLowerCase(Locale.ROOT);

        boolean studentExists = store.users.stream().anyMatch(user ->
            user.getEmail().equalsIgnoreCase(cleanOwner)
                && "STUDENT".equalsIgnoreCase(user.getRole())
        );
        if (!studentExists) {
            throw new IllegalArgumentException(
                "No registered student account exists for that email."
            );
        }

        Assignment assignment = new Assignment(
            store.nextAssignmentId++, cleanOwner, cleanTitle,
            cleanDesc, dueDate, cleanCreator
        );
        store.assignments.add(assignment);
        store.notices.add(new Notice(
            cleanOwner,
            "New assignment: " + cleanTitle + " (due " + dueDate + ")"
        ));
        store.save();
        workQueue.offer("Assignment notification queued for " + cleanOwner + ": " + cleanTitle);
    }

    public List<Assignment> mine(String userEmail, Boolean submitted) {
        if (userEmail == null || userEmail.isBlank()) {
            return List.of();
        }
        String cleanEmail = userEmail.trim();
        return store.assignments.stream()
            .filter(assignment ->
                assignment.getOwner().equalsIgnoreCase(cleanEmail)
                    || (assignment.getCreatedBy() != null
                        && assignment.getCreatedBy().equalsIgnoreCase(cleanEmail)))
            .filter(assignment -> submitted == null
                || assignment.isSubmitted() == submitted)
            .sorted(Comparator.comparing(Assignment::getDueDate)
                .thenComparingInt(Assignment::getId))
            .toList();
    }

    public synchronized boolean edit(
        int id, String creator, String title,
        String description, LocalDate dueDate
    ) {
        if (creator == null || creator.isBlank()) {
            return false;
        }
        String cleanCreator = creator.trim();
        for (Assignment assignment : store.assignments) {
            if (assignment.getId() == id
                && assignment.getCreatedBy() != null
                && assignment.getCreatedBy().equalsIgnoreCase(cleanCreator)) {
                if (title == null || title.isBlank()
                    || dueDate == null || dueDate.isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("Enter a title and a valid due date.");
                }
                String cleanTitle = title.trim();
                String cleanDesc = (description == null) ? "" : description.trim();
                assignment.update(cleanTitle, cleanDesc, dueDate);
                store.notices.add(new Notice(
                    assignment.getOwner(),
                    "Assignment updated: " + cleanTitle + " (due " + dueDate + ")"
                ));
                store.save();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean submit(int id, String owner) {
        if (owner == null || owner.isBlank()) {
            return false;
        }
        String cleanOwner = owner.trim();
        for (Assignment assignment : store.assignments) {
            if (assignment.getId() == id
                && assignment.getOwner().equalsIgnoreCase(cleanOwner)
                && !assignment.isSubmitted()) {
                assignment.setSubmitted(true);
                store.notices.add(new Notice(
                    assignment.getOwner(),
                    "Assignment submitted: " + assignment.getTitle()
                ));
                if (assignment.getCreatedBy() != null && !assignment.getCreatedBy().isBlank()) {
                    store.notices.add(new Notice(
                        assignment.getCreatedBy(),
                        "Student " + assignment.getOwner() + " submitted: " + assignment.getTitle()
                    ));
                }
                store.save();
                return true;
            }
        }
        return false;
    }

    public PriorityQueue<Assignment> deadlineQueue(String userEmail) {
        PriorityQueue<Assignment> queue =
            new PriorityQueue<>(Comparator.comparing(Assignment::getDueDate)
                .thenComparingInt(Assignment::getId));
        for (Assignment assignment : mine(userEmail, false)) {
            queue.add(assignment);
        }
        return queue;
    }

    public void exportCsv(String path) {
        CsvUtil.exportAssignments(store.assignments, path);
    }

    public synchronized int importCsv(String path) {
        int count = CsvUtil.importAssignments(store, path);
        store.save();
        return count;
    }
}
