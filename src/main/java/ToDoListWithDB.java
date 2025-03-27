import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class ToDoListWithDB {
    private DefaultListModel<String> taskModel;
    private JList<String> taskList;
    private JTextField taskInput, dateInput;
    private Connection conn;

    public ToDoListWithDB() {
        // 1. Conectare la baza de date
        connectToDatabase();

        // 2. Creare UI
        JFrame frame = new JFrame("To-Do List cu Bază de Date");
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        taskModel = new DefaultListModel<>();
        taskList = new JList<>(taskModel);
        loadTasksFromDB(null);

        JScrollPane scrollPane = new JScrollPane(taskList);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(2, 2));

        taskInput = new JTextField();
        dateInput = new JTextField("YYYY-MM-DD");
        JButton addButton = new JButton("➕ Adaugă");
        JButton deleteButton = new JButton("🗑️ Șterge");
        JButton searchButton = new JButton("🔍 Caută după dată");
        JButton editButton = new JButton("✏️ Editează");

        inputPanel.add(new JLabel("Sarcină:"));
        inputPanel.add(taskInput);
        inputPanel.add(new JLabel("Dată (YYYY-MM-DD):"));
        inputPanel.add(dateInput);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(editButton);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // 3. Funcționalitate butoane
        addButton.addActionListener(e -> addTaskToDB());
        deleteButton.addActionListener(e -> deleteTaskFromDB());
        searchButton.addActionListener(e -> loadTasksFromDB(dateInput.getText()));
        editButton.addActionListener(e -> editTaskInDB());

        // Când selectezi o sarcină, o pune în câmpurile de editare
        taskList.addListSelectionListener(e -> fillFieldsForEditing());

        frame.setVisible(true);
    }

    // Conectarea la SQLite și crearea tabelului
    private void connectToDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:C:/Users/HP/Desktop/ToDoList/DataBase.db");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, task TEXT, date TEXT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Încărcarea sarcinilor din DB (filtrare opțională după dată)
    private void loadTasksFromDB(String dateFilter) {
        taskModel.clear();
        try {
            PreparedStatement pstmt;
            if (dateFilter == null || dateFilter.isEmpty()) {
                pstmt = conn.prepareStatement("SELECT id, task, date FROM tasks");
            } else {
                pstmt = conn.prepareStatement("SELECT id, task, date FROM tasks WHERE date = ?");
                pstmt.setString(1, dateFilter);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                taskModel.addElement(rs.getInt("id") + " | " + rs.getString("task") + " 📅 " + rs.getString("date"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Adăugarea unei sarcini în baza de date
    private void addTaskToDB() {
        String task = taskInput.getText();
        String date = dateInput.getText();
        if (!task.isEmpty() && !date.isEmpty()) {
            try {
                PreparedStatement pstmt = conn.prepareStatement("INSERT INTO tasks (task, date) VALUES (?, ?)");
                pstmt.setString(1, task);
                pstmt.setString(2, date);
                pstmt.executeUpdate();
                loadTasksFromDB(null);
                taskInput.setText("");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Ștergerea unei sarcini din baza de date
    private void deleteTaskFromDB() {
        int selectedIndex = taskList.getSelectedIndex();
        if (selectedIndex != -1) {
            String selectedTask = taskModel.getElementAt(selectedIndex);
            int taskId = extractTaskId(selectedTask);
            try {
                PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tasks WHERE id = ?");
                pstmt.setInt(1, taskId);
                pstmt.executeUpdate();
                loadTasksFromDB(null);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Editarea unei sarcini
    private void editTaskInDB() {
        int selectedIndex = taskList.getSelectedIndex();
        if (selectedIndex != -1) {
            String selectedTask = taskModel.getElementAt(selectedIndex);
            int taskId = extractTaskId(selectedTask);
            String newTask = taskInput.getText();
            String newDate = dateInput.getText();

            if (!newTask.isEmpty() && !newDate.isEmpty()) {
                try {
                    PreparedStatement pstmt = conn.prepareStatement("UPDATE tasks SET task = ?, date = ? WHERE id = ?");
                    pstmt.setString(1, newTask);
                    pstmt.setString(2, newDate);
                    pstmt.setInt(3, taskId);
                    pstmt.executeUpdate();
                    loadTasksFromDB(null);
                    taskInput.setText("");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Umple câmpurile pentru editare atunci când selectezi o sarcină
    private void fillFieldsForEditing() {
        int selectedIndex = taskList.getSelectedIndex();
        if (selectedIndex != -1) {
            String selectedTask = taskModel.getElementAt(selectedIndex);
            String[] parts = selectedTask.split(" \\| ");
            if (parts.length > 1) {
                String taskDetails = parts[1];
                String[] taskParts = taskDetails.split(" 📅 ");
                if (taskParts.length > 1) {
                    taskInput.setText(taskParts[0]);
                    dateInput.setText(taskParts[1]);
                }
            }
        }
    }

    // Extrage ID-ul din textul selectat
    private int extractTaskId(String selectedTask) {
        try {
            return Integer.parseInt(selectedTask.split(" \\| ")[0]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ToDoListWithDB::new);
    }
}

