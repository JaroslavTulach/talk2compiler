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

    public static void main(String... args) {
        String who = args.length > 0 ? args[0] : "unknown";
        System.err.println(CODE.call(who));
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
}
