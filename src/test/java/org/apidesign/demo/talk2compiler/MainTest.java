package org.apidesign.demo.talk2compiler;

import org.graalvm.polyglot.Context;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MainTest {
    @Test
    public void my() {
        Context c = Context.create("my");
        c.eval("my", "nic");
    }

    @Before
    public void warmingUp() {
        int count;
        if (Boolean.getBoolean("noigv")) {
            // Skip warmup if IGV dump isn't requested
            count = 1;
        } else {
            count = 1000000;
        }
        for (int i = 0; i < count; i++) {
            sayHelloTruffle();
        }
    }

    @Test
    public void checkSayHello() {
        Assert.assertEquals("Hello from Truffle!", sayHelloTruffle());
    }

    private static Object sayHelloTruffle() {
        return Main.CODE.call("Truffle");
    }
}
