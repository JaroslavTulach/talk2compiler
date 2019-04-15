package org.apidesign.demo.talk2compiler;

import org.graalvm.polyglot.Context;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MainTest {
    interface StdLib {
        Object malloc(int size);
        void free(Object pointer);
        String strdup(String orig);
    }

    @Test
    public void accessNativeLanguage() {
        Context c = Context.create("NativeAccess");

        String stdLibInterface = "default {\n" + //
                        "  strdup(string):string;\n" + //
                        "  malloc(UINT32):pointer;\n" + //
                        "  free(pointer):void;\n" + //
                        "}";

        StdLib stdLib = c.eval("NativeAccess", stdLibInterface).as(StdLib.class);

        Object memory = stdLib.malloc(10);
        stdLib.free(memory);
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
