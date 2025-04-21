import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

public class SimpleUniversitySystem {
    private static Map<String, User> users = new HashMap<>();
    private static JFrame mainFrame;
    private static User currentUser;
    private static List<Student> students = new ArrayList<>();
    private static List<Faculty> faculty = new ArrayList<>();
    private static List<Course> courses = new ArrayList<>();
    private static List<Book> books = new ArrayList<>();
    private static List<TimeTableEntry> timeTable = new ArrayList<>();
    private static Map<String, Map<String, List<Attendance>>> attendanceRecords = new HashMap<>(); // studentId -> (courseCode -> attendance list)
    private static Map<String, Map<String, Grade>> grades = new HashMap<>(); // studentId -> (courseCode -> grade)
    private static Map<String, List<FeeRecord>> feeRecords = new HashMap<>(); // studentId -> fee records
    private static Map<String, List<BookIssue>> bookIssues = new HashMap<>(); // studentId -> issued books
    
    // Custom colors
    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private static final Color SECONDARY_COLOR = new Color(52, 152, 219);
    private static final Color BACKGROUND_COLOR = new Color(236, 240, 241);
    private static final Color ACCENT_COLOR = new Color(46, 204, 113);
    private static final Color TEXT_COLOR = new Color(44, 62, 80);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font REGULAR_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    
    private static JTabbedPane mainTabbedPane; // Add this field to access tabbedPane globally
    private static final String LOGO_PATH = "christ_logo.png"; // Path to the logo
    
    // Data models
    static class Student {
        String id;
        String name;
        String department;
        String email;
        Set<String> enrolledCourses;
        
        Student(String id, String name, String department, String email) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.email = email;
            this.enrolledCourses = new HashSet<>();
        }
        
