package org.iz.cs.chunker.minecraft.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.iz.cs.chunker.minecraft.Behavior;
import org.iz.cs.chunker.minecraft.BehaviorContainer;
import org.iz.cs.chunker.minecraft.Constants;

public class GetDedicatedServerInstance extends BehaviorContainer {

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, Class> getBehaviors() {
        Map<String, Class> result = new HashMap<>();
        result.put("1.15.2", I_1_15_2.class);
        return result;
    }

    public static class I_1_15_2 extends Behavior<Void, Object> {

        private Field instanceField;

        @Override
        public boolean checkMappings() {
            validateClassMapping(Constants.DEDICATED_SERVER_CN);
            validateFieldMapping(Constants.DEDICATED_SERVER_CN, Constants.INSTANCE);
            return true;
        }

        @Override
        public boolean checkClasses() {
            validateClass(Constants.DEDICATED_SERVER_CN);
            validateField(Constants.DEDICATED_SERVER_CN, Constants.INSTANCE);
            return true;
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected void bootstrap() {
            try {
                Class dedicatedServerClass = classCache.get(Constants.DEDICATED_SERVER_CN);
                instanceField = dedicatedServerClass.getDeclaredField("instance");
            } catch (NoSuchFieldException | SecurityException e) {
                throw new IllegalStateException("Probably not implemented");
            }
        }

        @Override
        public Object apply(Void t) {
            try {
                return instanceField.get(null);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
