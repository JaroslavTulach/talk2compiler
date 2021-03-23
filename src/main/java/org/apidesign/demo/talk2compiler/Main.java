package org.apidesign.demo.talk2compiler;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class Main extends RootNode {
    static final Main MAIN = new Main();
    static final CallTarget CODE = Truffle.getRuntime().createCallTarget(MAIN);

    private Main() {
        super(null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final String name = (String) frame.getArguments()[0];
        return formatGreeting("Hello from %s!", name);
    }

    @TruffleBoundary
    private static String formatGreeting(String msg, String name) {
        return String.format(msg, name);
    }

    public static void main(String... args) {
        String who = args.length > 0 ? args[0] : "unknown";
        int cnt;
        if (Boolean.getBoolean("noigv")) {
            cnt = 1;
        } else {
            cnt = args.length > 1 ? Integer.parseInt(args[1]) : 10000000;
        }
        int print = 1;
        for (int i = 1; i <= cnt; i++) {
            final Object result = CODE.call(who);
            if (i >= print) {
                System.err.println("run #" + i + " result: " + result);
                print *= 2;
            }
        }
    }
}
