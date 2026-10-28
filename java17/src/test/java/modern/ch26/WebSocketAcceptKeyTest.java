package modern.ch26;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 第 26 章：RFC 6455 第 1.3 节给出的握手示例 */
class WebSocketAcceptKeyTest {

    @Test
    void rfc6455Example() throws Exception {
        Method m = Class.forName("modern.ch26.ws.MiniWebSocketServer").getDeclaredMethod("acceptKey", String.class);
        m.setAccessible(true);
        assertEquals("s3pPLMBiTxaQ9kYGzzhZRbK+xOo=", m.invoke(null, "dGhlIHNhbXBsZSBub25jZQ=="));
    }
}
