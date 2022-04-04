package org.apidesign.demo.talk2compiler;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;

public class Main extends RootNode {
    static final Main MAIN;
    
    static Plus newPlus(Compute l, Compute r) {
        return MainFactory.PlusNodeGen.create(l, r);
    }
    
    static {
        Compute p = newPlus(new Index(0),
            newPlus(new Index(2), new Index(1))
        );
        MAIN = new Main(p);
    }
    static final CallTarget CODE = Truffle.getRuntime().createCallTarget(MAIN);

    @Child private Compute program;

    private Main(Compute program) {
        super(null);
        this.program = program;
    }

    public static void main(String... args) {
        System.err.println(CODE.call((Object) new Number[] { 5, Math.PI, 11, 15 }));
    }

    @Override
    public Object execute(VirtualFrame frame) {       
        return program.executeEval(frame);
    }

    public static abstract class Compute extends Node {
        public abstract Object executeEval(VirtualFrame frame);
    }

    @NodeChildren({
            @NodeChild(value = "left"),
            @NodeChild(value = "right")})
    public static abstract class Plus extends Compute {        
        @Specialization
        Number doII(int left, int right) {
            return left + right;
        }
        
        @Specialization
        Number doDI(double left, int right) {
            return left + right;
        }
        
        @Specialization
        Number doID(int left, double right) {
            return left + right;
        }
        
        @Specialization
        Number doDD(double left, double right) {
            return left + right;
        }
        
        @Fallback
        Object doFallback(Object leftValue, Object rightValue) {            
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("Cannot + for " + leftValue + " and " + rightValue);
        }
    }

    public static final class Index extends Compute {
        private final int index;

        public Index(int index) {
            this.index = index;
        }

        @Override
        public Number executeEval(VirtualFrame frame) {
            return (Number) frame.getArguments()[index];
        }
    }
}
