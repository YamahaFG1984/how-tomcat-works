package modern.ch15.digestertest;

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSet;

/**
 * 把一组规则打包成 RuleSet（原书继承 RuleSetBase；Tomcat 的 RuleSet 是只有一个方法的接口）。
 * Tomcat 用同样的方式组织 server.xml 和 web.xml 的解析规则：ContextRuleSet、WebRuleSet……
 */
public class EmployeeRuleSet implements RuleSet {

    @Override
    public void addRuleInstances(Digester digester) {
        digester.addObjectCreate("employee", Employee.class.getName());
        digester.addSetProperties("employee");
        digester.addObjectCreate("employee/office", Office.class.getName());
        digester.addSetProperties("employee/office");
        digester.addSetNext("employee/office", "addOffice", Office.class.getName());
        digester.addObjectCreate("employee/office/address", Address.class.getName());
        digester.addSetProperties("employee/office/address");
        digester.addSetNext("employee/office/address", "setAddress", Address.class.getName());
    }
}
