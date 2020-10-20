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
                return isReady_f.getBoolean(applyOther(GET_DEDICATED_SERVER_INSTANCE, null));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

    }
}
