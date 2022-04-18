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
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.GenerateLibrary;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.Node;
import org.apidesign.demo.talk2compiler.MainFactory.GetArrayElementNodeGen;
import com.oracle.truffle.api.profiles.ValueProfile;

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
    
    public static abstract class AbstractArray {}

    
    // We are using Truffle Libraries: each array implementation will
    // get a generated "library": a Truffle node that handles the abstract
    // operations defined here. The abstract class AbstractArray now serves
    // as only a marker.
    @GenerateLibrary
    public static abstract class MyArrayLibrary extends Library {        
        public abstract Object getItem(AbstractArray self, int index);        
    }
    
    // The most generic implementation uses actual array
    @ExportLibrary(MyArrayLibrary.class)
    public static final class JavaArray extends AbstractArray {
        private final Object data[];

        public JavaArray(Object[] data) {
            this.data = data;
        }

        // By using @ExportMessage we implement the library messages
        // Try finding the invocations of this method in your IDE 
        // (after the code was compiled and Truffle DSL could generate
        // the implmentation of MyArrayLibrary for JavaArray)
        @ExportMessage
        public Object getItem(int index) {
            return data[index];
        }
    }
    
    // One of the possible optimized implementations. Many languages
    // have constructs to create a seqence of whole numbers with some
    // start, length and stride. For example 1, 2, 3, ... 10.
    @ExportLibrary(MyArrayLibrary.class)
    public static final class IntegerSequence extends AbstractArray {
        final int start;
        final int length;
        final int stride;

        public IntegerSequence(int start, int length, int stride) {
            this.start = start;
            this.length = length;
            this.stride = stride;
        }
        
        // The advantage of Truffle libraries: one can use @Cached and
        // other annotations.
        @ExportMessage
        public Object getItem(int index,
                @Cached("createIdentityProfile()") ValueProfile profile) {
            assert index < length;
            return start + index * profile.profile(stride);
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
            return executeInternal((AbstractArray) array, (Integer) index);
        }

        abstract Object executeInternal(AbstractArray abstractArray, int integer);
        
        // This is how the library can be used:
        // Take a look at the generated code. How does it lookup the library
        // implementation based on the type of a? The drawback of Truffle
        // libraries is that they are too generic and, for example, this
        // lookup is expensive for startup performance (first executions before
        // the AST stabilized). At this moment Truffle libraries are advisable
        // over manual specializations for Truffle PE aware services that can
        // be implemented by 3rd parties, such as the interop between languages
        // in the next commit...
        //
        // Try to change the limit to "1", what do you see in IGV now?
        @Specialization(limit = "2")
        Object doArray(AbstractArray a, int i,
                @CachedLibrary("a") MyArrayLibrary aLib) {
            return aLib.getItem(a, i);
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
