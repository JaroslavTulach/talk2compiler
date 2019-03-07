package org.apidesign.demo.talk2compiler;

import org.junit.Assert;
import org.junit.Test;

public class MainTest {
    @Test
    public void sayHello() {
        Object ret = Main.CODE.call("Truffle");
        Assert.assertEquals("Hello from Truffle!", ret);
    }
}
