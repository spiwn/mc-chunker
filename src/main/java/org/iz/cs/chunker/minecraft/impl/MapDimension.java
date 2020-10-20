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
