package modern.ch15.digestertest;

import org.apache.tomcat.util.digester.Digester;

import java.nio.file.Path;

/**
 * Digester 示例 3：用 RuleSet 添加规则，输出和 Test02 完全相同。
 * 运行：./run.sh ch15.digestertest.Test03
 */
public final class Test03 {

    public static void main(String[] args) throws Exception {
        Path file = Path.of(System.getProperty("user.dir"), "etc", "employee2.xml");
        var digester = new Digester();
        digester.addRuleSet(new EmployeeRuleSet());
        Test02.print((Employee) digester.parse(file.toFile()));
    }
}
