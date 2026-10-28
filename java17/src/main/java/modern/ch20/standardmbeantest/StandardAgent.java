package modern.ch20.standardmbeantest;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * 标准 MBean 示例（原书 Listing 20.3）。
 * 原书用 MBeanServerFactory.createMBeanServer() 新建一个 MBean 服务器（当时 JMX 还要单独下载 MX4J 等实现）；
 * Java 5 起 JMX 内置在 JDK 里，通常直接用平台 MBean 服务器——JConsole、VisualVM 连上来就能看到。
 *
 * 运行：./run.sh ch20.standardmbeantest.StandardAgent
 */
public final class StandardAgent {

    public static void main(String[] args) throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        var objectName = new ObjectName("modern.ch20:type=" + Car.class.getName());

        // 原书 createMBean(类名, 名字) 让服务器自己 new；直接注册一个现成的对象也可以
        mBeanServer.registerMBean(new Car(), objectName);

        mBeanServer.setAttribute(objectName, new Attribute("Color", "blue"));
        System.out.println(mBeanServer.getAttribute(objectName, "Color"));
        mBeanServer.invoke(objectName, "drive", null, null);
    }
}