        Object[] toTableRow() {
            return new Object[]{id, name, department, email};
        }
    }
    
    static class Faculty {
        String id;
        String name;
        String department;
        String specialization;
        
        Faculty(String id, String name, String department, String specialization) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.specialization = specialization;
        }
        
        Object[] toTableRow() {
            return new Object[]{id, name, department, specialization};
        }
    }
    
    static class Course {
        String code;
        String name;
        int credits;
        String instructor;
        
        Course(String code, String name, int credits, String instructor) {
            this.code = code;
            this.name = name;
            this.credits = credits;
            this.instructor = instructor;
        }
        
        Object[] toTableRow() {
            return new Object[]{code, name, credits, instructor};
        }
    }

    static class Attendance {
        LocalDate date;
        boolean present;
        
        Attendance(LocalDate date, boolean present) {
            this.date = date;
            this.present = present;
        }
    }
    
    static class Grade {
        double midterm;
        double finalExam;
        double assignments;
        double totalGrade;
        
        Grade(double midterm, double finalExam, double assignments) {
            this.midterm = midterm;
            this.finalExam = finalExam;
            this.assignments = assignments;
            this.totalGrade = calculateTotal();
        }
        
        double calculateTotal() {
            return (midterm * 0.3) + (finalExam * 0.5) + (assignments * 0.2);
        }
    }

    static class Book {
        String id;
        String title;
        String author;
        String category;
        int totalCopies;
        int availableCopies;

        Book(String id, String title, String author, String category, int totalCopies) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.category = category;
            this.totalCopies = totalCopies;
            this.availableCopies = totalCopies;
        }

        Object[] toTableRow() {
            return new Object[]{id, title, author, category, availableCopies + "/" + totalCopies};
        }
    }

    static class BookIssue {
        String bookId;
        LocalDate issueDate;
        LocalDate dueDate;
        boolean returned;

        BookIssue(String bookId, LocalDate issueDate) {
            this.bookId = bookId;
            this.issueDate = issueDate;
            this.dueDate = issueDate.plusDays(14); // 2 weeks lending period
            this.returned = false;
        }

        Object[] toTableRow() {
            return new Object[]{bookId, issueDate, dueDate, returned ? "Returned" : "Issued"};
        }
    }

    static class FeeRecord {
        String description;
        double amount;
        LocalDate dueDate;
        boolean paid;

        FeeRecord(String description, double amount, LocalDate dueDate) {
            this.description = description;
            this.amount = amount;
            this.dueDate = dueDate;
            this.paid = false;
        }

        Object[] toTableRow() {
            return new Object[]{description, String.format("$%.2f", amount), dueDate, paid ? "Paid" : "Pending"};
        }
    }

    static class TimeTableEntry {
        String courseCode;
        String day;
        String startTime;
        String endTime;
        String room;

        TimeTableEntry(String courseCode, String day, String startTime, String endTime, String room) {
            this.courseCode = courseCode;
            this.day = day;
            this.startTime = startTime;
            this.endTime = endTime;
            this.room = room;
        }

        Object[] toTableRow() {
            return new Object[]{courseCode, day, startTime, endTime, room};
        }
    }

    // User class for authentication and profile management
    static class User {
        String username;
        String password;
        String role; // "ADMIN", "TEACHER", "STUDENT"
        String fullName;
        String email;
        String phone;
        String address;
        ImageIcon profilePicture;
        
        User(String username, String password, String role, String fullName) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.fullName = fullName;
            this.profilePicture = new ImageIcon(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB));
        }

        boolean hasPermission(String action) {
            switch(role) {
                case "ADMIN":
                    return true;
                case "TEACHER":
                    return !action.equals("MANAGE_USERS") && !action.equals("MANAGE_FEES");
                case "STUDENT":
                    // Students can only edit their profile and view other tabs
                    if (action.equals("UPDATE_PROFILE")) {
                        return true;
                    }
                    // For all other actions, they have view-only access
                    return false;
                default:
                    return false;
            }
        }
    }

    static {
        // Add users
        users.put("admin", new User("admin", "admin123", "ADMIN", "System Administrator"));
        
        // Add student profiles
        users.put("hiten", new User("hiten", "hiten123", "STUDENT", "Hiten Mandhyan"));
        users.put("naman", new User("naman", "naman123", "STUDENT", "Naman Jiwnani"));
        users.put("vaibhavi", new User("vaibhavi", "vaibhavi123", "STUDENT", "Vaibhavi Sengar"));
        
        // Add teacher profile
        users.put("varuna", new User("varuna", "varuna123", "TEACHER", "Dr. Varuna Gupta"));
        
        // Add sample student data
        Student student1 = new Student("S001", "Hiten Mandhyan", "Computer Science", "hiten@christ.in");
        Student student2 = new Student("S002", "Naman Jiwnani", "Computer Science", "naman@christ.in");
        Student student3 = new Student("S003", "Vaibhavi Sengar", "Computer Science", "vaibhavi@christ.in");
        students.add(student1);
        students.add(student2);
        students.add(student3);
        
        // Add sample faculty
        faculty.add(new Faculty("F001", "Dr. Varuna Gupta", "Computer Science", "Data Science & AI"));
        
        // Add sample courses
        Course course1 = new Course("CS101", "Introduction to Programming", 3, "Dr. Varuna Gupta");
        Course course2 = new Course("CS102", "Data Structures", 4, "Dr. Varuna Gupta");
        Course course3 = new Course("CS103", "Database Management", 3, "Dr. Varuna Gupta");
        courses.add(course1);
        courses.add(course2);
        courses.add(course3);
        
        // Enroll students in courses
        student1.enrolledCourses.add("CS101");
        student1.enrolledCourses.add("CS102");
        student2.enrolledCourses.add("CS101");
        student2.enrolledCourses.add("CS103");
        student3.enrolledCourses.add("CS102");
        student3.enrolledCourses.add("CS103");
        
        // Add sample attendance
        Map<String, List<Attendance>> student1Attendance = new HashMap<>();
        List<Attendance> cs101Attendance = new ArrayList<>();
        cs101Attendance.add(new Attendance(LocalDate.now().minusDays(7), true));
        cs101Attendance.add(new Attendance(LocalDate.now().minusDays(14), true));
        student1Attendance.put("CS101", cs101Attendance);
        attendanceRecords.put("S001", student1Attendance);
        
        // Add sample grades
        Map<String, Grade> student1Grades = new HashMap<>();
        student1Grades.put("CS101", new Grade(85.0, 90.0, 88.0));
        grades.put("S001", student1Grades);

        // Add sample books
        books.add(new Book("B001", "Java Programming", "John Smith", "Programming", 5));
        books.add(new Book("B002", "Data Structures in Java", "Jane Doe", "Computer Science", 3));
        books.add(new Book("B003", "Database Systems", "Robert Johnson", "Computer Science", 4));

        // Add sample timetable entries
        timeTable.add(new TimeTableEntry("CS101", "Monday", "09:00", "10:30", "Room 101"));
        timeTable.add(new TimeTableEntry("CS102", "Tuesday", "11:00", "12:30", "Room 202"));
        timeTable.add(new TimeTableEntry("CS103", "Wednesday", "14:00", "15:30", "Room 303"));

        // Add sample fee records for students
        List<FeeRecord> hitenFees = new ArrayList<>();
        hitenFees.add(new FeeRecord("Tuition Fee - Semester 1", 75000.0, LocalDate.now().plusMonths(1)));
        hitenFees.add(new FeeRecord("Library Fee", 5000.0, LocalDate.now().plusMonths(1)));
        feeRecords.put("S001", hitenFees);

        List<FeeRecord> namanFees = new ArrayList<>();
        namanFees.add(new FeeRecord("Tuition Fee - Semester 1", 75000.0, LocalDate.now().plusMonths(1)));
        namanFees.add(new FeeRecord("Library Fee", 5000.0, LocalDate.now().plusMonths(1)));
        feeRecords.put("S002", namanFees);

        List<FeeRecord> vaibhaviFees = new ArrayList<>();
        vaibhaviFees.add(new FeeRecord("Tuition Fee - Semester 1", 75000.0, LocalDate.now().plusMonths(1)));
        vaibhaviFees.add(new FeeRecord("Library Fee", 5000.0, LocalDate.now().plusMonths(1)));
        feeRecords.put("S003", vaibhaviFees);
    }

    // Helper Methods
    private static JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(REGULAR_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY_COLOR);
        button.setPreferredSize(new Dimension(120, 35));
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(SECONDARY_COLOR);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(PRIMARY_COLOR);
            }
        });

        return button;
    }

    private static JTabbedPane createStyledTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(REGULAR_FONT);
        tabbedPane.setBackground(BACKGROUND_COLOR);
        return tabbedPane;
    }

    private static void showErrorMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }

    private static JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(REGULAR_FONT);
        table.setRowHeight(25);
        table.setIntercellSpacing(new Dimension(10, 5));
        table.setGridColor(new Color(230, 230, 230));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(PRIMARY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(52, 152, 219, 50));
        table.setSelectionForeground(TEXT_COLOR);
        return table;
    }

    private static JScrollPane createStyledScrollPane(JComponent component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    private static JPanel createStyledPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(10, 10, 10, 10),
            BorderFactory.createLineBorder(new Color(230, 230, 230))
        ));
        return panel;
    }

    // Helper method to load and scale the logo
    private static ImageIcon loadAndScaleLogo(int width, int height) {
        try {
            // Load the image file directly from the project root
            File logoFile = new File(LOGO_PATH);
            if (logoFile.exists()) {
                ImageIcon originalIcon = new ImageIcon(logoFile.getAbsolutePath());
                Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            } else {
                System.out.println("Logo file not found at: " + logoFile.getAbsolutePath());
                return createPlaceholderLogo(width, height);
            }
        } catch (Exception e) {
            System.out.println("Error loading logo: " + e.getMessage());
            e.printStackTrace();
            return createPlaceholderLogo(width, height);
        }
    }

    // Helper method to create a placeholder logo
    private static ImageIcon createPlaceholderLogo(int width, int height) {
        BufferedImage placeholder = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = placeholder.createGraphics();
        g2d.setColor(new Color(29, 71, 148)); // Christ University Blue
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, height/4));
        String text = "CHRIST";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, (width - fm.stringWidth(text))/2, height/2);
        g2d.dispose();
        return new ImageIcon(placeholder);
    }

    public static void main(String[] args) {
        try {
            // Set system look and feel
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            
            // Customize Nimbus colors
            UIManager.put("nimbusBase", PRIMARY_COLOR);
            UIManager.put("nimbusBlueGrey", SECONDARY_COLOR);
            UIManager.put("control", BACKGROUND_COLOR);
            UIManager.put("text", TEXT_COLOR);
            UIManager.put("Table.background", Color.WHITE);
            UIManager.put("Table.alternateRowColor", new Color(245, 245, 245));
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create and show login window
        SwingUtilities.invokeLater(() -> createLoginWindow());
    }

    private static void createLoginWindow() {
        mainFrame = new JFrame("Christ University Management System - Login");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(600, 600);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.getContentPane().setBackground(BACKGROUND_COLOR);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Logo
        ImageIcon logoIcon = loadAndScaleLogo(300, 150);
        JLabel logoLabel = new JLabel(logoIcon);
        logoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(logoLabel, gbc);

        // Title with updated styling
        JLabel titleLabel = new JLabel("Christ University Management System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(29, 71, 148)); // Christ University Blue
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 10, 30, 10);
        mainPanel.add(titleLabel, gbc);

        // Reset insets for other components
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.gridwidth = 1;

        // Role selection
        JLabel roleLabel = new JLabel("Login as:");
        roleLabel.setFont(REGULAR_FONT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(roleLabel, gbc);

        String[] roles = {"ADMIN", "TEACHER", "STUDENT"};
        JComboBox<String> roleCombo = new JComboBox<>(roles);
        roleCombo.setFont(REGULAR_FONT);
        roleCombo.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(roleCombo, gbc);

        // Username field
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(REGULAR_FONT);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(usernameLabel, gbc);

        JTextField usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(200, 30));
        usernameField.setFont(REGULAR_FONT);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(usernameField, gbc);

        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(REGULAR_FONT);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(passwordLabel, gbc);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(200, 30));
        passwordField.setFont(REGULAR_FONT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(passwordField, gbc);

        // Login button
        JButton loginButton = new JButton("Login");
        styleButton(loginButton);
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String selectedRole = roleCombo.getSelectedItem().toString().toUpperCase();

            if (validateLogin(username, password, selectedRole)) {
                currentUser = users.get(username.toLowerCase());
                showDashboard();
            } else {
                showErrorMessage(mainFrame, "Invalid credentials or incorrect role selected");
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 10, 10, 10);
        mainPanel.add(loginButton, gbc);

        mainFrame.add(mainPanel);
        mainFrame.setVisible(true);
    }

    private static boolean validateLogin(String username, String password, String role) {
        User user = users.get(username.toLowerCase()); // Make username case-insensitive
        return user != null && 
               user.password.equals(password) && 
               user.role.equals(role);
    }

    private static void showDashboard() {
        mainFrame.dispose();
        JFrame dashboard = new JFrame("Christ University Management System - Dashboard");
        dashboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        dashboard.setSize(1200, 800);
        dashboard.setLocationRelativeTo(null);
        dashboard.getContentPane().setBackground(BACKGROUND_COLOR);

        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BACKGROUND_COLOR);

        // Header panel with logo and title
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add logo to header
        ImageIcon logoIcon = loadAndScaleLogo(100, 50);
        JLabel logoLabel = new JLabel(logoIcon);
        logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        headerPanel.add(logoLabel);

        // Add title to header with updated styling
        JLabel titleLabel = new JLabel("Christ University Management System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(29, 71, 148));
        headerPanel.add(titleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Create and add tabbed pane
        mainTabbedPane = createStyledTabbedPane();
        
        // Add My Details tab first for all users
        mainTabbedPane.addTab("My Details", null, createMyDetailsPanel(), "View Your Details");

        // Add other tabs based on role
        if (currentUser.role.equals("ADMIN")) {
            addAdminTabs();
        } else if (currentUser.role.equals("TEACHER")) {
            addTeacherTabs();
        } else { // STUDENT
            addStudentTabs();
        }
        
        // Add Edit Profile tab at the end
        mainTabbedPane.addTab("Edit Profile", null, createProfileEditPanel(), "Edit Your Profile");

        // Set tab titles with emojis
        setTabTitles(mainTabbedPane);

        mainPanel.add(mainTabbedPane, BorderLayout.CENTER);
        dashboard.add(mainPanel);
        dashboard.setVisible(true);
    }

    private static void addAdminTabs() {
        mainTabbedPane.addTab("Students", null, createStudentPanel(), "Manage Students");
        mainTabbedPane.addTab("Faculty", null, createFacultyPanel(), "Manage Faculty");
        mainTabbedPane.addTab("Courses", null, createCoursePanel(), "Manage Courses");
        mainTabbedPane.addTab("Attendance", null, createAttendancePanel(), "Manage Attendance");
        mainTabbedPane.addTab("Grades", null, createGradesPanel(), "Manage Grades");
        mainTabbedPane.addTab("Library", null, createLibraryPanel(), "Manage Library");
        mainTabbedPane.addTab("Fees", null, createFeesPanel(), "Manage Fees");
        mainTabbedPane.addTab("Timetable", null, createTimetablePanel(), "View Timetable");
    }

    private static void addTeacherTabs() {
        mainTabbedPane.addTab("Courses", null, createCoursePanel(), "View Courses");
        mainTabbedPane.addTab("Attendance", null, createAttendancePanel(), "Manage Attendance");
        mainTabbedPane.addTab("Grades", null, createGradesPanel(), "Manage Grades");
        mainTabbedPane.addTab("Library", null, createLibraryPanel(), "Access Library");
    }

    private static void addStudentTabs() {
        // Create read-only versions of panels for students
        mainTabbedPane.addTab("Courses", null, createReadOnlyPanel(createCoursePanel()), "View Courses");
        mainTabbedPane.addTab("Attendance", null, createReadOnlyPanel(createAttendancePanel()), "View Attendance");
        mainTabbedPane.addTab("Grades", null, createReadOnlyPanel(createGradesPanel()), "View Grades");
        mainTabbedPane.addTab("Library", null, createReadOnlyPanel(createLibraryPanel()), "Access Library");
        mainTabbedPane.addTab("Fees", null, createReadOnlyPanel(createFeesPanel()), "View Fees");
        mainTabbedPane.addTab("Timetable", null, createReadOnlyPanel(createTimetablePanel()), "View Timetable");
    }

    private static JPanel createReadOnlyPanel(JPanel originalPanel) {
        // Make all components in the panel non-editable
        makeComponentsReadOnly(originalPanel);
        return originalPanel;
    }

    private static void makeComponentsReadOnly(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(false);
            } else if (comp instanceof JTextField) {
                ((JTextField) comp).setEditable(false);
            } else if (comp instanceof JTextArea) {
                ((JTextArea) comp).setEditable(false);
            } else if (comp instanceof JComboBox) {
                ((JComboBox<?>) comp).setEnabled(false);
            } else if (comp instanceof Container) {
                makeComponentsReadOnly((Container) comp);
            }
        }
    }

    private static void setTabTitles(JTabbedPane tabbedPane) {
        Map<String, String> tabEmojis = new HashMap<>();
        tabEmojis.put("My Details", "👤");
        tabEmojis.put("Students", "👥");
        tabEmojis.put("Faculty", "👨‍🏫");
        tabEmojis.put("Courses", "📚");
        tabEmojis.put("Attendance", "📋");
        tabEmojis.put("Grades", "📊");
        tabEmojis.put("Library", "📖");
        tabEmojis.put("Fees", "💰");
        tabEmojis.put("Timetable", "🕒");
        tabEmojis.put("Edit Profile", "✏️");

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String title = tabbedPane.getTitleAt(i);
            String emoji = tabEmojis.getOrDefault(title, "");
            tabbedPane.setTitleAt(i, emoji + " " + title);
        }
    }

    private static JPanel createMyDetailsPanel() {
        JPanel panel = createStyledPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Profile Picture Section
        JPanel picturePanel = new JPanel(new BorderLayout());
        picturePanel.setBackground(Color.WHITE);
        
        JLabel pictureLabel = new JLabel();
        pictureLabel.setPreferredSize(new Dimension(150, 150));
        if (currentUser.profilePicture != null) {
            Image img = currentUser.profilePicture.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            pictureLabel.setIcon(new ImageIcon(img));
        }
        pictureLabel.setBorder(BorderFactory.createLineBorder(SECONDARY_COLOR));
        
        picturePanel.add(pictureLabel, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 4;
        panel.add(picturePanel, gbc);

        // Reset gridheight for other components
        gbc.gridheight = 1;
        gbc.gridx = 1;

        // Display user information (non-editable)
        addDetailField(panel, "Username:", currentUser.username, gbc, 0);
        addDetailField(panel, "Role:", currentUser.role, gbc, 1);
        addDetailField(panel, "Full Name:", currentUser.fullName, gbc, 2);
        addDetailField(panel, "Email:", currentUser.email != null ? currentUser.email : "Not set", gbc, 3);
        addDetailField(panel, "Phone:", currentUser.phone != null ? currentUser.phone : "Not set", gbc, 4);
        addDetailField(panel, "Address:", currentUser.address != null ? currentUser.address : "Not set", gbc, 5);

        return panel;
    }

    private static void addDetailField(JPanel panel, String labelText, String value, 
            GridBagConstraints gbc, int row) {
        JLabel label = new JLabel(labelText);
        label.setFont(REGULAR_FONT);
        label.setForeground(TEXT_COLOR);
        
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(label, gbc);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(REGULAR_FONT);
        valueLabel.setForeground(PRIMARY_COLOR);
        
        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 20, 10, 10);
        panel.add(valueLabel, gbc);
        gbc.insets = new Insets(10, 10, 10, 10);
    }

    private static JPanel createProfileEditPanel() {
        JPanel panel = createStyledPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Profile Picture Section
        JPanel picturePanel = new JPanel(new BorderLayout());
        picturePanel.setBackground(Color.WHITE);
        
        JLabel pictureLabel = new JLabel();
        pictureLabel.setPreferredSize(new Dimension(150, 150));
        if (currentUser.profilePicture != null) {
            Image img = currentUser.profilePicture.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            pictureLabel.setIcon(new ImageIcon(img));
        }
        pictureLabel.setBorder(BorderFactory.createLineBorder(SECONDARY_COLOR));
        
        JButton uploadButton = createStyledButton("Upload Picture");
        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif"));
            
            if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                try {
                    File selectedFile = fileChooser.getSelectedFile();
                    BufferedImage img = ImageIO.read(selectedFile);
                    Image scaledImg = img.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                    currentUser.profilePicture = new ImageIcon(scaledImg);
                    pictureLabel.setIcon(currentUser.profilePicture);
                } catch (Exception ex) {
                    showErrorMessage(panel, "Error loading image");
                }
            }
        });

        picturePanel.add(pictureLabel, BorderLayout.CENTER);
        picturePanel.add(uploadButton, BorderLayout.SOUTH);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 4;
        panel.add(picturePanel, gbc);

        // Profile Information Section
        gbc.gridheight = 1;
        gbc.gridx = 1;
        
        // Username and Role (non-editable)
        addProfileField(panel, "Username:", currentUser.username, false, gbc, 0);
        addProfileField(panel, "Role:", currentUser.role, false, gbc, 1);
        
        // Editable fields
        JTextField nameField = addProfileField(panel, "Full Name:", currentUser.fullName, true, gbc, 2);
        JTextField emailField = addProfileField(panel, "Email:", currentUser.email, true, gbc, 3);
        JTextField phoneField = addProfileField(panel, "Phone:", currentUser.phone, true, gbc, 4);
        JTextField addressField = addProfileField(panel, "Address:", currentUser.address, true, gbc, 5);

        // Save Button
        JButton saveButton = createStyledButton("Save Changes");
        saveButton.addActionListener(e -> {
            currentUser.fullName = nameField.getText();
            currentUser.email = emailField.getText();
            currentUser.phone = phoneField.getText();
            currentUser.address = addressField.getText();
            
            // Refresh the My Details panel
            refreshMyDetailsPanel();
            
            JOptionPane.showMessageDialog(panel, "Profile updated successfully!");
        });

        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 5, 5, 5);
        panel.add(saveButton, gbc);

        return panel;
    }

    private static JTextField addProfileField(JPanel panel, String label, String value, 
            boolean editable, GridBagConstraints gbc, int row) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(REGULAR_FONT);
        fieldLabel.setForeground(TEXT_COLOR);
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(fieldLabel, gbc);

        JTextField field = new JTextField(value != null ? value : "");
        field.setPreferredSize(new Dimension(200, 30));
        field.setFont(REGULAR_FONT);
        field.setEditable(editable);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);

        return field;
    }

    private static void styleButton(JButton button) {
        button.setFont(REGULAR_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY_COLOR);
        button.setPreferredSize(new Dimension(120, 35));
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(SECONDARY_COLOR);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(PRIMARY_COLOR);
            }
        });
    }

    private static void refreshMyDetailsPanel() {
        // Find and update the My Details tab
        int myDetailsIndex = 0; // My Details is always the first tab
        mainTabbedPane.setComponentAt(myDetailsIndex, createMyDetailsPanel());
        mainTabbedPane.revalidate();
        mainTabbedPane.repaint();
    }

    private static JPanel createStudentPanel() {
        JPanel panel = createStyledPanel();
        
        // Create table with styled components
        String[] columns = {"ID", "Name", "Department", "Email"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Add existing students to table
        for (Student student : students) {
            model.addRow(student.toTableRow());
        }

        JTable table = createStyledTable(model);
        JScrollPane scrollPane = createStyledScrollPane(table);

        // Form Panel for adding/editing students
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField idField = new JTextField(20);
        JTextField nameField = new JTextField(20);
        JTextField deptField = new JTextField(20);
        JTextField emailField = new JTextField(20);

        addFormField(formPanel, "Student ID:", idField, gbc, 0);
        addFormField(formPanel, "Name:", nameField, gbc, 1);
        addFormField(formPanel, "Department:", deptField, gbc, 2);
        addFormField(formPanel, "Email:", emailField, gbc, 3);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setBackground(Color.WHITE);

        JButton addButton = createStyledButton("Add Student");
        JButton editButton = createStyledButton("Edit");
        JButton deleteButton = createStyledButton("Delete");
        JButton saveButton = createStyledButton("Save Changes");

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(saveButton);

        // Add button listeners
        addButton.addActionListener(e -> {
            String id = idField.getText();
            String name = nameField.getText();
            String dept = deptField.getText();
            String email = emailField.getText();

            if (id.isEmpty() || name.isEmpty() || dept.isEmpty() || email.isEmpty()) {
                showErrorMessage(panel, "All fields are required");
                return;
            }

            Student newStudent = new Student(id, name, dept, email);
            students.add(newStudent);
            model.addRow(newStudent.toTableRow());
            clearFields(idField, nameField, deptField, emailField);
        });

        editButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                showErrorMessage(panel, "Please select a student to edit");
                return;
            }

            idField.setText((String) model.getValueAt(selectedRow, 0));
            nameField.setText((String) model.getValueAt(selectedRow, 1));
            deptField.setText((String) model.getValueAt(selectedRow, 2));
            emailField.setText((String) model.getValueAt(selectedRow, 3));
        });

        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                showErrorMessage(panel, "Please select a student to delete");
                return;
            }

            String studentId = (String) model.getValueAt(selectedRow, 0);
            students.removeIf(s -> s.id.equals(studentId));
            model.removeRow(selectedRow);
        });

        saveButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                showErrorMessage(panel, "Please select a student to update");
                return;
            }

            String id = idField.getText();
            String name = nameField.getText();
            String dept = deptField.getText();
            String email = emailField.getText();

            if (id.isEmpty() || name.isEmpty() || dept.isEmpty() || email.isEmpty()) {
                showErrorMessage(panel, "All fields are required");
                return;
            }

            // Update model and students list
            model.setValueAt(id, selectedRow, 0);
            model.setValueAt(name, selectedRow, 1);
            model.setValueAt(dept, selectedRow, 2);
            model.setValueAt(email, selectedRow, 3);

            // Update student in the list
            String originalId = (String) model.getValueAt(selectedRow, 0);
            for (Student student : students) {
                if (student.id.equals(originalId)) {
                    student.name = name;
                    student.department = dept;
                    student.email = email;
                    break;
                }
            }

            clearFields(idField, nameField, deptField, emailField);
            JOptionPane.showMessageDialog(panel, "Student information updated successfully!");
        });

        // Layout
        panel.setLayout(new BorderLayout(10, 10));
        panel.add(new JLabel("Student Management", SwingConstants.CENTER), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBackground(Color.WHITE);
        southPanel.add(formPanel, BorderLayout.CENTER);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }

    private static void clearFields(JTextField... fields) {
        for (JTextField field : fields) {
            field.setText("");
        }
    }

    private static void addFormField(JPanel panel, String labelText, JComponent field, 
            GridBagConstraints gbc, int row) {
        JLabel label = new JLabel(labelText);
        label.setFont(REGULAR_FONT);
        label.setForeground(TEXT_COLOR);
        
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
    }

    private static JPanel createFacultyPanel() {
        JPanel panel = createStyledPanel();
        
        // Create table with styled components
        String[] columns = {"ID", "Name", "Department", "Specialization"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Add existing faculty to table
        for (Faculty f : faculty) {
            model.addRow(f.toTableRow());
        }

        JTable table = createStyledTable(model);
        JScrollPane scrollPane = createStyledScrollPane(table);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(createStyledButton("Add Faculty"));
        buttonPanel.add(createStyledButton("Edit"));
        buttonPanel.add(createStyledButton("Delete"));

        // Add components to panel
        panel.add(new JLabel("Faculty Management", SwingConstants.CENTER), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private static JPanel createCoursePanel() {
        JPanel panel = createStyledPanel();
        
        // Create table with styled components
        String[] columns = {"Code", "Name", "Credits", "Instructor"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Add existing courses to table
        for (Course course : courses) {
            model.addRow(course.toTableRow());
        }

        JTable table = createStyledTable(model);
        JScrollPane scrollPane = createStyledScrollPane(table);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(createStyledButton("Add Course"));
        buttonPanel.add(createStyledButton("Edit"));
        buttonPanel.add(createStyledButton("Delete"));

        // Add components to panel
        panel.add(new JLabel("Course Management", SwingConstants.CENTER), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private static JPanel createAttendancePanel() {
        JPanel panel = createStyledPanel();
        
        // Top panel for selection
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> studentCombo = new JComboBox<>();
        JComboBox<String> courseCombo = new JComboBox<>();
        
        // Add items to combos
        studentCombo.addItem("Select Student");
        for (Student s : students) {
            studentCombo.addItem(s.id + " - " + s.name);
        }
        
        courseCombo.addItem("Select Course");
        for (Course c : courses) {
            courseCombo.addItem(c.code + " - " + c.name);
        }
        
        selectionPanel.add(new JLabel("Student:"));
        selectionPanel.add(studentCombo);
        selectionPanel.add(new JLabel("Course:"));
        selectionPanel.add(courseCombo);
        
        // Create table
        String[] columns = {"Date", "Status"};
        DefaultTableModel modelTable = new DefaultTableModel(columns, 0);
        JTable table = createStyledTable(modelTable);
        JScrollPane scrollPane = createStyledScrollPane(table);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton markAttendanceBtn = createStyledButton("Mark Attendance");
        buttonPanel.add(markAttendanceBtn);
        
        // Add listeners
        studentCombo.addActionListener(e -> updateAttendanceTable(modelTable, studentCombo, courseCombo));
        courseCombo.addActionListener(e -> updateAttendanceTable(modelTable, studentCombo, courseCombo));
        
        markAttendanceBtn.addActionListener(e -> {
            String selectedStudent = (String) studentCombo.getSelectedItem();
            String selectedCourse = (String) courseCombo.getSelectedItem();
            
            if ("Select Student".equals(selectedStudent) || "Select Course".equals(selectedCourse)) {
                showErrorMessage(panel, "Please select both student and course");
                return;
            }
            
            String studentId = selectedStudent.split(" - ")[0];
            String courseCode = selectedCourse.split(" - ")[0];
            
            Object[] options = {"Present", "Absent"};
            int choice = JOptionPane.showOptionDialog(panel,
                "Mark attendance for " + selectedStudent + "\nin " + selectedCourse,
                "Mark Attendance",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
                
            if (choice != JOptionPane.CLOSED_OPTION) {
                boolean present = (choice == JOptionPane.YES_OPTION);
                addAttendance(studentId, courseCode, present);
                updateAttendanceTable(modelTable, studentCombo, courseCombo);
            }
        });
        
        panel.add(selectionPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private static void updateAttendanceTable(DefaultTableModel model, JComboBox<String> studentCombo, JComboBox<String> courseCombo) {
        model.setRowCount(0);
        
        String selectedStudent = (String) studentCombo.getSelectedItem();
        String selectedCourse = (String) courseCombo.getSelectedItem();
        
        if ("Select Student".equals(selectedStudent) || "Select Course".equals(selectedCourse)) {
            return;
        }
        
        String studentId = selectedStudent.split(" - ")[0];
        String courseCode = selectedCourse.split(" - ")[0];
        
        Map<String, List<Attendance>> studentAttendance = attendanceRecords.get(studentId);
        if (studentAttendance != null) {
            List<Attendance> courseAttendance = studentAttendance.get(courseCode);
            if (courseAttendance != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (Attendance a : courseAttendance) {
                    model.addRow(new Object[]{
                        a.date.format(formatter),
                        a.present ? "Present" : "Absent"
                    });
                }
            }
        }
    }
    
    private static void addAttendance(String studentId, String courseCode, boolean present) {
        attendanceRecords.computeIfAbsent(studentId, k -> new HashMap<>())
            .computeIfAbsent(courseCode, k -> new ArrayList<>())
            .add(new Attendance(LocalDate.now(), present));
    }
    
    private static JPanel createGradesPanel() {
        JPanel panel = createStyledPanel();
        
        // Top panel for selection
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> studentCombo = new JComboBox<>();
        JComboBox<String> courseCombo = new JComboBox<>();
        
        // Add items to combos
        studentCombo.addItem("Select Student");
        for (Student s : students) {
            studentCombo.addItem(s.id + " - " + s.name);
        }
        
        courseCombo.addItem("Select Course");
        for (Course c : courses) {
            courseCombo.addItem(c.code + " - " + c.name);
        }
        
        selectionPanel.add(new JLabel("Student:"));
        selectionPanel.add(studentCombo);
        selectionPanel.add(new JLabel("Course:"));
        selectionPanel.add(courseCombo);
        
        // Create table
        String[] columns = {"Component", "Score"};
        DefaultTableModel modelTable = new DefaultTableModel(columns, 0);
        JTable table = createStyledTable(modelTable);
        JScrollPane scrollPane = createStyledScrollPane(table);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addGradesBtn = createStyledButton("Add/Update Grades");
        buttonPanel.add(addGradesBtn);
        
        // Add listeners
        studentCombo.addActionListener(e -> updateGradesTable(modelTable, studentCombo, courseCombo));
        courseCombo.addActionListener(e -> updateGradesTable(modelTable, studentCombo, courseCombo));
        
        addGradesBtn.addActionListener(e -> {
            String selectedStudent = (String) studentCombo.getSelectedItem();
            String selectedCourse = (String) courseCombo.getSelectedItem();
            
            if ("Select Student".equals(selectedStudent) || "Select Course".equals(selectedCourse)) {
                showErrorMessage(panel, "Please select both student and course");
                return;
            }
            
            String studentId = selectedStudent.split(" - ")[0];
            String courseCode = selectedCourse.split(" - ")[0];
            
            JTextField midtermField = new JTextField(5);
            JTextField finalField = new JTextField(5);
            JTextField assignmentsField = new JTextField(5);
            
            Object[] fields = {
                "Midterm (30%):", midtermField,
                "Final Exam (50%):", finalField,
                "Assignments (20%):", assignmentsField
            };
            
            int result = JOptionPane.showConfirmDialog(panel, fields,
                "Enter Grades for " + selectedStudent,
                JOptionPane.OK_CANCEL_OPTION);
                
            if (result == JOptionPane.OK_OPTION) {
                try {
                    double midterm = Double.parseDouble(midtermField.getText());
                    double finalExam = Double.parseDouble(finalField.getText());
                    double assignments = Double.parseDouble(assignmentsField.getText());
                    
                    if (midterm < 0 || midterm > 100 || finalExam < 0 || finalExam > 100 ||
                        assignments < 0 || assignments > 100) {
                        throw new NumberFormatException();
                    }
                    
                    addGrades(studentId, courseCode, midterm, finalExam, assignments);
                    updateGradesTable(modelTable, studentCombo, courseCombo);
                } catch (NumberFormatException ex) {
                    showErrorMessage(panel, "Please enter valid grades (0-100)");
                }
            }
        });
        
        panel.add(selectionPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private static void updateGradesTable(DefaultTableModel model, JComboBox<String> studentCombo, JComboBox<String> courseCombo) {
        model.setRowCount(0);
        
        String selectedStudent = (String) studentCombo.getSelectedItem();
        String selectedCourse = (String) courseCombo.getSelectedItem();
        
        if ("Select Student".equals(selectedStudent) || "Select Course".equals(selectedCourse)) {
            return;
        }
        
        String studentId = selectedStudent.split(" - ")[0];
        String courseCode = selectedCourse.split(" - ")[0];
        
        Map<String, Grade> studentGrades = grades.get(studentId);
        if (studentGrades != null) {
            Grade grade = studentGrades.get(courseCode);
            if (grade != null) {
                model.addRow(new Object[]{"Midterm (30%)", String.format("%.2f", grade.midterm)});
                model.addRow(new Object[]{"Final Exam (50%)", String.format("%.2f", grade.finalExam)});
                model.addRow(new Object[]{"Assignments (20%)", String.format("%.2f", grade.assignments)});
                model.addRow(new Object[]{"Total Grade", String.format("%.2f", grade.totalGrade)});
            }
        }
    }
    
    private static void addGrades(String studentId, String courseCode, double midterm, double finalExam, double assignments) {
        grades.computeIfAbsent(studentId, k -> new HashMap<>())
            .put(courseCode, new Grade(midterm, finalExam, assignments));
    }

    private static JPanel createLibraryPanel() {
        JPanel panel = createStyledPanel();

        // Create tabbed pane for library management
        JTabbedPane libraryTabs = createStyledTabbedPane();
        libraryTabs.addTab("Book Catalog", null, createBookCatalogPanel(), "Manage Books");
        libraryTabs.addTab("Issue/Return", null, createBookIssuePanel(), "Issue or Return Books");

        // Set tab titles with emojis
        libraryTabs.setTitleAt(0, "📖 Book Catalog");
        libraryTabs.setTitleAt(1, "📋 Issue/Return");

        panel.add(libraryTabs, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel createBookCatalogPanel() {
        JPanel panel = createStyledPanel();
        
        // Create table with styled components
        String[] columns = {"ID", "Title", "Author", "Category", "Copies (Available/Total)"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Add existing books to table
        for (Book book : books) {
            model.addRow(book.toTableRow());
        }

        JTable table = createStyledTable(model);
        JScrollPane scrollPane = createStyledScrollPane(table);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = createStyledButton("Add Book");
        buttonPanel.add(addButton);

        // Add button listener
        addButton.addActionListener(e -> {
            JTextField idField = new JTextField();
            JTextField titleField = new JTextField();
            JTextField authorField = new JTextField();
            JTextField categoryField = new JTextField();
            JTextField copiesField = new JTextField();

            Object[] fields = {
                "Book ID:", idField,
                "Title:", titleField,
                "Author:", authorField,
                "Category:", categoryField,
                "Total Copies:", copiesField
            };

            int result = JOptionPane.showConfirmDialog(panel, fields, "Add Book", 
                JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                try {
                    Book newBook = new Book(
                        idField.getText(),
                        titleField.getText(),
                        authorField.getText(),
                        categoryField.getText(),
                        Integer.parseInt(copiesField.getText())
                    );
                    books.add(newBook);
                    model.addRow(newBook.toTableRow());
                } catch (NumberFormatException ex) {
                    showErrorMessage(panel, "Please enter a valid number for copies");
                }
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel createBookIssuePanel() {
        JPanel panel = createStyledPanel();
        
        // Selection panel
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> studentCombo = new JComboBox<>();
        
        studentCombo.addItem("Select Student");
        for (Student s : students) {
            studentCombo.addItem(s.id + " - " + s.name);
        }

        selectionPanel.add(new JLabel("Student:"));
        selectionPanel.add(studentCombo);

        // Create table
        String[] columns = {"Book ID", "Issue Date", "Due Date", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = createStyledTable(model);
        JScrollPane scrollPane = createStyledScrollPane(table);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton issueButton = createStyledButton("Issue Book");
        JButton returnButton = createStyledButton("Return Book");
        buttonPanel.add(issueButton);
        buttonPanel.add(returnButton);

        // Add listeners
        studentCombo.addActionListener(e -> updateIssuedBooksTable(model, studentCombo));

        issueButton.addActionListener(e -> {
            String selectedStudent = (String) studentCombo.getSelectedItem();
            if ("Select Student".equals(selectedStudent)) {
                showErrorMessage(panel, "Please select a student");
                return;
            }

            String studentId = selectedStudent.split(" - ")[0];
            
            // Show book selection dialog
            JComboBox<String> bookCombo = new JComboBox<>();
            for (Book book : books) {
                if (book.availableCopies > 0) {
                    bookCombo.addItem(book.id + " - " + book.title);
                }
            }

            if (bookCombo.getItemCount() == 0) {
                showErrorMessage(panel, "No books available for issue");
                return;
            }

            Object[] fields = {
                "Select Book:", bookCombo
            };

            int result = JOptionPane.showConfirmDialog(panel, fields, "Issue Book", 
                JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                String bookId = ((String) bookCombo.getSelectedItem()).split(" - ")[0];
                issueBook(studentId, bookId);
                updateIssuedBooksTable(model, studentCombo);
                
                // Update available copies
                for (Book book : books) {
                    if (book.id.equals(bookId)) {
                        book.availableCopies--;
                        break;
                    }
                }
            }
        });

        returnButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                showErrorMessage(panel, "Please select a book to return");
                return;
            }

            String selectedStudent = (String) studentCombo.getSelectedItem();
            String studentId = selectedStudent.split(" - ")[0];
            String bookId = (String) model.getValueAt(selectedRow, 0);

            returnBook(studentId, bookId);
            updateIssuedBooksTable(model, studentCombo);

            // Update available copies
            for (Book book : books) {
                if (book.id.equals(bookId)) {
                    book.availableCopies++;
                    break;
                }
            }
        });

        panel.add(selectionPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private static void updateIssuedBooksTable(DefaultTableModel model, JComboBox<String> studentCombo) {
        model.setRowCount(0);
        
        String selectedStudent = (String) studentCombo.getSelectedItem();
        if ("Select Student".equals(selectedStudent)) {
            return;
        }

        String studentId = selectedStudent.split(" - ")[0];
        List<BookIssue> issues = bookIssues.get(studentId);
        if (issues != null) {
            for (BookIssue issue : issues) {
                model.addRow(issue.toTableRow());
            }
        }
    }

    private static void issueBook(String studentId, String bookId) {
        BookIssue issue = new BookIssue(bookId, LocalDate.now());
        bookIssues.computeIfAbsent(studentId, k -> new ArrayList<>()).add(issue);
    }

    private static void returnBook(String studentId, String bookId) {
        List<BookIssue> issues = bookIssues.get(studentId);
        if (issues != null) {
            for (BookIssue issue : issues) {
                if (issue.bookId.equals(bookId) && !issue.returned) {
                    issue.returned = true;
                    break;
                }
            }
        }
    }

    private static JPanel createFeesPanel() {
        JPanel panel = createStyledPanel();
        
        // Selection panel
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> studentCombo = new JComboBox<>();
        
        studentCombo.addItem("Select Student");
        for (Student s : students) {
            studentCombo.addItem(s.id + " - " + s.name);
        }

        selectionPanel.add(new JLabel("Student:"));
        selectionPanel.add(studentCombo);

        // Create table
        String[] columns = {"Description", "Amount", "Due Date", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = createStyledTable(model);
        JScrollPane scrollPane = createStyledScrollPane(table);

        // Summary panel
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel totalLabel = new JLabel("Total Due: $0.00");
        summaryPanel.add(totalLabel);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addFeeButton = createStyledButton("Add Fee");
        JButton markPaidButton = createStyledButton("Mark as Paid");
        buttonPanel.add(addFeeButton);
        buttonPanel.add(markPaidButton);

        // Add listeners
        studentCombo.addActionListener(e -> {
            updateFeesTable(model, studentCombo);
            updateTotalDue(totalLabel, studentCombo);
        });

        addFeeButton.addActionListener(e -> {
            String selectedStudent = (String) studentCombo.getSelectedItem();
            if ("Select Student".equals(selectedStudent)) {
                showErrorMessage(panel, "Please select a student");
                return;
            }

            String studentId = selectedStudent.split(" - ")[0];

            JTextField descField = new JTextField();
            JTextField amountField = new JTextField();
            JTextField dueDateField = new JTextField();

            Object[] fields = {
                "Description:", descField,
                "Amount ($):", amountField,
                "Due Date (YYYY-MM-DD):", dueDateField
            };

            int result = JOptionPane.showConfirmDialog(panel, fields, "Add Fee", 
                JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                try {
                    FeeRecord newFee = new FeeRecord(
                        descField.getText(),
                        Double.parseDouble(amountField.getText()),
                        LocalDate.parse(dueDateField.getText())
                    );
                    feeRecords.computeIfAbsent(studentId, k -> new ArrayList<>()).add(newFee);
                    updateFeesTable(model, studentCombo);
                    updateTotalDue(totalLabel, studentCombo);
                } catch (Exception ex) {
                    showErrorMessage(panel, "Please enter valid values. Use YYYY-MM-DD format for date.");
                }
            }
        });

        markPaidButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                showErrorMessage(panel, "Please select a fee record");
                return;
            }

            String selectedStudent = (String) studentCombo.getSelectedItem();
            String studentId = selectedStudent.split(" - ")[0];
            List<FeeRecord> fees = feeRecords.get(studentId);
            if (fees != null && selectedRow < fees.size()) {
                fees.get(selectedRow).paid = true;
                updateFeesTable(model, studentCombo);
                updateTotalDue(totalLabel, studentCombo);
            }
        });

        panel.add(selectionPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(summaryPanel, BorderLayout.EAST);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private static void updateFeesTable(DefaultTableModel model, JComboBox<String> studentCombo) {
        model.setRowCount(0);
        
        String selectedStudent = (String) studentCombo.getSelectedItem();
        if ("Select Student".equals(selectedStudent)) {
            return;
        }

        String studentId = selectedStudent.split(" - ")[0];
        List<FeeRecord> fees = feeRecords.get(studentId);
        if (fees != null) {
            for (FeeRecord fee : fees) {
                model.addRow(fee.toTableRow());
            }
        }
    }

    private static void updateTotalDue(JLabel totalLabel, JComboBox<String> studentCombo) {
        double totalDue = 0.0;
        
        String selectedStudent = (String) studentCombo.getSelectedItem();
        if (!"Select Student".equals(selectedStudent)) {
            String studentId = selectedStudent.split(" - ")[0];
            List<FeeRecord> fees = feeRecords.get(studentId);
            if (fees != null) {
                for (FeeRecord fee : fees) {
                    if (!fee.paid) {
                        totalDue += fee.amount;
                    }
                }
            }
        }
        
        totalLabel.setText(String.format("Total Due: $%.2f", totalDue));
    }

    private static JPanel createTimetablePanel() {
        JPanel panel = createStyledPanel();
        
        // Create table with styled components
        String[] columns = {"Course", "Day", "Start Time", "End Time", "Room"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Add existing timetable entries
        for (TimeTableEntry entry : timeTable) {
            model.addRow(entry.toTableRow());
        }

        JTable table = createStyledTable(model);
        JScrollPane scrollPane = createStyledScrollPane(table);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = createStyledButton("Add Schedule");
        buttonPanel.add(addButton);

        // Add button listener
        addButton.addActionListener(e -> {
            JComboBox<String> courseCombo = new JComboBox<>();
            JComboBox<String> dayCombo = new JComboBox<>(new String[]{
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
            });
            JTextField startTimeField = new JTextField();
            JTextField endTimeField = new JTextField();
            JTextField roomField = new JTextField();

            for (Course c : courses) {
                courseCombo.addItem(c.code + " - " + c.name);
            }

            Object[] fields = {
                "Course:", courseCombo,
                "Day:", dayCombo,
                "Start Time (HH:mm):", startTimeField,
                "End Time (HH:mm):", endTimeField,
                "Room:", roomField
            };

            int result = JOptionPane.showConfirmDialog(panel, fields, "Add Schedule", 
                JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                String courseCode = ((String) courseCombo.getSelectedItem()).split(" - ")[0];
                TimeTableEntry newEntry = new TimeTableEntry(
                    courseCode,
                    (String) dayCombo.getSelectedItem(),
                    startTimeField.getText(),
                    endTimeField.getText(),
                    roomField.getText()
                );
                timeTable.add(newEntry);
                model.addRow(newEntry.toTableRow());
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }
} 