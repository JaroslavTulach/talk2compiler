package org.apidesign.demo.talk2compiler;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class MainTest {
    @After
    public void warmingUp() throws Exception {
        int count;
        if (Boolean.getBoolean("noigv")) {
            // Skip warmup if IGV dump isn't requested
            count = 1;
        } else {
            count = 10000000;
        }
        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < count; i++) {
                Main.CODE.call("kuk");
            }
            Thread.sleep(1000);
        }
    }

    @Test
    public void checkSayHello() {
        Main.MAIN.addMapping("huh", 33);
        Main.MAIN.addMapping("kuk", 66);
        Main.CODE.call("kuk");
        Main.CODE.call("buk");
        Main.CODE.call("muk");
        Main.CODE.call("puk");
        Assert.assertEquals(66, Main.CODE.call("kuk"));
        Assert.assertNull(Main.CODE.call("buk"));
        Assert.assertEquals(33, Main.CODE.call("huh"));
    }
}
