package modern.ch10.core;

/** web.xml 里的 <login-config>：认证方式（BASIC / FORM / DIGEST / CLIENT-CERT / NONE）和域的名字 */
public record LoginConfig(String authMethod, String realmName) {
}
