package org.apidesign.demo.talk2compiler;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeCast;
import com.oracle.truffle.api.dsl.TypeCheck;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.Node;
import java.util.Set;

// In this example we are going to rewrite our original code to be structured
// as a typical structural programming language into statements and expressions.
// This will demonstrate how to deal with a control flow in Truffle AST based
// interpreters.

public class Main extends RootNode {
    static final Main MAIN;
    
    static Plus newPlus(Expression l, Expression r) {
        return MainFactory.PlusNodeGen.create(l, r);
    }
    
    static {
        FrameDescriptor.Builder fdBuilder = FrameDescriptor.newBuilder();
        int aIndex = fdBuilder.addSlot(FrameSlotKind.Object, "a", null);        
        
        MAIN = new Main(new BlockStatement(new Statement[]{
                new WriteVariable(aIndex, new Index(0)),
                new Loop(aIndex, 
                        new WriteVariable(aIndex, 
                                newPlus(new ReadVariable(aIndex), new Index(0)))),
                new ReturnStatement(new ReadVariable(aIndex)),
        }), fdBuilder.build());
    }
    static final CallTarget CODE = Truffle.getRuntime().createCallTarget(MAIN);

    @Child private Statement program;

    private Main(Statement program, FrameDescriptor fd) {
        super(null, fd);
        this.program = program;
    }

    public static void main(String... args) {
        System.err.println(CODE.call((Object) new Number[] { 5, Math.PI, 11, 15 }));
    }

    @Override
    public Object execute(VirtualFrame frame) {       
        try {
            program.executeStatement(frame);
        } catch (ReturnException ex) {
            return ex.result;
        }
        return null;
    }

    // Note: Compute was renamed to Expression
    public static abstract class Expression extends Node {
        public abstract Object executeEval(VirtualFrame frame);
    }
    
    @TypeSystem({ int.class , double.class, Undefined.class })
    public static class NumericTypeSystem {
        @ImplicitCast
        public static double implicitInt2Double(int value) {
            return value;
        }
        
        // Whenever Truffle DSL generated code needs to check if an Object
        // is of type Undefined, it will use this method instead of `instanceof`.
        // In this method we exploit the fact that Undefined is a singleton.
        // Checking the reference against some well-known value is tiny bit faster
        // than reading the type information from it and comparing that.
        @TypeCheck(Undefined.class)
        public static boolean undefCheck(Object value) {
            return value == Undefined.INSTANCE;
        }
        
        // Likewise standard Java casting requires loading the type information
        // to check if the cast is safe. Knowing that Undefined is a singleton,
        // we can avoid that extra work.
        @TypeCast(Undefined.class)
        public static Undefined asUndefined(Object value) {
            return Undefined.INSTANCE;
        }
    }

    @TypeSystemReference(NumericTypeSystem.class)
    public static abstract class Plus extends Expression {
        @Child Expression left;
        @Child Expression right;
        
        public Plus(Expression left, Expression right) {
            this.left = left;
            this.right = right;
        }
        
        public final Object executeEval(VirtualFrame frame) {
            return executeInternal(left.executeEval(frame), right.executeEval(frame));
        }
        
        abstract Object executeInternal(Object left, Object right);
        
        @Specialization
        Number doII(int left, int right) {
            return left + right;
        }
        
        @Specialization
        Number doDD(double left, double right) {
            return left + right;
        }
        
        @Specialization
        Undefined doLeftUndef(Undefined left, Object right) {
            return Undefined.INSTANCE;
        }        
        
        @Specialization
        Undefined doRightUndef(Object left, Undefined right) {
            return Undefined.INSTANCE;
        }        
        
        @Fallback
        Object doFallback(Object leftValue, Object rightValue) {            
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("Cannot + for " + leftValue + " and " + rightValue);
        }
    }

    public static final class Index extends Expression {
        private final int index;

        public Index(int index) {
            this.index = index;
        }

        @Override
        public Object executeEval(VirtualFrame frame) {
            return frame.getArguments()[index];
        }
    }
        
    public static final class ReturnException extends ControlFlowException {
        public final Object result;
        
        public ReturnException(Object result) {
            this.result = result;
        }
    }
    
    // Statement represents some action that does not produce a value
    // In order for a statement to be useful, it should do some side
    // effects, for example, write into a local/global variable.
    public static abstract class Statement extends Node {
        public abstract void executeStatement(VirtualFrame frame) throws ReturnException;
    }
    
    // Throwing a ReturnException allows us to easily jump to the
    // outer try { ... } catch (ReturnException) { ... } block.
    public static final class ReturnStatement extends Statement {
        @Child Expression value;
        
        public ReturnStatement(Expression value) {
            this.value = value;
        }
        
        @Override
        public void executeStatement(VirtualFrame frame) {
            throw new ReturnException(value.executeEval(frame));
        }
    }
    
    // Block statement represents `{ ... }` in programming languages with
    // C inspired syntax. It is a wrapper for an array of other statements,
    // which should be sequentially executed
    //
    // Note that we must use `@ExplodeLoop`, otherwise the compiler cannot
    // infer the "constantness" of `s` within the loop (it's coming from
    // a @Children array, which is implicitly @CompilationFinal(dimensions=1))
    // and hence cannot inline the executeStatement call.
    public static final class BlockStatement extends Statement {
        @Children Statement statements[];
        
        public BlockStatement(Statement statements[]) {
            this.statements = statements;
        }
        
        @Override
        @ExplodeLoop
        public void executeStatement(VirtualFrame frame) {
            for(Statement s : statements) {
                s.executeStatement(frame);
            }
        }
    }
    
    // For reading/writing local variables we are going to use the VirtualFrame
    // facility provided by Truffle framework. Each guest language function
    // (i.e., RootNode) has FrameDescriptor attached to it. FrameDescriptor
    // describes the layout of the VirtualFrame instances (for example, there
    // are local variables "a", "b" and "c"). VirtualFrame holds the actual
    // values of those local variables.
    
    // Take a look at how those writes/reads from the VirtualFrame
    // get translated into Graal nodes in IGV. The integer value the flows
    // in and out of the frame gets boxed. Can we do better?
    
    @ImportStatic(FrameSlotKind.class)
    public static final class WriteVariable extends Statement {
        private final int index;
        @Child Expression compute;

        public WriteVariable(int index, Expression compute) {
            this.index = index;
            this.compute = compute;
        }               

        public void executeStatement(VirtualFrame frame) {
            Object value = compute.executeEval(frame);
            frame.setObject(index, value);
        }
    }
    
    @ImportStatic(FrameSlotKind.class)
    public static final class ReadVariable extends Expression {
        private final int index;

        public ReadVariable(int index) {
            this.index = index;
        }

        public Object executeEval(VirtualFrame frame) {
            return frame.getObject(index);
        }
    }
    
    // For our example we hard-code the loop condition to keep things simple...
    @ImportStatic(FrameSlotKind.class)
    public static final class Loop extends Statement {
        @Child ReadVariable read;
        @Child Statement body;
        
        public Loop(int controlVarIndex, Statement body) {
            this.read = new ReadVariable(controlVarIndex);
            this.body = body;
        }
        
        @Override
        public void executeStatement(VirtualFrame frame) {
            while (!Integer.valueOf(10).equals(read.executeEval(frame))) {
                body.executeStatement(frame);
            }
        }
    }   
}
