package org.iz.cs.chunker.minecraft.impl;

import static org.iz.cs.chunker.minecraft.BehaviorManager.BehaviorName.MAP_DIMENSION;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.iz.cs.chunker.Chunker;
import org.iz.cs.chunker.minecraft.Behavior;
import org.iz.cs.chunker.minecraft.BehaviorContainer;
import org.iz.cs.chunker.minecraft.Constants;

public class GetLevelKey extends BehaviorContainer {

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, Class> getBehaviors() {
        Map<String, Class> result = new HashMap<>();
        result.put("20w22a", I_20w22a.class);
        result.put("20w20b", I_20w20b.class);
        result.put("1.14.4", I_1_14_4.class);
        return result;
    }

    public static class I_20w22a extends Behavior<String, Object> {

        @Override
        public boolean checkMappings() {
            validateClassMapping(Constants.LEVEL_CN);
            validateClassMapping(Constants.RESOURCE_KEY_CN);
            for (String dimension : Chunker.DIMENSIONS) {
                validateFieldMapping(Constants.LEVEL_CN, (String) applyOther(MAP_DIMENSION, dimension));
            }
            return true;
        }

        @Override
        public boolean checkClasses() {
            validateClass(Constants.LEVEL_CN);
            validateClass(Constants.RESOURCE_KEY_CN);
            for (String dimension : Chunker.DIMENSIONS) {
                validateField(Constants.LEVEL_CN, (String) applyOther(MAP_DIMENSION, dimension));
            }
            return true;
        }

        @Override
        public Object apply(String dimension) {
            try {
                Field level_dimension_f = classCache.get(Constants.LEVEL_CN)
                        .getDeclaredField(mapping.getField(Constants.LEVEL_CN, (String) applyOther(MAP_DIMENSION, dimension)));

                return level_dimension_f.get(null);
            } catch (IllegalAccessException
                    | IllegalArgumentException
                    | NoSuchFieldException
                    | SecurityException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class I_20w20b extends Behavior<String, Object> {

        String DIMENSION_TYPE_CL = "net.minecraft.world.level.dimension.DimensionType";

        @Override
        public boolean checkMappings() {
            validateClassMapping(DIMENSION_TYPE_CL);
            for (String dimension : Chunker.DIMENSIONS) {
                validateFieldMapping(DIMENSION_TYPE_CL, (String) applyOther(MAP_DIMENSION, dimension));
            }
            return true;
        }

        @Override
        public boolean checkClasses() {
            validateClass(DIMENSION_TYPE_CL);
            for (String dimension : Chunker.DIMENSIONS) {
                validateField(DIMENSION_TYPE_CL, (String) applyOther(MAP_DIMENSION, dimension));
            }
            return true;
        }

        @Override
        public Object apply(String dimension) {
            try {
                Field level_dimension_f = classCache.get(DIMENSION_TYPE_CL)
                        .getDeclaredField(mapping.getField(DIMENSION_TYPE_CL, (String) applyOther(MAP_DIMENSION, dimension)));

                return level_dimension_f.get(null);
            } catch (IllegalAccessException
                    | IllegalArgumentException
                    | NoSuchFieldException
                    | SecurityException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class I_1_14_4 extends Behavior<String, Object> {

        String DIMENSION_TYPE_CL = "net.minecraft.world.level.dimension.DimensionType";

        @Override
        public boolean checkMappings() {
            validateClassMapping(DIMENSION_TYPE_CL);
            for (String dimension : Chunker.DIMENSIONS) {
                validateFieldMapping(DIMENSION_TYPE_CL, (String) applyOther(MAP_DIMENSION, dimension));
            }
            return true;
        }

        @Override
        public boolean checkClasses() {
            validateClass(DIMENSION_TYPE_CL);
            for (String dimension : Chunker.DIMENSIONS) {
                validateField(DIMENSION_TYPE_CL, (String) applyOther(MAP_DIMENSION, dimension));
            }
            return true;
        }

        @Override
        public Object apply(String dimension) {
            try {
                Field level_dimension_f = classCache.get(DIMENSION_TYPE_CL)
                        .getDeclaredField(mapping.getField(DIMENSION_TYPE_CL, (String) applyOther(MAP_DIMENSION, dimension)));
                return level_dimension_f.get(null);
            } catch (IllegalAccessException
                    | IllegalArgumentException
                    | NoSuchFieldException
                    | SecurityException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
