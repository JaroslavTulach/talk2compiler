package org.apidesign.demo.talk2compiler;

import org.apidesign.demo.talk2compiler.bn.BooleanNetwork;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MainTest {
    private static BooleanNetwork network;

    @BeforeClass
    public static void prepareNetwork() {
        network = BooleanNetwork.generate();
    }

    @Before
    public void warmingUp() {
        int count;
        if (Boolean.getBoolean("noigv")) {
            // Skip warmup if IGV dump isn't requested
            count = 1;
        } else {
            count = 100000;
        }

        Main.HOTSPOT.measureSearchTime(network, count);
        for (int i = 0; i < count; i++) {
            searchNetwork(1);
        }
    }

    @Test
    public void checkSayHello() {
        searchNetwork(5);
    }

    private static long searchNetwork(int times) {
        return (Long) Main.CODE.call(network, times);
    }
}
