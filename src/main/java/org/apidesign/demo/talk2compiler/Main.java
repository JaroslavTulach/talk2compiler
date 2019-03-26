package org.apidesign.demo.talk2compiler;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

public class Main extends RootNode {
    static final Main MAIN = new Main();
    static final CallTarget CODE = Truffle.getRuntime().createCallTarget(MAIN);

    @Child
    private Compute compute;

    private Main() {
        super(null);
    }

    public static void main(String... args) {
        String who = args.length > 0 ? args[0] : "unknown";
        System.err.println(CODE.call(who));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final int[] name = (int[]) frame.getArguments()[0];
        return compute.compute(name);
    }

    void setProgram(Compute program) {
        compute = insert(program);
    }

    public static abstract class Compute extends Node {
        public abstract int compute(int[] arr);
    }

    public static final class Plus extends Compute {
        @Child Compute left;
        @Child Compute right;

        public Plus(Compute left, Compute right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public int compute(int[] arr) {
            int leftValue = left.compute(arr);
            int rightValue = right.compute(arr);
            return leftValue + rightValue;
        }
    }

    public static final class Arg extends Compute {
        private final int index;

        public Arg(int index) {
            this.index = index;
        }

        @Override
        public int compute(int[] arr) {
            return arr[index];
        }
    }
}
