package org.apidesign.demo.talk2compiler;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeCast;
import com.oracle.truffle.api.dsl.TypeCheck;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.Node;
import org.apidesign.demo.talk2compiler.MainFactory.GetArrayElementNodeGen;

// In this example we are going to show how one can create custom
// OOP style abstractions in Truffle intepreters.

public class Main extends RootNode {
    static final Main MAIN;
    
    static Plus newPlus(Expression l, Expression r) {
        return MainFactory.PlusNodeGen.create(l, r);
    }
    
    static {
        MAIN = new Main(GetArrayElementNodeGen.create(new CreateArrayNode(), new Index(0)));
    }
    static final CallTarget CODE = Truffle.getRuntime().createCallTarget(MAIN);

    @Child private Expression program;

    private Main(Expression program) {
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
    
    // In the spirit of the traditional OOP approach we define an abstract
    // class that represents an array in our language. We would like to
    // have several data structures that implement the protocol defined
    // by this abstract class
    public static abstract class AbstractArray {
        public abstract Object getItem(int index);
    }
    
    // The most generic implementation uses actual array
    public static final class JavaArray extends AbstractArray {
        private final Object data[];

        public JavaArray(Object[] data) {
            this.data = data;
        }

        @Override
        public Object getItem(int index) {
            return data[index];
        }
    }
    
    // One of the possible optimized implementations. Many languages
    // have constructs to create a seqence of whole numbers with some
    // start, length and stride. For example 1, 2, 3, ... 10.
    public static final class IntegerSequence extends AbstractArray {
        final int start;
        final int length;
        final int stride;

        public IntegerSequence(int start, int length, int stride) {
            this.start = start;
            this.length = length;
            this.stride = stride;
        }
        
        @Override
        public Object getItem(int index) {
            assert index < length;
            return start + index * stride;
        }
    }
    
    public static abstract class GetArrayElementNode extends Expression {
        @Child Expression arrayExpression;
        @Child Expression indexExpression;

        public GetArrayElementNode(Expression arrayExpression, Expression indexExpression) {
            this.arrayExpression = arrayExpression;
            this.indexExpression = indexExpression;
        }                
        
        @Override
        public final Object executeEval(VirtualFrame frame) {
            Object array = arrayExpression.executeEval(frame);
            Object index = indexExpression.executeEval(frame);
            if (!(index instanceof Integer)) {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException();
            }
            if (!(array instanceof AbstractArray)) {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException();                
            }
            // To "devirtualize" the call, we use Truffle DSL and Specializations
            return executeInternal((AbstractArray) array, (Integer) index);
        }

        abstract Object executeInternal(AbstractArray abstractArray, int integer);
        
        // This is a more generic approach that uses Truffle DSL features
        // The drawbacks of this are:
        //   - the generated code for this is little bit less efficinet in the intepreter mode
        //   - we still cannot use Truffle nodes or profiles in the AbstractArray implementations
        //     because they are not Truffle nodes, i.e., are not attached to any Truffle AST
        @Specialization(guards = "cachedClass == a.getClass()", limit = "2")
        Object doArrayCached(AbstractArray a, int i,
                @Cached("a.getClass()") Class<? extends AbstractArray> cachedClass) {
            return cachedClass.cast(a).getItem(i);
        }
        
        @Specialization(replaces = "doArrayCached")
        Object doArrayGeneric(AbstractArray a, int i) {
            return doArrayCached(a, i, a.getClass());
        }
    }
    
    public static final class CreateArrayNode extends Expression {
        @Override
        public Object executeEval(VirtualFrame frame) {
            // In order to introduce some polymorphism we use an if on something "dynamic"
            if (frame.getArguments().length > 5) {
                return new JavaArray(new Object[] {1, 2, 3, 11});
            }
            return new IntegerSequence(1, 10, 1);
        }
        
    }
}
