import java.sql.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

import static java.time.format.DateTimeFormatter.ofPattern;

public class ToDoList
{
    private DefaultListModel<String> taskModel;
    private Connection conn = null;
    private JTextField taskInput, dateInput;
    private JList<String> taskList;

    private void connectToDatbase()
    {
        try{
            conn = DriverManager.getConnection("jdbc:sqlite:DataBase.db");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS Tasks (id INTEGER PRIMARY KEY , task TEXT, date TEXT)");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
    private void loadTaskFromDB(String dateFilter) {
        if (taskModel==null) return;
        taskModel.clear();
        try {
            PreparedStatement pstmt;
            if (dateFilter == null || dateFilter.isEmpty()) {
                pstmt = conn.prepareStatement("SELECT id, task, date FROM Tasks");
            }
            else
            {
                pstmt = conn.prepareStatement("SELECT id,task,date FROM Tasks WHERE date = ?");
                pstmt.setString(1, dateFilter);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                taskModel.addElement( rs.getInt("id") + "|Task: " + rs.getString("task") + "|Date: " + rs.getString("date"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    private void addTask()
    {
        String task = taskInput.getText();
        if(task.length() > 0)
        {
            task = task.substring(0,1).toUpperCase() + task.substring(1);
        }
        String date = getCurrentDate();
        if(!task.isEmpty() && !date.isEmpty())
        {
            try
            {
                PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Tasks (task,date) VALUES (?, ?)");
                pstmt.setString(1,task);
                pstmt.setString(2,date);
                pstmt.executeUpdate();
                loadTaskFromDB(null);
                taskInput.setText("");

            }
            catch(SQLException e)
            {
                e.printStackTrace();
            }
        }
    }
    private String getCurrentDate()
    {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }
    private int extractTaskId(String selected_task)
    {
        try{
            return Integer.parseInt(selected_task.split("\\|Task: ")[0]);
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
            return -1;
        }
    }
    private void update()
    {
        int selected_index = taskList.getSelectedIndex();
        if(selected_index != -1)
        {
            String selected_task = taskModel.getElementAt(selected_index);
            int task_id = extractTaskId(selected_task);
            String new_task = taskInput.getText();
            String new_date = dateInput.getText();

            if(!new_task.isEmpty() && !new_date.isEmpty())
            {
                try
                {
                    PreparedStatement pstmt = conn.prepareStatement("UPDATE Tasks SET task = ?, date = ? WHERE id = ?");
                    pstmt.setString(1,new_task);
                    pstmt.setString(2,new_date);
                    pstmt.setInt(3,task_id);
                    pstmt.executeUpdate();
                    loadTaskFromDB(null);
                    taskInput.setText("");
                }
                catch(SQLException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void fill()
    {
        int selected_index = taskList.getSelectedIndex();
        if(selected_index != -1)
        {
            String selected_task = taskModel.getElementAt(selected_index);
            String[] parts = selected_task.split("\\| ");
            if(parts.length >1)
            {
                String task = parts[1].split(": ")[1];
                taskInput.setText(task);
            }
            if(parts.length >2)
            {
                String date = parts[2].split(": ")[1];
                dateInput.setText(date);
            }

        }
    }

    private void delete() {
        int selected_index = taskList.getSelectedIndex();
        if (selected_index != -1) {
            String selected_task = taskModel.getElementAt(selected_index);
            int task_id = extractTaskId(selected_task);
            try {
                PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Tasks WHERE id = ?");
                pstmt.setInt(1, task_id);
                pstmt.executeUpdate();
                resetTaskId();
                reorderTaskId();
                loadTaskFromDB(null);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    private void resetTaskId()
    {
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='Tasks'");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void reorderTaskId() {
        try {

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, task, date FROM Tasks ORDER BY id");


            int newId = 1;
            while (rs.next()) {
                int currentId = rs.getInt("id");


                if (currentId != newId) {
                    PreparedStatement pstmt = conn.prepareStatement("UPDATE Tasks SET id = ? WHERE id = ?");
                    pstmt.setInt(1, newId);
                    pstmt.setInt(2, currentId);
                    pstmt.executeUpdate();
                }


                newId++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void openSearchWindow()
    {
        JDialog searchDialog = new JDialog();
        searchDialog.setTitle("Search Task");
        searchDialog.setSize(300, 150);
        searchDialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(2, 1));
        JTextField searchDateInput = new JTextField(getCurrentDate());
        JButton searchButton = new JButton("Search");

        panel.add(new JLabel("Enter Date (DD-MM-YYYY):"));
        panel.add(searchDateInput);
        searchDialog.add(panel, BorderLayout.CENTER);
        searchDialog.add(searchButton, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> {
            loadTaskFromDB(searchDateInput.getText());
            searchDialog.dispose();
        });

        searchDialog.setVisible(true);
    }

    public ToDoList()
    {
        connectToDatbase();

        JFrame frame = new JFrame("To-Do List");
        frame.setSize(500,500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setBackground(Color.PINK);

        taskModel = new DefaultListModel<>();
        taskList = new JList<>(taskModel);
        taskList.setFont(new Font("Times New Roman",Font.PLAIN,16));
        loadTaskFromDB(null);

        JScrollPane scrollPane = new JScrollPane(taskList);
        frame.add(scrollPane,BorderLayout.CENTER);


        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(2,2));
        inputPanel.setBackground(Color.PINK);

        taskInput = new JTextField();
        taskInput.setPreferredSize(new Dimension(300,40));
        dateInput = new JTextField(getCurrentDate());
        dateInput.setEditable(false);
        JButton addButton = new JButton("Add Task");
        addButton.setBackground(Color.PINK);
        ImageIcon icon = new ImageIcon("add_button.png");
        Image img = icon.getImage().getScaledInstance(60,60,Image.SCALE_SMOOTH);
        addButton.setIcon(new ImageIcon(img));

        JButton editButton = new JButton("Edit Task");
        editButton.setBackground(Color.PINK);
        ImageIcon icon1 = new ImageIcon("edit_button.png");
        Image img1 = icon1.getImage().getScaledInstance(60,60,Image.SCALE_SMOOTH);
        editButton.setIcon(new ImageIcon(img1));


        JButton searchButton = new JButton("Search Task");
        searchButton.setBackground(Color.PINK);
        ImageIcon icon2 = new ImageIcon("search_button.png");
        Image img2 = icon2.getImage().getScaledInstance(60,60,Image.SCALE_SMOOTH);
        searchButton.setIcon(new ImageIcon(img2));

        JButton deleteButton = new JButton("Delete Task");
        deleteButton.setBackground(Color.PINK);
        ImageIcon icon3 = new ImageIcon("trash_button.png");
        Image img3 = icon3.getImage().getScaledInstance(60,60,Image.SCALE_SMOOTH);
        deleteButton.setIcon(new ImageIcon(img3));

        JLabel task = new JLabel("Tasks");
        task.setFont(new Font("Times New Roman",Font.BOLD,18));
        inputPanel.add(task);
        inputPanel.add(taskInput);



        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(deleteButton);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(buttonPanel,BorderLayout.SOUTH);

        addButton.addActionListener(e -> addTask());
        editButton.addActionListener(e->update());
        searchButton.addActionListener(e->openSearchWindow());
        deleteButton.addActionListener(e->delete());

        taskList.addListSelectionListener(e->fill());
        frame.setVisible(true);

    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(ToDoList::new);
    }

}

