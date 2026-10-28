package modern.ch15.digestertest;

import org.apache.tomcat.util.digester.Digester;

import java.nio.file.Path;

/**
 * Digester 示例 1：创建对象、设置属性、调用方法。
 * 原书使用 commons-digester；Tomcat 从 6.0 起把 Digester 并入了自己的代码（org.apache.tomcat.util.digester），API 几乎相同。
 * 运行：./run.sh ch15.digestertest.Test01
 */
public final class Test01 {

    public static void main(String[] args) throws Exception {
        Path file = Path.of(System.getProperty("user.dir"), "etc", "employee1.xml");
        var digester = new Digester();
        digester.addObjectCreate("employee", Employee.class.getName());
        digester.addSetProperties("employee");
        digester.addCallMethod("employee", "printName");

        var employee = (Employee) digester.parse(file.toFile());
        System.out.println("First name : " + employee.getFirstName());
        System.out.println("Last name : " + employee.getLastName());
    }
}
