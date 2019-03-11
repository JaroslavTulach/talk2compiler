package org.apidesign.demo.talk2compiler;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;
import java.util.HashMap;
import java.util.Map;

public class Main extends RootNode {
    static final Main MAIN = new Main();
    static final CallTarget CODE = Truffle.getRuntime().createCallTarget(MAIN);

    private final Map<String, Integer> ids = new HashMap<>();

    @CompilerDirectives.CompilationFinal
    private Chain cache;

    private Main() {
        super(null);
    }

    public void addMapping(String text, int id) {
        ids.put(text, id);
    }

    public static void main(String... args) {
        String who = args.length > 0 ? args[0] : "unknown";
        System.err.println(CODE.call(who));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final String value = (String) frame.getArguments()[0];
        return findInChainAndThenId(value);
    }

    @ExplodeLoop
    private Integer findInChainAndThenId(String key) {
        Chain item = cache;
        while (item != null) {
            if (item.key == key) {
                return item.value;
            }
            item = item.next;
        }
        Integer value = findId(key);
        if (cache == null || cache.count < 2) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cache = new Chain(key, value, cache);
        }
        return value;
    }

    @TruffleBoundary
    private Integer findId(String name) {
        return ids.get(name);
    }

    private static final class Chain {
        public final String key;
        public final Integer value;
        public final Chain next;
        public final int count;

        Chain(String key, Integer value, Chain next) {
            this.key = key;
            this.value = value;
            this.next = next;
            this.count = next == null ? 1 : next.count + 1;
        }
    }
}
