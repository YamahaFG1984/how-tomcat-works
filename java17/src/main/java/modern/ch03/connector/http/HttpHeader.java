package modern.ch03.connector.http;

/** 一个请求头。名字统一转成小写，方便比较（HTTP 头名大小写不敏感）。 */
public record HttpHeader(String name, String value) {
}
