public class TestSQLite {
    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ Driver SQLite încărcat cu succes!");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Driver SQLite NU a fost găsit!");
            e.printStackTrace();
        }
    }
}
