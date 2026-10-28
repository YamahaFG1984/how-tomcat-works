package modern.ch18.startup;

import modern.ch06.core.LifecycleEvent;
import modern.ch06.core.LifecycleException;
import modern.ch06.core.LifecycleListener;
import modern.ch15.mini.MiniDigester;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Host 的生命周期监听器（原书 org.apache.catalina.startup.HostConfig）：
 *   START：扫描 appBase，依次部署 ① 描述符 *.xml ② WAR 包 *.war ③ 含 WEB-INF 的目录；
 *   之后每隔 checkInterval 秒再扫描一次（live deploy）：新出现的应用自动部署，消失的应用自动卸载，
 *   web.xml 被修改的应用自动重载。
 *   STOP：停止扫描。
 */
public class HostConfig implements LifecycleListener {

    private final StandardHostDeployer deployer;
    private final Path appBase;
    /** 已处理过的 appBase 条目 → 对应的上下文路径（原书的 deployed 列表） */
    private final Map<String, String> deployed = new ConcurrentHashMap<>();
    private final Map<String, Long> webXmlTimes = new ConcurrentHashMap<>();
    private int checkInterval = 15;
    private boolean unpackWARs = true;
    private ScheduledExecutorService checker;

    public HostConfig(StandardHostDeployer deployer, Path appBase) {
        this.deployer = deployer;
        this.appBase = appBase.toAbsolutePath();
    }

    public void setCheckInterval(int seconds) { this.checkInterval = seconds; }
    public void setUnpackWARs(boolean unpackWARs) { this.unpackWARs = unpackWARs; }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        switch (event.type()) {
            case START -> {
                deployApps();
                checker = Executors.newSingleThreadScheduledExecutor(r -> {
                    var t = new Thread(r, "HostConfig-liveDeploy");
                    t.setDaemon(true);
                    return t;
                });
                checker.scheduleWithFixedDelay(this::check, checkInterval, checkInterval, TimeUnit.SECONDS);
            }
            case STOP -> checker.shutdownNow();
            default -> { }
        }
    }

    /** 三个部署方法的顺序很重要：描述符可以覆盖同名的 WAR 和目录，WAR 解压后的目录不应被当作新目录再部署一次 */
    synchronized void deployApps() {
        List<Path> files = list();
        files.stream().filter(p -> p.toString().endsWith(".xml")).forEach(this::deployDescriptor);
        files.stream().filter(p -> p.toString().endsWith(".war")).forEach(this::deployWAR);
        list().stream().filter(Files::isDirectory).forEach(this::deployDirectory);
    }

    /** live deploy：部署新应用、卸载已删除的应用、重载 web.xml 被修改的应用 */
    synchronized void check() {
        deployApps();
        deployed.entrySet().removeIf(entry -> {
            if (Files.exists(appBase.resolve(entry.getKey()))) {
                return false;
            }
            try {
                deployer.remove(entry.getValue());
            } catch (LifecycleException e) {
                e.printStackTrace();
            }
            return true;
        });
        webXmlTimes.forEach((contextPath, time) -> {
            var context = deployer.findDeployedApp(contextPath);
            if (context != null && lastModified(context.getDocBase().resolve("WEB-INF/web.xml")) != time) {
                webXmlTimes.put(contextPath, lastModified(context.getDocBase().resolve("WEB-INF/web.xml")));
                System.out.println("HostConfig: " + contextPath + " 的 web.xml 被修改，重载");
                context.reload();
            }
        });
    }

    /** 描述符 hello.xml 里写着 <Context docBase="..."/>，上下文路径由文件名决定：/hello（ROOT.xml → ""） */
    private void deployDescriptor(Path xml) {
        String name = xml.getFileName().toString();
        if (deployed.containsKey(name)) {
            return;
        }
        String contextPath = contextPath(name.substring(0, name.length() - 4));
        var digester = new MiniDigester();
        var docBase = new String[1];
        digester.addRule("Context", new MiniDigester.Rule() {
            @Override public void begin(MiniDigester d, Attributes a) { docBase[0] = a.getValue("docBase"); }
        });
        try {
            digester.parse(xml);
        } catch (Exception e) {
            throw new IllegalStateException("解析描述符失败: " + xml, e);
        }
        deployed.put(name, contextPath);
        System.out.println("HostConfig: 部署描述符 " + name);
        install(contextPath, Path.of(docBase[0]));
    }

    /** app2.war → /app2；unpackWARs 时先解压到 appBase/app2/ 再按目录部署 */
    private void deployWAR(Path war) {
        String name = war.getFileName().toString();
        if (deployed.containsKey(name)) {
            return;
        }
        String base = name.substring(0, name.length() - 4);
        String contextPath = contextPath(base);
        deployed.put(name, contextPath);
        if (deployer.findDeployedApp(contextPath) != null) {
            return;                                          // 已经由描述符部署
        }
        Path dir = appBase.resolve(base);
        System.out.println("HostConfig: 部署 WAR " + name + (unpackWARs ? "（解压到 " + base + "/）" : ""));
        expand(war, dir);
        deployed.put(base, contextPath);                     // 解压出的目录不再当作新目录
        install(contextPath, dir);
    }

    /** 只有含 WEB-INF 的目录才算 Web 应用；app1/ → /app1，ROOT/ → "" */
    private void deployDirectory(Path dir) {
        String name = dir.getFileName().toString();
        if (deployed.containsKey(name) || !Files.isDirectory(dir.resolve("WEB-INF"))) {
            return;
        }
        String contextPath = contextPath(name);
        deployed.put(name, contextPath);
        if (deployer.findDeployedApp(contextPath) != null) {
            return;
        }
        System.out.println("HostConfig: 部署目录 " + name + "/");
        install(contextPath, dir);
    }

    private void install(String contextPath, Path docBase) {
        try {
            deployer.install(contextPath, docBase);
            webXmlTimes.put(contextPath, lastModified(docBase.toAbsolutePath().resolve("WEB-INF/web.xml")));
        } catch (LifecycleException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 解压 WAR，并防止「zip slip」：条目名里的 ../ 想写到目标目录之外的，一律拒绝 */
    private static void expand(Path war, Path dir) {
        try (var zip = new ZipFile(war.toFile())) {
            for (ZipEntry entry : zip.stream().toList()) {
                Path target = dir.resolve(entry.getName()).normalize();
                if (!target.startsWith(dir)) {
                    throw new IOException("非法的 WAR 条目: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (var in = zip.getInputStream(entry)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<Path> list() {
        try (Stream<Path> s = Files.list(appBase)) {
            return s.filter(p -> !p.getFileName().toString().equalsIgnoreCase("WEB-INF")
                            && !p.getFileName().toString().equalsIgnoreCase("META-INF"))
                    .sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String contextPath(String name) {
        return name.equals("ROOT") ? "" : "/" + name;
    }

    private static long lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return -1;
        }
    }
}
