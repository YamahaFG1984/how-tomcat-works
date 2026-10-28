package modern.ch12.startup;

import modern.ch12.core.StandardContextMapper;

import java.util.Map;

/**
 * 不启动服务器，直接演示四条映射规则。
 * 运行：./run.sh ch12.startup.MapperDemo
 */
public final class MapperDemo {

    public static void main(String[] args) {
        var mapper = new StandardContextMapper(Map.of(
                "/Primitive", "Primitive",
                "/modern/*", "Modern",
                "/modern/admin/*", "Admin",
                "*.hello", "Hello",
                "/", "Default"));

        for (String uri : new String[] {
                "/Primitive", "/modern", "/modern/users/42", "/modern/admin/x",
                "/a/b/index.hello", "/modern/x.hello", "/", "/nothing/here"}) {
            System.out.printf("%-20s → %s%n", uri, mapper.map(uri).map(Object::toString).orElse("找不到"));
        }
    }
}
