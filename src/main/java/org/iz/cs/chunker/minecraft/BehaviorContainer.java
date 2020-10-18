package org.iz.cs.chunker.minecraft;

import java.util.Map;


public abstract class BehaviorContainer {

    @SuppressWarnings("rawtypes")
    public abstract Map<String, Class> getBehaviors();

}
