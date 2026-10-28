package modern.ch20.modelmbeantest2;

import org.apache.tomcat.util.modeler.ManagedBean;
import org.apache.tomcat.util.modeler.Registry;

import javax.management.Attribute;
import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * 用 Modeler 免去手写 ModelMBeanInfo（原书 Listing 20.9，Commons Modeler → Tomcat 内置的 modeler）。
 * Registry 从 mbeans-descriptors.xml 读出 ManagedBean，ManagedBean.createMBean(资源) 直接得到可注册的 MBean。
 *
 * 运行：./run.sh ch20.modelmbeantest2.ModelAgent
 */
public final class ModelAgent {

    public static void main(String[] args) throws Exception {
        Registry registry = Registry.getRegistry(null);
        registry.loadDescriptors("modern.ch20.modelmbeantest2", ModelAgent.class.getClassLoader());
        MBeanServer mBeanServer = registry.getMBeanServer();          // 就是平台 MBean 服务器

        var car = new Car();
        System.out.println("Creating ObjectName");
        var objectName = new ObjectName("modern.ch20:type=MyCar,variant=Modeler");

        ManagedBean managed = registry.findManagedBean("myMBean");
        DynamicMBean mbean = managed.createMBean(car);
        mBeanServer.registerMBean(mbean, objectName);

        mBeanServer.setAttribute(objectName, new Attribute("color", "green"));
        System.out.println("Color:" + mBeanServer.getAttribute(objectName, "color"));
        mBeanServer.setAttribute(objectName, new Attribute("color", "blue"));
        System.out.println("Color:" + mBeanServer.getAttribute(objectName, "color"));
        mBeanServer.invoke(objectName, "drive", null, null);
    }
}
