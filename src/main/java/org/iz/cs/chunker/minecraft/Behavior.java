package org.iz.cs.chunker.minecraft;

import java.util.function.Function;

import org.iz.cs.chunker.Mapping;

public abstract class Behavior<T, R> implements Function<T, R> {


    protected BehaviorManager behaviorManager;
    protected ClassCache classCache;
    protected Mapping mapping;

    protected Behavior() {
        //super();
    }

    protected void bootstrap() {

    }

    void init(BehaviorManager behaviorManager, ClassCache classCache, Mapping mapping) {
        this.behaviorManager = behaviorManager;
        this.classCache = classCache;
        this.mapping = mapping;
        bootstrap();
    }

}
