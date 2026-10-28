package modern.ch26.tomcat;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tomcat 11 的 TLS 与 HTTP/2：
 *   8443  HTTPS 连接器 + Http2Protocol：浏览器/curl 通过 TLS 的 ALPN 协商出 h2
 *   8080  明文连接器 + Http2Protocol：支持 h2c（curl --http2-prior-knowledge 或 Upgrade: h2c）
 * 启动时用 keytool 生成一张自签名证书（只用于演示，浏览器会提示不安全）。
 *   /info        协议、是否安全、客户端端口（同一个 TCP 连接上的请求端口相同）
 *   /slow?ms=1000
 *
 * 运行：./run.sh ch26.tomcat.EmbeddedHttp2Tls
 *       curl -k -v --http2 https://localhost:8443/info
 *       ./run.sh ch26.tomcat.Http2Client https://localhost:8443/slow 20
 */
public final class EmbeddedHttp2Tls {

    public static void main(String[] args) throws Exception {
        int httpPort = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int httpsPort = args.length > 1 ? Integer.parseInt(args[1]) : 8443;
        Path keystore = createSelfSignedKeystore();

        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch26").toString());
        tomcat.setPort(httpPort);
        Connector http = tomcat.getConnector();
        http.setProperty("address", "127.0.0.1");
        http.addUpgradeProtocol(new Http2Protocol());                 // h2c

        var https = new Connector();                                  // 默认 HTTP/1.1 NIO
        https.setPort(httpsPort);
        https.setProperty("address", "127.0.0.1");
        https.setScheme("https");
        https.setSecure(true);
        https.setProperty("SSLEnabled", "true");
        var ssl = new SSLHostConfig();                                // 可以有多个，按 SNI 主机名选择
        var cert = new SSLHostConfigCertificate(ssl, SSLHostConfigCertificate.Type.RSA);
        cert.setCertificateKeystoreFile(keystore.toString());
        cert.setCertificateKeystorePassword("changeit");
        cert.setCertificateKeyAlias("tomcat");
        ssl.addCertificate(cert);
        https.addSslHostConfig(ssl);
        https.addUpgradeProtocol(new Http2Protocol());                // TLS + ALPN 协商 h2
        tomcat.getService().addConnector(https);

        Context ctx = tomcat.addContext("", Files.createTempDirectory("ch26-docbase").toString());
        Tomcat.addServlet(ctx, "info", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                if (req.getRequestURI().startsWith("/slow")) {
                    String ms = req.getParameter("ms");
                    try {
                        Thread.sleep(ms == null ? 1000 : Long.parseLong(ms));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().printf("protocol=%s secure=%s scheme=%s 客户端端口=%d 线程=%s%n",
                        req.getProtocol(), req.isSecure(), req.getScheme(), req.getRemotePort(),
                        Thread.currentThread().getName());
            }
        });
        ctx.addServletMappingDecoded("/", "info");

        tomcat.start();
        System.out.printf("HTTP/2：https://localhost:%d/info（h2）  http://localhost:%d/info（h2c）%n", httpsPort, httpPort);
        tomcat.getServer().await();
    }

    /** 调用 JDK 自带的 keytool 生成自签名证书；keytool 和当前 java 在同一个 bin 目录 */
    static Path createSelfSignedKeystore() throws IOException, InterruptedException {
        Path dir = Files.createTempDirectory("ch26-tls");
        Path keystore = dir.resolve("localhost.p12");
        String javaBin = ProcessHandle.current().info().command().orElse("java");
        String keytool = Path.of(javaBin).resolveSibling("keytool").toString();
        Process p = new ProcessBuilder(keytool, "-genkeypair", "-alias", "tomcat", "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "30", "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1",
                "-storetype", "PKCS12", "-keystore", keystore.toString(), "-storepass", "changeit")
                .inheritIO().start();
        if (p.waitFor() != 0) throw new IOException("keytool 失败");
        System.out.println("已生成自签名证书：" + keystore);
        return keystore;
    }
}
