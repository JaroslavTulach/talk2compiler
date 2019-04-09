package org.apidesign.demo.talk2compiler;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.dsl.TypeSystemReference;
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
        return compute.execute(frame);
    }

    void setProgram(Compute program) {
        compute = insert(program);
    }

    @TypeSystem(value = {int.class, double.class })
    public static class ArgTypes {
        @ImplicitCast
        public static double toDouble(int x) {
            return x;
        }
    }

    @TypeSystemReference(ArgTypes.class)
    public static abstract class Compute extends Node {
        public abstract Object execute(VirtualFrame vf);
    }

    @NodeChildren({
        @NodeChild(value = "left"),
        @NodeChild(value = "right"),
    })
    public static abstract class Plus extends Compute {

        @Specialization
        public int executeInt(int leftValue, int rightValue) {
            return leftValue + rightValue;
        }

        @Specialization(replaces = "executeInt")
        public double execute(double leftValue, double rightValue) {
            return leftValue + rightValue;
        }
    }

    public static final class Arg extends Compute {
        private final int index;

        public Arg(int index) {
            this.index = index;
        }

        @Override
        public Object execute(VirtualFrame vf) {
            final Object[] arr = (Object[]) vf.getArguments()[0];
            Object res = arr[index];
            return res;
        }
    }
}
