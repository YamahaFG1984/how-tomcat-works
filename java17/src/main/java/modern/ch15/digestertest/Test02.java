package modern.ch15.digestertest;

import org.apache.tomcat.util.digester.Digester;

import java.nio.file.Path;

/**
 * Digester 示例 2：创建三层对象，并用 SetNext 建立父子关系。
 * 运行：./run.sh ch15.digestertest.Test02
 */
public final class Test02 {

    public static void main(String[] args) throws Exception {
        Path file = Path.of(System.getProperty("user.dir"), "etc", "employee2.xml");
        var digester = new Digester();
        digester.addObjectCreate("employee", Employee.class.getName());
        digester.addSetProperties("employee");
        digester.addObjectCreate("employee/office", Office.class.getName());
        digester.addSetProperties("employee/office");
        digester.addSetNext("employee/office", "addOffice", Office.class.getName());
        digester.addObjectCreate("employee/office/address", Address.class.getName());
        digester.addSetProperties("employee/office/address");
        digester.addSetNext("employee/office/address", "setAddress", Address.class.getName());

        print((Employee) digester.parse(file.toFile()));
    }

    public static void print(Employee employee) {
        System.out.println("-------------------------------------------------");
        for (Office office : employee.getOffices()) {
            Address address = office.getAddress();
            System.out.println(office.getDescription());
            System.out.println("Address : " + address.getStreetNumber() + " " + address.getStreetName());
            System.out.println("--------------------------------");
        }
    }
}
