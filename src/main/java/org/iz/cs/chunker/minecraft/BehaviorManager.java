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
package org.iz.cs.chunker.minecraft;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.iz.cs.chunker.Configuration;
import org.iz.cs.chunker.Mapping;
import org.iz.cs.chunker.minecraft.impl.*;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class BehaviorManager {


    public static enum BehaviorName {
        IS_SERVER_READY(new IsServerReady()),
        GET_DEDICATED_SERVER_CLASS_NAME(new GetDedicatedServerClassName()),
        GET_DEDICATED_SERVER_INSTANCE(new GetDedicatedServerInstance()),
        GENERATE_CHUNK(new GenerateChunk()),
        GET_LEVEL(new GetLevel()),
        GET_LEVEL_KEY(new GetLevelKey()),
        MAP_DIMENSION(new MapDimension()),
        ;

        private BehaviorContainer behaviorContainer;

        private BehaviorName(BehaviorContainer behaviorContainer) {
            this.behaviorContainer = behaviorContainer;
        }

        public BehaviorContainer getBehaviorContainer() {
            return behaviorContainer;
        }

        public Object apply(Object arg, BehaviorManager bm) {
            return bm.get(this).apply(arg);
        }

    }

    private static final BehaviorName[] BEHAVIOR_NAMES = BehaviorName.values();

    private String version;
    private ClassCache classCache;
    private Mapping mapping;
    private ServerInterface serverInterface;

    private Map<BehaviorName, Behavior> behaviorCache;

    public BehaviorManager(String version, ClassCache classCache, Mapping mapping, ServerInterface serverInterface) {
        this.version = version;
        this.classCache = classCache;
        this.mapping = mapping;
        this.serverInterface = serverInterface;
        this.behaviorCache = new EnumMap<>(BehaviorName.class);
    }

    private Behavior get(BehaviorName behaviorName) {
        Behavior result = this.behaviorCache.get(behaviorName);
        if (result == null) {
            Class<? extends Behavior> last = null;
            String lastVersion = null;

            String minVersion = null;
            Class<? extends Behavior> min = null;

            for (Entry<String, Map<BehaviorName, Class<? extends Behavior>>> entry : LazyLoader.versionMap.entrySet()) {
                Class<? extends Behavior> next = entry.getValue().get(behaviorName);
                if (next == null) {
                    continue;
                }

                if (min == null || VersionUtils.compare(minVersion, entry.getKey()) > 0) {
                    min = next;
                    minVersion = entry.getKey();
                }

                if (VersionUtils.compare(entry.getKey(), this.version) > 0) {
                    continue;
                }
                if (last != null && VersionUtils.compare(entry.getKey(), lastVersion) < 0) {
                    continue;
                }
                last = next;
                lastVersion = entry.getKey();
            }
            if (last == null && Configuration.defaultBehaviors && min != null) {
                last = min;
            }

            if (last == null) {
                throw new IllegalStateException(behaviorName.name() + " " + this.version);
            }
            result = newInstance(last);
            this.behaviorCache.put(behaviorName, result);
        }
        return result;
    }

    public <A extends Behavior> A newInstance(Class<A> cl) {
        try {
            Constructor constructor = cl.getDeclaredConstructor();
            A result = (A) constructor.newInstance();
            result.init(this, classCache, mapping, serverInterface);
            result.checkMappings();
            result.checkClasses();
            result.bootstrap();
            return result;
        } catch (NoSuchMethodException
                | SecurityException
                | InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean checkMappings() {
        boolean result = true;
        for (BehaviorName behaviorName : BEHAVIOR_NAMES) {
            result = result & get(behaviorName).checkMappings();
        }
        return result;
    }

    public boolean checkClasses() {
        boolean result = true;
        for (BehaviorName behaviorName : BEHAVIOR_NAMES) {
            result = result & get(behaviorName).checkClasses();
        }
        return result;
    }

    public String getVersion() {
        return version;
    }

    private static class LazyLoader {
        static Map<String, Map<BehaviorName, Class<? extends Behavior>>> versionMap = new LinkedHashMap();

        static {
            for (BehaviorName behaviorName : BEHAVIOR_NAMES) {
                for (Entry<String, Class> entry : behaviorName.behaviorContainer.getBehaviors().entrySet()) {
                    String version = entry.getKey();
                    Map<BehaviorName, Class<? extends Behavior>> c = versionMap.get(version);
                    if (c == null) {
                        c = new EnumMap<>(BehaviorName.class);
                        versionMap.put(version, c);
                    }
                    c.put(behaviorName, entry.getValue());
                }
            }
        }
    }

}
