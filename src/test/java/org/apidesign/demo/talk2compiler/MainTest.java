package org.apidesign.demo.talk2compiler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MainTest {
    @Before
    public void warmingUp() {
        Main.Plus program = new Main.Plus(
            new Main.Plus(new Main.Arg(0), new Main.Arg(1)),
            new Main.Arg(2)
        );
        Main.MAIN.setProgram(program);

        int count;
        if (Boolean.getBoolean("noigv")) {
            // Skip warmup if IGV dump isn't requested
            count = 1;
        } else {
            count = 10000000;
        }
        for (int i = 0; i < count; i++) {
            sayHelloTruffle();
        }
    }

    @Test
    public void checkSayHello() {
        Assert.assertEquals(7 + 8 + 2, sayHelloTruffle());
    }

    private static Object sayHelloTruffle() {
        final Object arr = new int[] { 7, 8, 2, 4 };
        return Main.CODE.call(arr);
    }
}
