/*
 * Copyright 2020 Ivan Zhivkov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.iz.cs.chunker.minecraft.impl;

import static org.iz.cs.chunker.minecraft.BehaviorManager.BehaviorName.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.iz.cs.chunker.minecraft.Behavior;
import org.iz.cs.chunker.minecraft.BehaviorContainer;
import org.iz.cs.chunker.minecraft.Constants;

public class GetLevel extends BehaviorContainer {

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, Class> getBehaviors() {
        Map<String, Class> result = new HashMap<>();
        result.put("20w22a", I_20w22a.class);
        result.put("20w21a", I_20w21a.class);
        result.put("1.14.4", I_1_14_4.class);
        return result;
    }

    public static class I_20w22a extends Behavior<String, Object> {

        private Method getLevel_m;
        private Map<String, Object> levelCache;

        @Override
        public boolean checkMappings() {
            validateClassMapping(Constants.LEVEL_CN);
            validateClassMapping(Constants.RESOURCE_KEY_CN);
            validateClassMapping(Constants.MINECRAFT_SERVER_CN);
            validateMethodMapping(
                    Constants.MINECRAFT_SERVER_CN,
                    Constants.GET_LEVEL_M,
                    Constants.RESOURCE_KEY_CN);
            return true;
        }

        @Override
        public boolean checkClasses() {
            validateClass(Constants.LEVEL_CN);
            validateClass(Constants.RESOURCE_KEY_CN);
            validateClass(Constants.MINECRAFT_SERVER_CN);
            validateMethod(
                    Constants.MINECRAFT_SERVER_CN,
                    Constants.GET_LEVEL_M,
                    Constants.RESOURCE_KEY_CN);
            return true;
        }

        @Override
        protected void bootstrap() {
            levelCache = new HashMap<>();
            try {
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
        public Object apply(String dimension) {
            Object level = levelCache.get(dimension);
            if (level == null) {
                try {
                    level = getLevel_m.invoke(
                            applyOther(GET_DEDICATED_SERVER_INSTANCE, null),
                            applyOther(GET_LEVEL_KEY, dimension));
                    if (level == null) {
                        throw new IllegalStateException("Server did not load level " + dimension);
                    }
                    levelCache.put(dimension, level);
                } catch (IllegalAccessException
                        | IllegalArgumentException
                        | InvocationTargetException
                        | SecurityException e) {
                    throw new RuntimeException(e);
                }
            }

            return level;
        }
    }

    public static class I_20w21a extends Behavior<String, Object> {

        String DIMENSION_TYPE_CL = "net.minecraft.world.level.dimension.DimensionType";

        private Method getLevel_m;
        private Map<String, Object> levelCache;

        @Override
        public boolean checkMappings() {
            validateClassMapping(Constants.RESOURCE_KEY_CN);
            validateClassMapping(Constants.MINECRAFT_SERVER_CN);
            validateMethodMapping(
                    Constants.MINECRAFT_SERVER_CN,
                    Constants.GET_LEVEL_M,
                    Constants.RESOURCE_KEY_CN);
            return true;
        }

        @Override
        public boolean checkClasses() {
            validateClass(Constants.RESOURCE_KEY_CN);
            validateClass(Constants.MINECRAFT_SERVER_CN);
            validateMethod(
                    Constants.MINECRAFT_SERVER_CN,
                    Constants.GET_LEVEL_M,
                    Constants.RESOURCE_KEY_CN);
            return true;
        }

        @Override
        protected void bootstrap() {
            levelCache = new HashMap<>();
            try {
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
        public Object apply(String dimension) {
            Object level = levelCache.get(dimension);
            if (level == null) {
                try {
                    level = getLevel_m.invoke(
                            applyOther(GET_DEDICATED_SERVER_INSTANCE, null),
                            applyOther(GET_LEVEL_KEY, dimension));
                    if (level == null) {
                        throw new IllegalStateException("Server did not load level " + dimension);
                    }
                    levelCache.put(dimension, level);
                } catch (IllegalAccessException
                        | IllegalArgumentException
                        | InvocationTargetException
                        | SecurityException e) {
                    throw new RuntimeException(e);
                }
            }

            return level;
        }
    }

    public static class I_1_14_4 extends Behavior<String, Object> {

        String DIMENSION_TYPE_CL = "net.minecraft.world.level.dimension.DimensionType";

        private Method getLevel_m;
        private Map<String, Object> levelCache;

        @Override
        public boolean checkMappings() {
            validateClassMapping(DIMENSION_TYPE_CL);
            validateClassMapping(Constants.MINECRAFT_SERVER_CN);
            validateMethodMapping(
                    Constants.MINECRAFT_SERVER_CN,
                    Constants.GET_LEVEL_M,
                    DIMENSION_TYPE_CL);
            return true;
        }

        @Override
        public boolean checkClasses() {
            validateClass(DIMENSION_TYPE_CL);
            validateClass(Constants.MINECRAFT_SERVER_CN);
            validateMethod(
                    Constants.MINECRAFT_SERVER_CN,
                    Constants.GET_LEVEL_M,
                    DIMENSION_TYPE_CL);
            return true;
        }

        @Override
        protected void bootstrap() {
            levelCache = new HashMap<>();
            try {
                Class<?> mc_s_cl = classCache.get(Constants.MINECRAFT_SERVER_CN);
                Class<?> rk_cl = classCache.get(DIMENSION_TYPE_CL);

                getLevel_m = mc_s_cl.getMethod(
                        mapping.getMethod(
                                Constants.MINECRAFT_SERVER_CN,
                                Constants.GET_LEVEL_M,
                                DIMENSION_TYPE_CL),
                        rk_cl);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Object apply(String dimension) {
            Object level = levelCache.get(dimension);
            if (level == null) {
                try {
                    level = getLevel_m.invoke(
                            applyOther(GET_DEDICATED_SERVER_INSTANCE, null),
                            applyOther(GET_LEVEL_KEY, dimension));
                    if (level == null) {
                        throw new IllegalStateException("Server did not load level " + dimension);
                    }
                    levelCache.put(dimension, level);
                } catch (IllegalAccessException
                        | IllegalArgumentException
                        | InvocationTargetException
                        | SecurityException e) {
                    throw new RuntimeException(e);
                }
            }

            return level;
        }
    }
}
