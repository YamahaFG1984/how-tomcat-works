package modern.ch15.mini;

import modern.ch15.digestertest.Address;
import modern.ch15.digestertest.Employee;
import modern.ch15.digestertest.Office;
import modern.ch15.digestertest.Test02;

import java.nio.file.Path;

/**
 * 用自己写的 MiniDigester 跑原书的 Test02，输出应当完全一样。
 * 创建对象用构造器引用（Employee::new），不再需要按类名反射。
 * 运行：./run.sh ch15.mini.MiniDigesterTest
 */
public final class MiniDigesterTest {

    public static void main(String[] args) throws Exception {
        var digester = new MiniDigester();
        digester.addObjectCreate("employee", Employee::new);
        digester.addSetProperties("employee");
        digester.addObjectCreate("employee/office", Office::new);
        digester.addSetProperties("employee/office");
        digester.addSetNext("employee/office", "addOffice");
        digester.addObjectCreate("employee/office/address", Address::new);
        digester.addSetProperties("employee/office/address");
        digester.addSetNext("employee/office/address", "setAddress");

        Employee employee = digester.parse(Path.of(System.getProperty("user.dir"), "etc", "employee2.xml"));
        Test02.print(employee);
    }
}
