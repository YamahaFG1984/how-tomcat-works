package modern.ch09.session;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import modern.common.Facade;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话对象（原书 org.apache.catalina.session.StandardSession）。
 * 实现 Serializable，管理器停止时会把它写进 SESSIONS.ser，下次启动再读回来。
 * transient 字段不参与序列化：管理器引用和外观对象都在读回后重新建立。
 */
public class StandardSession implements HttpSession, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final long creationTime;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private volatile long lastAccessedTime;
    private volatile int maxInactiveInterval;      // 秒；负数表示永不过期
    private volatile boolean isNew = true;
    private volatile boolean valid = true;

    private transient StandardManager manager;
    private transient HttpSession facade;

    StandardSession(StandardManager manager, String id, int maxInactiveInterval) {
        this.manager = manager;
        this.id = id;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = creationTime;
        this.maxInactiveInterval = maxInactiveInterval;
    }

    /** 交给 Servlet 的永远是外观，Servlet 无法转型回 StandardSession 去调用 expire() 等内部方法 */
    public HttpSession getSession() {
        if (facade == null) {
            facade = Facade.of(HttpSession.class, this);
        }
        return facade;
    }

    /* ---------- 容器内部方法 ---------- */

    void setManager(StandardManager manager) {
        this.manager = manager;
    }

    /** 每次请求用到这个会话时调用：更新最后访问时间，它从此不再是「新」会话 */
    void access() {
        lastAccessedTime = System.currentTimeMillis();
        isNew = false;
    }

    boolean isValid() {
        return valid;
    }

    boolean isExpired(long now) {
        return maxInactiveInterval >= 0 && now - lastAccessedTime >= maxInactiveInterval * 1000L;
    }

    /** 让会话过期：标记无效、从管理器移除、解除所有属性绑定 */
    public void expire() {
        if (!valid) {
            return;
        }
        valid = false;
        if (manager != null) {
            manager.remove(this);
        }
        attributes.clear();
    }

    /* ---------- HttpSession ---------- */

    @Override public String getId() { return id; }
    @Override public long getCreationTime() { checkValid(); return creationTime; }
    @Override public long getLastAccessedTime() { checkValid(); return lastAccessedTime; }
    @Override public ServletContext getServletContext() { return null; }
    @Override public void setMaxInactiveInterval(int interval) { this.maxInactiveInterval = interval; }
    @Override public int getMaxInactiveInterval() { return maxInactiveInterval; }
    @Override public boolean isNew() { checkValid(); return isNew; }
    @Override public void invalidate() { checkValid(); expire(); }

    @Override
    public Object getAttribute(String name) {
        checkValid();
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        checkValid();
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object value) {
        checkValid();
        if (value == null) {
            attributes.remove(name);                // 规范：setAttribute(name, null) 等于 removeAttribute
        } else {
            attributes.put(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        checkValid();
        attributes.remove(name);
    }

    private void checkValid() {
        if (!valid) {
            throw new IllegalStateException("会话 " + id + " 已失效");
        }
    }
}
