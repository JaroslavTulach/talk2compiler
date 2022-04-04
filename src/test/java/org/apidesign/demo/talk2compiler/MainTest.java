package org.apidesign.demo.talk2compiler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MainTest {
    @Before
    public void warmingUp() {
        int count;
        if (Boolean.getBoolean("noigv")) {
            // Skip warmup if IGV dump isn't requested
            count = 1;
        } else {
            count = 1000000000;
        }
        Number[] arr = { 7, 8, 9 };
        for (int i = 0; i < count; i++) {
            eval(arr);
        }
    }

    @Test
    public void evalTest() {
        Assert.assertEquals(16 + Math.PI, eval(new Number[] { 5, Math.PI, 11, 15 }));
    }

    private static Object eval(Number[] arr) {
        return Main.CODE.call(arr);
    }
}
