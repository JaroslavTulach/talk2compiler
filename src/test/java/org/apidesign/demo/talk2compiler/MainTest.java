package org.apidesign.demo.talk2compiler;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class MainTest {
    private static final Main MUL_BY_3 = new Main(3);

    private class NR implements Iterator<Integer> {
        private final Random generator;
        int remaining;

        NR(long seed, int remaining) {
            this.generator = new Random(seed);
            this.remaining = remaining;
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        public Integer next() {
            if (remaining == 0) {
                throw new NoSuchElementException();
            }
            remaining--;
            return generator.nextInt();
        }

        public int[] toArray() {
            int[] res = new int[remaining];
            for (int i = 0; i < res.length; i++) {
                res[i] = next();
            }
            return res;
        }
    }

    @Before
    public void warmUp() {
        for (int i = 0; i < 1000000; i++) {
            long seed = System.currentTimeMillis();
            Object r1 = MUL_BY_3.CODE.call(new NR(seed, 1000).toArray());
            assertEquals(Integer.class, r1.getClass());
            Object r2 = MUL_BY_3.mulAndSum(new NR(seed, 1000).toArray());
            assertEquals(r1, r2);
        }
    }

    @Test
    public void saySumItAll() {
        final int[] arr = new NR(System.currentTimeMillis(), 1000000).toArray();
        long before = System.currentTimeMillis();
        int fast = -1;
        for (int i = 0; i < 100; i++) {
            fast = (int) MUL_BY_3.CODE.call(arr);
        }
        int slow = -1;
        long middle = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            slow = (int) MUL_BY_3.mulAndSum(arr);
        }
        long then = System.currentTimeMillis();
        assertEquals(slow, fast);

        fail(String.format("Fast %d ms, slow %d ms", middle - before, then - middle));
    }
}
