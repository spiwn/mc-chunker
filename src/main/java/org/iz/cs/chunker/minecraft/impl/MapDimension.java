package org.iz.cs.chunker.minecraft.impl;

import java.util.HashMap;
import java.util.Map;

import org.iz.cs.chunker.minecraft.Behavior;
import org.iz.cs.chunker.minecraft.BehaviorContainer;

public class MapDimension extends BehaviorContainer {

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, Class> getBehaviors() {
        Map<String, Class> result = new HashMap<>();
        result.put("20w22a", I_20w22a.class);
        result.put("20w21a", I_20w21a.class);
        result.put("1.14.4", I_1_14_4.class);
        return result;
    }

    public static class I_20w22a extends Behavior<String, String> {

        @Override
        public String apply(String dimension) {
            return dimension;
        }
    }

    public static class I_20w21a extends Behavior<String, String> {

        private static final String SUFFIX = "_LOCATION";
        @Override
        public String apply(String dimension) {
            return dimension + SUFFIX;
        }
    }

    public static class I_1_14_4 extends Behavior<String, String> {

        private static final String END = "END";
        private static final String THE_END = "THE_" + END;

        @Override
        public String apply(String dimension) {
            if (END.equals(dimension)) {
                return THE_END;
            }
            return dimension;
        }
    }

}
