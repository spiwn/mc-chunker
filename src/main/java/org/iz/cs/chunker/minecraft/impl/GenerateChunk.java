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

import static org.iz.cs.chunker.minecraft.BehaviorManager.BehaviorName.GET_LEVEL;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.iz.cs.chunker.minecraft.Behavior;
import org.iz.cs.chunker.minecraft.BehaviorContainer;
import org.iz.cs.chunker.minecraft.Constants;

public class GenerateChunk extends BehaviorContainer {

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, Class> getBehaviors() {
        Map<String, Class> result = new HashMap<>();
        result.put("1.14.4", I_1_14_4.class);
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

    public static class I_1_14_4 extends Behavior<GenerateChunkArguments, Void> {

        private Method getChunk_m;
        private Map<String, Object> levelCache;

        @Override
        public boolean checkMappings() {
            validateClassMapping(Constants.LEVEL_CN);
            validateMethodMapping(Constants.LEVEL_CN, Constants.GET_CHUNK_M, "int", "int");
            return true;
        }

        @Override
        public boolean checkClasses() {
            validateClass(Constants.LEVEL_CN);
            validateMethod(Constants.LEVEL_CN, Constants.GET_CHUNK_M, "int", "int");
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
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Void apply(GenerateChunkArguments t) {
            Object level = levelCache.get(t.dimension);
            if (level == null) {
                try {
                    level = applyOther(GET_LEVEL, t.dimension);
                    if (level == null) {
                        throw new IllegalStateException("Server did not load level " + t.dimension);
                    }
                    levelCache.put(t.dimension, level);
                } catch (IllegalArgumentException
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
