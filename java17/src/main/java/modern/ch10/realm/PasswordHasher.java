package modern.ch10.realm;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 加盐、慢速的密码哈希（PBKDF2-HMAC-SHA256，JDK 自带）。
 * 存储格式：pbkdf2$迭代次数$盐(Base64)$哈希(Base64)
 * 原书的 SimpleRealm 直接存明文并用 equals 比较——数据库一旦泄露，所有密码就都泄露了。
 */
public final class PasswordHasher {

    private static final int ITERATIONS = 210_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS);
        var b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
    }

    public static boolean matches(String password, String stored) {
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !parts[0].equals("pbkdf2")) {
            return false;
        }
        var b64 = Base64.getDecoder();
        byte[] expected = b64.decode(parts[3]);
        byte[] actual = pbkdf2(password, b64.decode(parts[2]), Integer.parseInt(parts[1]));
        // 常数时间比较：不会因为「前几个字节对上了」而提前返回，避免时序攻击
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
