package modern.ch03.connector.http;

/**
 * 请求行。原书的 HttpRequestLine 用三个可复用的 char[] 加三个 xxxEnd 下标，
 * 目的是避免为每个请求创建 String（2004 年 GC 很贵）。现代 JVM 上短命小对象几乎免费，
 * 一个 record 就够了。
 */
public record HttpRequestLine(String method, String uri, String protocol) {
}
