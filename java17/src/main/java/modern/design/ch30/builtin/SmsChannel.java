package modern.design.ch30.builtin;

import modern.design.ch30.spi.Channel;
import modern.design.ch30.spi.Message;

/** 内置渠道：短信，只支持 70 个字以内的正文（用到了后加的 supports 方法） */
public class SmsChannel implements Channel {

    @Override public String name() { return "sms"; }

    @Override
    public boolean supports(Message m) {
        return m.text().length() <= 70;
    }

    @Override
    public void send(Message m) {
        if (m.to().startsWith("000")) throw new IllegalStateException("短信网关拒绝了号码 " + m.to());
        System.out.println("      [sms] → " + m.to() + "：" + m.text());
    }
}
