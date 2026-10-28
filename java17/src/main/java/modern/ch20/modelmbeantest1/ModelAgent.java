package modern.ch20.modelmbeantest1;

import javax.management.Attribute;
import javax.management.Descriptor;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;
import java.lang.management.ManagementFactory;

/**
 * 模型 MBean 示例（原书 Listing 20.5）：用 JDK 自带的 RequiredModelMBean，手工描述要暴露的属性和操作。
 *
 * 和原书的一处不同：属性 Color 的描述符里写明了 getMethod / setMethod。
 * JMX 规范规定模型 MBean 通过描述符找到读写属性的方法；缺了它们，getAttribute 拿不到 Car 里的真实值。
 *
 * 运行：./run.sh ch20.modelmbeantest1.ModelAgent
 */
public final class ModelAgent {

    public static void main(String[] args) throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        var objectName = new ObjectName("modern.ch20:type=MyCar,variant=RequiredModelMBean");

        var modelMBean = new RequiredModelMBean(createModelMBeanInfo());
        modelMBean.setManagedResource(new Car(), "ObjectReference");
        mBeanServer.registerMBean(modelMBean, objectName);

        mBeanServer.setAttribute(objectName, new Attribute("Color", "green"));
        System.out.println("Color:" + mBeanServer.getAttribute(objectName, "Color"));
        mBeanServer.setAttribute(objectName, new Attribute("Color", "blue"));
        System.out.println("Color:" + mBeanServer.getAttribute(objectName, "Color"));
        mBeanServer.invoke(objectName, "drive", null, null);
    }

    private static ModelMBeanInfoSupport createModelMBeanInfo() {
        Descriptor colorDesc = new DescriptorSupport(
                "name=Color", "descriptorType=attribute", "displayName=Color",
                "getMethod=getColor", "setMethod=setColor");
        var attributes = new ModelMBeanAttributeInfo[] {
                new ModelMBeanAttributeInfo("Color", String.class.getName(), "the color.",
                        true, true, false, colorDesc)
        };
        var operations = new ModelMBeanOperationInfo[] {
                new ModelMBeanOperationInfo("drive", "the drive method", null, "void", MBeanOperationInfo.ACTION),
                new ModelMBeanOperationInfo("getColor", "get color attribute", null,
                        String.class.getName(), MBeanOperationInfo.INFO),
                new ModelMBeanOperationInfo("setColor", "set Color attribute",
                        new MBeanParameterInfo[] {new MBeanParameterInfo("new color", String.class.getName(), "new Color value")},
                        "void", MBeanOperationInfo.ACTION)
        };
        return new ModelMBeanInfoSupport(Car.class.getName(), null, attributes, null, operations, null);
    }
}
