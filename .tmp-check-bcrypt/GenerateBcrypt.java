import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String[] passwords = {"test123456", "admin123456"};
        for (String password : passwords) {
            System.out.println(password + "=" + encoder.encode(password));
        }
    }
}
