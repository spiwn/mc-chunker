package org.iz.cs.chunker.minecraft.impl;

import java.util.HashMap;
import java.util.Map;

import org.iz.cs.chunker.minecraft.Behavior;
import org.iz.cs.chunker.minecraft.BehaviorContainer;
import org.iz.cs.chunker.minecraft.Constants;

public class GetDedicatedServerClassName extends BehaviorContainer {

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, Class> getBehaviors() {
        Map<String, Class> result = new HashMap<>();
        result.put("1.15.2", I_1_15_2.class);
        return result;
    }

    public static class I_1_15_2 extends Behavior<Void, String> {

        @Override
        public boolean checkMappings() {
            validateClassMapping(Constants.DEDICATED_SERVER_CN);
            return true;
        }

        @Override
        public String apply(Void t) {
            return mapping.getClassName(Constants.DEDICATED_SERVER_CN);
        }
    }

}
