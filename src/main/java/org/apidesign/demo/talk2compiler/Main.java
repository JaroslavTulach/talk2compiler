package org.apidesign.demo.talk2compiler;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.apidesign.demo.talk2compiler.bn.BooleanNetwork;

public class Main extends RootNode {
    private static final Boolean[] SEARCH = {
        false, true, null, null, true,
        null, null, null, null, null,
        null, null, true, null, true,
        null, null, false, null, true,
    };

    static final Main HOTSPOT;
    static final Main TRUFFLE;
    static final CallTarget CODE;
    static {
        SearchAlgorithm alg = new SearchAlgorithm(SEARCH);

        HOTSPOT = new Main("[HotSpot]", alg);
        TRUFFLE = new Main("[Truffle]", alg);
        CODE = Truffle.getRuntime().createCallTarget(TRUFFLE);
    }

    private final String prefix;
    private final SearchAlgorithm alg;

    private Main(String prefix, SearchAlgorithm alg) {
        super(null);
        this.prefix = prefix;
        this.alg = alg;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        BooleanNetwork bn = (BooleanNetwork)frame.getArguments()[0];
        Integer repeat = (Integer)frame.getArguments()[1];
        return measureSearchTime(bn, repeat);
    }

    final long measureSearchTime(BooleanNetwork network, int repeat) {
        long then = System.currentTimeMillis();
        int first = alg.search(network);
        for (int i = 0; i < repeat; i++) {
            int next = alg.search(network);
            assert first == next;
        }
        long sumTime = System.currentTimeMillis() - then;
        printTimeAndResult(prefix, sumTime, repeat <= 1 ? -1 : first);
        return sumTime;
    }

    @CompilerDirectives.TruffleBoundary
    private void printTimeAndResult(String prefix, long sumTime, int first) {
        if (first != -1) {
            System.err.println(prefix + " Took " + sumTime + " ms to find " + first + " nodes in graph");
        }
    }

    private static class SearchAlgorithm {
        private final Boolean[] pattern;

        SearchAlgorithm(Boolean[] pattern) {
            this.pattern = pattern;
        }

        private int search(BooleanNetwork graph) {
            int vars = graph.getVariablesCount();
            BooleanNetwork.StateInfoAccess<BooleanNetwork.State> access = BooleanNetwork.StateInfoAccess.create(BooleanNetwork.State.class);
            BooleanNetwork.State head = null;
            BooleanNetwork.State tail = null;
            for (int i = 0; i < vars; i++) {
                BooleanNetwork.State root = graph.getState(i);
                if (head == null) {
                    head = tail = root;
                } else {
                    access.store(tail, root);
                    tail = root;
                }
            }
            return searchQueue(access, head, tail);
        }

        private int searchQueue(BooleanNetwork.StateInfoAccess<BooleanNetwork.State> access, BooleanNetwork.State head, BooleanNetwork.State tail) {
            int count = 0;
            while (head != null) {
                if (match(head, pattern)) {
                    count++;
                }
                for (BooleanNetwork.State next : head.getSuccessors()) {
                    if (access.get(next) == null && tail != next) {
                        access.store(tail, next);
                        tail = next;
                    }
                }
                head = access.get(head);
            }
            return count;
        }

        private boolean match(BooleanNetwork.State state, Boolean[] pattern) {
            for (int i = 0; i < pattern.length; i++) {
                if (pattern[i] == null) {
                    continue;
                }
                if (state.getValue(i) != pattern[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    public static void main(String... args) throws Exception {
        System.err.println("Generating boolean network...");
        BooleanNetwork network = BooleanNetwork.generate();
        System.err.println("Searching the network...");

        System.err.println("Warmup HotSpot");
        for (int i = 0; i < 10000; i++) {
            HOTSPOT.measureSearchTime(network, 1);
        }
        System.err.println("Check HotSpot");
        HOTSPOT.measureSearchTime(network, 10000);

        System.err.println("Warmup Truffle");
        for (int i = 0; i < 10000; i++) {
            CODE.call(network, 1);
        }
        System.err.println("Check Truffle");
        CODE.call(network, 10000);
    }
}
