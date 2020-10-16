package org.iz.cs.chunker.minecraft.impl;

import java.lang.reflect.Field;

import org.iz.cs.chunker.minecraft.Behavior;
import org.iz.cs.chunker.minecraft.Constants;

public interface GetDedicatedServerInstance {

    public static class I_1_15_2 extends Behavior<Void, Object> {

        private Field instanceField;

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
