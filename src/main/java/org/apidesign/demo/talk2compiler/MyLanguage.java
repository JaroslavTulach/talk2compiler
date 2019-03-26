package org.apidesign.demo.talk2compiler;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;

@TruffleLanguage.Registration(id="my", name = "my")
public class MyLanguage extends TruffleLanguage<Data> {

    @Override
    protected Data createContext(Env env) {
        return new Data(env);
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        return super.parse(request);
    }
}

class Data {

    private final TruffleLanguage.Env env;

    Data(TruffleLanguage.Env env) {
        this.env = env;
    }
}