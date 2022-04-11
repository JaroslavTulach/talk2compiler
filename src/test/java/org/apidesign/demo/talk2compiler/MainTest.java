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
        Number[] arr = { 1, 8, 9 };
        for (int i = 0; i < count; i++) {
            eval(arr);
            if (i % 10000 == 0)
                System.out.println(i);
        }
    }

    @Test
    public void evalTest() {
        Assert.assertEquals(10, eval(new Number[] { 1, Math.PI, 11, 15 }));
    }
    
    private static Object eval(Object[] arr) {
        return Main.CODE.call(arr);
    }
}
