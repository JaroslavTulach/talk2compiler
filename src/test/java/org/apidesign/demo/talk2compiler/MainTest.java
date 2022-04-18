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
        for (int i = 0; i < count; i++) {
            eval(new Number[] { 0 });
            eval(new Number[] { 0, 1, 2, 3, 4, 5, 6, 7 });
        }
    }

    @Test
    public void evalTest() {
        Assert.assertEquals(1, eval(new Number[] { 0 }));
        Assert.assertEquals(2, eval(new Number[] { 1, 2, 3, 4, 5, 6 }));
    }

    private static Object eval(Object[] arr) {
        return Main.CODE.call(arr);
    }
}
