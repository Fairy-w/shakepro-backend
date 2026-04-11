import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class CheckBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        String[] passwords = {"test123456", "password", "admin123456", "123456", "admin", "root123456"};
        for (String password : passwords) {
            System.out.println(password + "=" + encoder.matches(password, hash));
        }
    }
}
