package org.apidesign.demo.talk2compiler;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class MainTest {
    @Before
    public void warmingUp() {
        Assume.assumeFalse("Skip warmup if IGV dump isn't requested", Boolean.getBoolean("noigv"));
        for (int i = 0; i < 10000000; i++) {
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
