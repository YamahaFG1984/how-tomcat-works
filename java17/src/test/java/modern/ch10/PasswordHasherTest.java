package modern.ch10;

import modern.ch10.realm.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 第 10 章：加盐哈希，同一个密码每次结果不同，但都能校验通过 */
class PasswordHasherTest {

    @Test
    void saltedHashesVerify() {
        String a = PasswordHasher.hash("secret");
        String b = PasswordHasher.hash("secret");
        assertNotEquals(a, b);
        assertTrue(PasswordHasher.matches("secret", a));
        assertFalse(PasswordHasher.matches("Secret", a));
    }
}
