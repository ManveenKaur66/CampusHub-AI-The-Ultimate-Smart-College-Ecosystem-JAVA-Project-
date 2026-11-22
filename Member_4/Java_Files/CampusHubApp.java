import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CampusHubApp {
    private static final Scanner scanner = new Scanner(System.in);
    private static Store database;
    private static AuthService auth;
    private static DirectoryService directory;
    private static AssignmentService assignments;
    private static EventService events;
    private static NotificationService notifications;

    @SuppressWarnings("unused")
    private static final Class<?>[] JDBC_COMPONENTS = {
        DBConnection.class, UserDAO.class, DirectoryDAO.class,
        AssignmentDAO.class, EventDAO.class, RegistrationDAO.class,
        NotificationDAO.class
    };

    private static String ask(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static int number(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(ask(prompt));
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static LocalDate date(String prompt) {
        while (true) {
            try {
                return LocalDate.parse(ask(prompt + " (YYYY-MM-DD): "));
            } catch (Exception exception) {
                System.out.println("Invalid date. Use YYYY-MM-DD.");
            }
        }
    }

    private static void show(Collection<?> values) {
        if (values.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        values.forEach(System.out::println);
    }

    public static void main(String[] args) {
        try {
            database = Store.load();
            auth = new AuthService(database);
            directory = new DirectoryService(database);
            assignments = new AssignmentService(database);
            events = new EventService(database);
            notifications = new NotificationService(database);
        } catch (Exception exception) {
            System.out.println("\n[STARTUP ERROR] " + exception.getMessage());
            return;
        }

        System.out.println("==============================================");
        System.out.println("             CAMPUSHUB AI - JAVA CLI");
        System.out.println("==============================================");
        System.out.println("Approved student roll numbers: "
            + database.studentRoster.size());
        notifications.checkDeadlines();

        while (true) {
            System.out.println("\n1. Student Registration"
                + "\n2. Faculty Registration"
                + "\n3. Login"
                + "\n0. Exit");

            int choice = number("Choice: ");
            if (choice == 0) {
                database.save();
                System.out.println("Goodbye!");
                return;
            }

            try {
                switch (choice) {
                    case 1 -> registerStudent();
                    case 2 -> registerFaculty();
                    case 3 -> login();
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception exception) {
                System.out.println("Error: " + exception.getMessage());
            }
        }
    }

    private static void registerStudent() {
        System.out.println("\nSTUDENT REGISTRATION");
        String rollNumber = ask("Class roll number: ");
        String name = ask("Full name (as per class list): ");
        String email = ask("Email: ");
        String password = ask("Create password (minimum 6 characters): ");

        boolean registered = auth.registerStudent(
            rollNumber, name, email, password
        );
        System.out.println(registered
            ? "Student account created. You can now log in."
            : "An account with this email already exists.");
    }

    private static void registerFaculty() {
        System.out.println("\nFACULTY REGISTRATION");
        String name = ask("Full name: ");
        String email = ask("Email: ");
        String password = ask("Create password (minimum 6 characters): ");

        boolean registered = auth.registerFaculty(name, email, password);
        System.out.println(registered
            ? "Faculty account created. You can now log in."
            : "An account with this email already exists.");
    }

    private static void login() {
        String email = ask("Email: ");
        String password = ask("Password: ");

        if (auth.login(email, password)) {
            System.out.println("Welcome, " + auth.getCurrent().getName() + "!");
            dashboard();
        } else {
            System.out.println("Incorrect email or password.");
        }
    }

    private static void dashboard() {
        while (auth.getCurrent() != null) {
            notifications.checkDeadlines();
            User user = auth.getCurrent();

            System.out.println("\n---------------- DASHBOARD ----------------");
            System.out.println("Signed in: " + user);
            System.out.println("1. Campus Directory"
                + "\n2. Assignments"
                + "\n3. Campus Events"
                + "\n4. My Profile"
                + "\n5. Notifications"
                + "\n6. CSV Tools"
                + "\n7. Java Concepts Demo"
                + "\n8. Logout");

            switch (number("Choice: ")) {
                case 1 -> directoryMenu();
                case 2 -> assignmentMenu();
                case 3 -> eventMenu();
                case 4 -> profile();
                case 5 -> notificationMenu();
                case 6 -> csvMenu();
                case 7 -> conceptsMenu();
                case 8 -> auth.logout();
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static boolean isFaculty() {
        return auth.getCurrent().getRole().equals("FACULTY");
    }

    private static void profile() {
        User user = auth.getCurrent();
        System.out.println("\nUSER PROFILE"
            + "\nName: " + user.getName()
            + "\nEmail: " + user.getEmail()
            + "\nRole: " + user.getRole());
    }

    private static void directoryMenu() {
        while (true) {
            System.out.println("\nDIRECTORY"
                + "\n1. List all"
                + "\n2. Add profile (Faculty)"
                + "\n3. Search/filter"
                + "\n4. Sort by department"
                + "\n5. Reports"
                + "\n0. Back");

            int choice = number("Choice: ");
            if (choice == 0) {
                return;
            }

            try {
                switch (choice) {
                    case 1 -> show(directory.search("", "All", "All"));
                    case 2 -> {
                        if (!isFaculty()) {
                            System.out.println("Only faculty can add directory profiles.");
                            break;
                        }
                        directory.add(
                            ask("Name: "),
                            ask("Email: "),
                            ask("Department: "),
                            ask("Role (STUDENT/FACULTY): ")
                        );
                        System.out.println("Profile added.");
                    }
                    case 3 -> show(directory.search(
                        ask("Name/email (blank = all): "),
                        ask("Department (All = any): "),
                        ask("Role (All = any): ")
                    ));
                    case 4 -> show(directory.sortDepartment());
                    case 5 -> {
                        System.out.println("Total profiles: " + database.profiles.size());
                        System.out.println("Department-wise count: "
                            + directory.departmentReport());
                        System.out.println("Role-wise count: "
                            + directory.roleReport());
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception exception) {
                System.out.println("Directory error: " + exception.getMessage());
            }
        }
    }

    private static void assignmentMenu() {
        while (true) {
            System.out.println("\nASSIGNMENTS"
                + "\n1. My assignments"
                + "\n2. Create/assign (Faculty)"
                + "\n3. Edit assignment (Faculty)"
                + "\n4. Mark submitted"
                + "\n5. Pending assignments"
                + "\n6. Submitted assignments"
                + "\n7. Deadline priority queue"
                + "\n0. Back");

            int choice = number("Choice: ");
            String email = auth.getCurrent().getEmail();
            if (choice == 0) {
                return;
            }

            try {
                switch (choice) {
                    case 1 -> show(assignments.mine(email, null));
                    case 2 -> {
                        if (!isFaculty()) {
                            System.out.println("Only faculty can create assignments.");
                            break;
                        }
                        String owner = ask("Assign to registered student email: ");
                        assignments.add(
                            owner,
                            ask("Title: "),
                            ask("Description: "),
                            date("Due date"),
                            email
                        );
                        System.out.println("Assignment created successfully.");
                    }
                    case 3 -> {
                        if (!isFaculty()) {
                            System.out.println("Only faculty can edit assignments.");
                            break;
                        }
                        int id = number("Assignment ID: ");
                        boolean updated = assignments.edit(
                            id, email, ask("New title: "),
                            ask("New description: "), date("New due date")
                        );
                        System.out.println(updated
                            ? "Assignment updated."
                            : "Assignment not found or permission denied.");
                    }
                    case 4 -> System.out.println(
                        assignments.submit(number("Assignment ID: "), email)
                            ? "Marked as submitted."
                            : "Assignment not found or already submitted."
                    );
                    case 5 -> show(assignments.mine(email, false));
                    case 6 -> show(assignments.mine(email, true));
                    case 7 -> {
                        PriorityQueue<Assignment> queue =
                            assignments.deadlineQueue(email);
                        if (queue.isEmpty()) {
                            System.out.println("No pending assignments.");
                        }
                        while (!queue.isEmpty()) {
                            System.out.println(queue.poll());
                        }
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception exception) {
                System.out.println("Assignment error: " + exception.getMessage());
            }
        }
    }

    private static void eventMenu() {
        while (true) {
            System.out.println("\nEVENTS"
                + "\n1. Upcoming/search"
                + "\n2. Add event (Faculty)"
                + "\n3. Register"
                + "\n4. Cancel registration"
                + "\n5. My registrations"
                + "\n6. Registration counts (Faculty)"
                + "\n0. Back");

            int choice = number("Choice: ");
            String email = auth.getCurrent().getEmail();
            if (choice == 0) {
                return;
            }

            try {
                switch (choice) {
                    case 1 -> show(events.search(
                        ask("Search name (blank = all): "),
                        ask("Category (All = any): ")
                    ));
                    case 2 -> {
                        if (!isFaculty()) {
                            System.out.println("Only faculty can add events.");
                            break;
                        }
                        events.add(
                            ask("Event name: "),
                            ask("Category: "),
                            ask("Venue: "),
                            date("Event date")
                        );
                        System.out.println("Event created; users notified.");
                    }
                    case 3 -> System.out.println(
                        events.register(number("Event ID: "), email)
                            ? "Registered successfully."
                            : "Could not register."
                    );
                    case 4 -> System.out.println(
                        events.cancel(number("Event ID: "), email)
                            ? "Registration cancelled."
                            : "Registration not found."
                    );
                    case 5 -> {
                        for (Registration registration : events.registrations(email)) {
                            database.events.stream()
                                .filter(event -> event.getId()
                                    == registration.getEventId())
                                .forEach(System.out::println);
                        }
                    }
                    case 6 -> {
                        if (!isFaculty()) {
                            System.out.println("Only faculty can view registration counts.");
                            break;
                        }
                        for (Event event : database.events) {
                            System.out.println(event + " | Registrations: "
                                + events.registrationCount(event.getId()));
                        }
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception exception) {
                System.out.println("Event error: " + exception.getMessage());
            }
        }
    }

    private static void notificationMenu() {
        String email = auth.getCurrent().getEmail();
        List<Notice> notices = notifications.forUser(email);
        long unread = notifications.unreadCount(email);
        System.out.println("Unread notifications: " + unread);
        show(notices);

        if (unread > 0 && ask("Mark all notifications as read? (y/n): ").equalsIgnoreCase("y")) {
            notifications.markAllRead(email);
            System.out.println("Notifications marked as read.");
        }
    }

    private static void csvMenu() {
        System.out.println("\nCSV TOOLS"
            + "\n1. Export directory CSV"
            + "\n2. Import directory CSV (Faculty)"
            + "\n3. Export assignments CSV"
            + "\n4. Import assignments CSV (Faculty)"
            + "\n0. Back");

        int choice = number("Choice: ");
        if (choice == 0) {
            return;
        }
        if ((choice == 2 || choice == 4) && !isFaculty()) {
            System.out.println("Only faculty can import CSV data.");
            return;
        }

        try {
            String path = ask("CSV file path (e.g. profiles.csv): ");
            switch (choice) {
                case 1 -> {
                    CsvUtil.exportProfiles(database.profiles, path);
                    System.out.println("Profiles exported.");
                }
                case 2 -> {
                    int count = CsvUtil.importProfiles(database, path);
                    database.save();
                    System.out.println(count + " profiles imported.");
                }
                case 3 -> {
                    assignments.exportCsv(path);
                    System.out.println("Assignments exported.");
                }
                case 4 -> System.out.println(
                    assignments.importCsv(path) + " assignments imported."
                );
                default -> System.out.println("Invalid choice.");
            }
        } catch (Exception exception) {
            System.out.println("CSV error: " + exception.getMessage());
        }
    }

    private static void conceptsMenu() {
        System.out.println("\nJAVA CONCEPTS"
            + "\n1. Producer-Consumer (wait/notify)"
            + "\n2. ExecutorService demo"
            + "\n3. Collections/Syllabus concepts demo"
            + "\n0. Back");

        int choice = number("Choice: ");
        if (choice == 1) {
            ProducerConsumerDemo.runDemo();
        } else if (choice == 3) {
            CollectionConceptDemo.run();
        } else if (choice == 2) {
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                for (int i = 1; i <= 4; i++) {
                    int task = i;
                    pool.submit(() -> System.out.println(
                        "Executor task " + task + " executed by "
                            + Thread.currentThread().getName()
                    ));
                }
            } finally {
                pool.shutdown();
                try {
                    if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                        pool.shutdownNow();
                    }
                } catch (InterruptedException exception) {
                    pool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        } else if (choice != 0) {
            System.out.println("Invalid choice.");
        }
    }
}
