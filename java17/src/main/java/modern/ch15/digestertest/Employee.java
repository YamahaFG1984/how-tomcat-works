package modern.ch15.digestertest;

import java.util.ArrayList;
import java.util.List;

/**
 * Digester 按 JavaBean 规范（无参构造器 + setXxx）创建和填充对象，
 * 所以这几个类保持了原书的 getter/setter 写法，没有改成 record。
 */
public class Employee {

    private String firstName;
    private String lastName;
    private final List<Office> offices = new ArrayList<>();

    public Employee() {
        System.out.println("Creating Employee");
    }

    public String getFirstName() { return firstName; }

    public void setFirstName(String firstName) {
        System.out.println("Setting firstName : " + firstName);
        this.firstName = firstName;
    }

    public String getLastName() { return lastName; }

    public void setLastName(String lastName) {
        System.out.println("Setting lastName : " + lastName);
        this.lastName = lastName;
    }

    public void addOffice(Office office) {
        System.out.println("Adding Office to this employee");
        offices.add(office);
    }

    public List<Office> getOffices() {
        return offices;
    }

    public void printName() {
        System.out.println("My name is " + firstName + " " + lastName);
    }
}
