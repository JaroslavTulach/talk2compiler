package org.apidesign.demo.talk2compiler;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class Main extends RootNode {
    private final int mul;
    final CallTarget CODE = Truffle.getRuntime().createCallTarget(this);

    public Main(int mul) {
        super(null);
        this.mul = mul;
    }

    public static void main(String... args) {
    }

    @Override
    public Object execute(VirtualFrame frame) {
        int[] arr = (int[]) frame.getArguments()[0];
        return mulAndSum(arr);
    }

    Object mulAndSum(int[] arr) {
        int sum = 0;
        for (int i = 0; i < arr.length; i++) {
            int value = arr[i];
            sum += mul * value;
        }
        return sum;
    }
}
