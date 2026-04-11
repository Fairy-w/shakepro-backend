import java.sql.*;

public class ResetAdminPassword {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3307/shakepro?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        String hash = "$2a$10$gWye2Avgn2a0Y4dfQlEKiOEbdVmwcotDNNpMogaskXtkTFP4SP4hC";
        try (Connection conn = DriverManager.getConnection(url, "root", "root123456")) {
            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE users SET password_hash=?, role='ADMIN', enabled=1, nickname=COALESCE(nickname, 'System Admin') WHERE username='admin'")) {
                update.setString(1, hash);
                int updated = update.executeUpdate();
                if (updated == 0) {
                    try (PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO users (username, password_hash, nickname, role, enabled) VALUES ('admin', ?, 'System Admin', 'ADMIN', 1)")) {
                        insert.setString(1, hash);
                        insert.executeUpdate();
                        System.out.println("inserted");
                    }
                } else {
                    System.out.println("updated=" + updated);
                }
            }
        }
    }
}
