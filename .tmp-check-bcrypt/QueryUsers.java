import java.sql.*;

public class QueryUsers {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3307/shakepro?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        try (Connection conn = DriverManager.getConnection(url, "root", "root123456");
             PreparedStatement ps = conn.prepareStatement("select id, username, role, enabled, password_hash from users order by id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.println(rs.getLong("id") + " | " + rs.getString("username") + " | " + rs.getString("role") + " | " + rs.getBoolean("enabled") + " | " + rs.getString("password_hash"));
            }
        }
    }
}
