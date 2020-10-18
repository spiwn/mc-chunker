package org.iz.cs.chunker.minecraft.impl;

import static org.iz.cs.chunker.minecraft.BehaviorManager.BehaviorName.GET_DEDICATED_SERVER_INSTANCE;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.iz.cs.chunker.minecraft.Behavior;
import org.iz.cs.chunker.minecraft.BehaviorContainer;
import org.iz.cs.chunker.minecraft.Constants;

public class IsServerReady extends BehaviorContainer {

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, Class> getBehaviors() {
        Map<String, Class> result = new HashMap<>();
        result.put("1.14.4", I_1_14_4.class);
        return result;
    }

    public static class I_1_14_4 extends Behavior<Void, Boolean> {

        @Override
        public boolean checkMappings() {
            validateClassMapping(Constants.MINECRAFT_SERVER_CN);
            validateFieldMapping(Constants.MINECRAFT_SERVER_CN, Constants.IS_READY_F);
            return true;
        }

        @Override
        public boolean checkClasses() {
            validateClass(Constants.MINECRAFT_SERVER_CN);
            validateField(Constants.MINECRAFT_SERVER_CN, Constants.IS_READY_F);
            return true;
        }


        private Class<?> mc_server_cl;
        private Field isReady_f;

        @Override
        protected void bootstrap() {
            mc_server_cl = classCache.get(Constants.MINECRAFT_SERVER_CN);
            try {
                isReady_f = mc_server_cl.getDeclaredField(mapping.getField(Constants.MINECRAFT_SERVER_CN, Constants.IS_READY_F));
                isReady_f.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Boolean apply(Void v) {
            try {
                Object dedicatedServer = applyOther(GET_DEDICATED_SERVER_INSTANCE, null);
                if (dedicatedServer == null) {
                    server.serverRunning = false;
                    throw new IllegalArgumentException("Server seems to have not been started");
                }
                return isReady_f.getBoolean(dedicatedServer);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

    }
}
