package modern.ch09.session;

import modern.ch06.core.Lifecycle;
import modern.ch06.core.LifecycleListener;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 会话管理器（原书 ManagerBase + StandardManager）：
 *  - 运行时把会话放在内存里；
 *  - 后台任务每隔 checkInterval 秒清理过期会话（processExpires）；
 *  - stop() 时把仍然有效的会话序列化到 SESSIONS.ser，start() 时再读回来。
 */
public class StandardManager implements Lifecycle {

    private final Map<String, StandardSession> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Path pathname;
    private int maxInactiveInterval = 30 * 60;   // StandardContext 的 sessionTimeout 默认 30 分钟
    private int checkInterval = 60;
    private ScheduledExecutorService expirer;

    public StandardManager(Path pathname) {
        this.pathname = pathname;
    }

    public void setMaxInactiveInterval(int seconds) { this.maxInactiveInterval = seconds; }
    public void setCheckInterval(int seconds) { this.checkInterval = seconds; }

    /** 会话 ID：16 个随机字节（128 位）转十六进制。必须用 SecureRandom，否则 ID 可被猜出来劫持会话 */
    public StandardSession createSession() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        var session = new StandardSession(this, HexFormat.of().withUpperCase().formatHex(bytes), maxInactiveInterval);
        sessions.put(session.getId(), session);
        return session;
    }

    /** 找到有效的会话就更新访问时间并返回；不存在或已过期返回 null */
    public StandardSession findSession(String id) {
        if (id == null) {
            return null;
        }
        StandardSession session = sessions.get(id);
        if (session == null) {
            return null;
        }
        if (session.isExpired(System.currentTimeMillis())) {
            session.expire();
            return null;
        }
        session.access();
        return session;
    }

    public List<StandardSession> findSessions() {
        return List.copyOf(sessions.values());
    }

    void remove(StandardSession session) {
        sessions.remove(session.getId());
    }

    public void processExpires() {
        long now = System.currentTimeMillis();
        sessions.values().stream().filter(s -> s.isExpired(now)).forEach(s -> {
            System.out.println("StandardManager: 会话 " + s.getId() + " 已过期");
            s.expire();
        });
    }

    /* ---------- 持久化 ---------- */

    /**
     * 反序列化是著名的安全隐患：恶意构造的字节流可以触发任意「gadget」类的代码。
     * Java 9 起可以给 ObjectInputStream 设置过滤器，只允许预期的类。
     */
    private static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "modern.ch09.session.StandardSession;java.lang.*;java.util.**;!*");

    public void load() {
        if (!Files.exists(pathname)) {
            return;
        }
        try (var in = new ObjectInputStream(Files.newInputStream(pathname))) {
            in.setObjectInputFilter(FILTER);
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                var session = (StandardSession) in.readObject();
                session.setManager(this);
                sessions.put(session.getId(), session);
            }
            System.out.println("StandardManager: 从 " + pathname.getFileName() + " 恢复了 " + count + " 个会话");
        } catch (InvalidClassException e) {
            System.out.println("StandardManager: 拒绝反序列化 " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            Files.deleteIfExists(pathname);            // 读完即删，和 Tomcat 一样
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void unload() {
        processExpires();
        List<StandardSession> valid = findSessions();
        try {
            Files.createDirectories(pathname.toAbsolutePath().getParent());
            try (var out = new ObjectOutputStream(Files.newOutputStream(pathname))) {
                out.writeInt(valid.size());
                for (StandardSession session : valid) {
                    out.writeObject(session);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.out.println("StandardManager: 把 " + valid.size() + " 个会话保存到 " + pathname);
        sessions.clear();
    }

    /* ---------- Lifecycle ---------- */

    @Override
    public void start() {
        load();
        if (checkInterval <= 0) {
            return;          // 不开自己的线程，由容器的后台线程调用 processExpires()（第 12 章，Tomcat 5 的做法）
        }
        expirer = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "StandardManager-expirer");
            t.setDaemon(true);
            return t;
        });
        expirer.scheduleWithFixedDelay(this::processExpires, checkInterval, checkInterval, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (expirer != null) {
            expirer.shutdownNow();
        }
        unload();
    }

    @Override public void addLifecycleListener(LifecycleListener listener) { }
    @Override public List<LifecycleListener> findLifecycleListeners() { return List.of(); }
    @Override public void removeLifecycleListener(LifecycleListener listener) { }
}
