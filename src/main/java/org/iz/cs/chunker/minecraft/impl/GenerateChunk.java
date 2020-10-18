package org.iz.cs.chunker.minecraft.impl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.iz.cs.chunker.Chunker;
import org.iz.cs.chunker.minecraft.Behavior;
import org.iz.cs.chunker.minecraft.BehaviorContainer;
import org.iz.cs.chunker.minecraft.Constants;
import org.iz.cs.chunker.minecraft.BehaviorManager.BehaviorName;
import org.iz.cs.chunker.minecraft.CompatibilityException;

public class GenerateChunk extends BehaviorContainer {

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, Class> getBehaviors() {
        Map<String, Class> result = new HashMap<>();
        result.put("1.15.2", I_1_15_2.class);
        return result;
    }

    public static class GenerateChunkArguments {
        String dimension;
        int x;
        int z;

        public GenerateChunkArguments(String dimension, int x, int z) {
            super();
            this.dimension = dimension;
            this.x = x;
            this.z = z;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setZ(int z) {
            this.z = z;
        }
    }

    public static class I_1_15_2 extends Behavior<GenerateChunkArguments, Void> {

        private Method getChunk_m;
        private Method getLevel_m;
        private Map<String, Object> levelCache;

        @Override
        public boolean checkMappings() {
            validateClassMapping(Constants.LEVEL_CN);
            validateClassMapping(Constants.MINECRAFT_SERVER_CN);
            validateMethodMapping(
                    Constants.MINECRAFT_SERVER_CN,
                    Constants.GET_LEVEL_M,
                    Constants.RESOURCE_KEY_CN);
            validateClassMapping(Constants.RESOURCE_KEY_CN);
            for (String dimension:Chunker.DIMENSIONS) {
                validateFieldMapping(Constants.LEVEL_CN, dimension);
            }
            return true;
        }

        @Override
        public boolean checkClasses() {
            validateClass(Constants.LEVEL_CN);
            validateClass(Constants.MINECRAFT_SERVER_CN);
            validateMethod(
                    Constants.MINECRAFT_SERVER_CN,
                    Constants.GET_LEVEL_M,
                    Constants.RESOURCE_KEY_CN);
            validateClass(Constants.RESOURCE_KEY_CN);
            for (String dimension:Chunker.DIMENSIONS) {
                validateField(Constants.LEVEL_CN, dimension);
            }
            return true;
        }

        @Override
        protected void bootstrap() {
            levelCache = new HashMap<>();
            Class<?> level_cl = classCache.get(Constants.LEVEL_CN);
            try {
                getChunk_m = level_cl.getDeclaredMethod(
                        mapping.getMethod(
                                Constants.LEVEL_CN,
                                Constants.GET_CHUNK_M,
                                "int",
                                "int"),
                        int.class,
                        int.class);
                Class<?> mc_s_cl = classCache.get(Constants.MINECRAFT_SERVER_CN);
                Class<?> rk_cl = classCache.get(Constants.RESOURCE_KEY_CN);

                getLevel_m = mc_s_cl.getMethod(
                        mapping.getMethod(
                                Constants.MINECRAFT_SERVER_CN,
                                Constants.GET_LEVEL_M,
                                Constants.RESOURCE_KEY_CN),
                        rk_cl);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Void apply(GenerateChunkArguments t) {
            Object level = levelCache.get(t.dimension);
            if (level == null) {
                try {
                    Field level_dimension_f = classCache.get(Constants.LEVEL_CN)
                            .getDeclaredField(mapping.getField(Constants.LEVEL_CN, t.dimension));

                    Object overworldLevelKey = level_dimension_f.get(null);

                    level = getLevel_m.invoke(
                            behaviorManager.get(
                                    BehaviorName.GET_DEDICATED_SERVER_INSTANCE).apply(null),
                                    overworldLevelKey);
                    if (level == null) {
                        throw new IllegalStateException("Server did not load level " + t.dimension);
                    }
                    levelCache.put(t.dimension, level);
                } catch (IllegalAccessException
                        | IllegalArgumentException
                        | InvocationTargetException
                        | NoSuchFieldException
                        | SecurityException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                getChunk_m.invoke(level, t.x, t.z);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
            return null;
        }
    }
}
