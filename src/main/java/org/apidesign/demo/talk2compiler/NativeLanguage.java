package org.apidesign.demo.talk2compiler;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

@TruffleLanguage.Registration(id="NativeAccess", name = "NativeAccess")
public class NativeLanguage extends TruffleLanguage<Data> {

    @Override
    protected Data createContext(Env env) {
        return new Data(env);
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return false;
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        CharSequence src = request.getSource().getCharacters();
        String name = request.getSource().getName();

        class DelayParserNode extends RootNode {
            DelayParserNode(TruffleLanguage<?> language) {
                super(language);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                return reparse(frame);
            }

            @CompilerDirectives.TruffleBoundary
            private Object reparse(VirtualFrame frame) {
                Data data = getContextReference().get();
                Source nfiSrc = Source.newBuilder("nfi", src, name).build();
                CallTarget code = data.env.parse(nfiSrc);
                Object res = code.call(frame.getArguments());
                return res;
            }
        }
        return Truffle.getRuntime().createCallTarget(new DelayParserNode(this));
    }
}

class Data {

    final TruffleLanguage.Env env;

    Data(TruffleLanguage.Env env) {
        this.env = env;
    }
}