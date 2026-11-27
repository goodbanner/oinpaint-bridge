package com.example.oinpaintbridge;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.jvm.internal.ContinuationImpl;
import kotlin.Unit;

public final class DummyCont extends ContinuationImpl {
    public static volatile Object lastResult = null;

    public DummyCont(Continuation<? super Object> completion) {
        super(completion);
    }

    @Override
    protected Object invokeSuspend(Object result) {
        lastResult = result;
        return Unit.INSTANCE;
    }
}
