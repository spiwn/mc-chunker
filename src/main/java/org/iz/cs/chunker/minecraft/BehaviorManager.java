package org.iz.cs.chunker.minecraft;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.iz.cs.chunker.Mapping;
import org.iz.cs.chunker.minecraft.impl.GenerateChunk;
import org.iz.cs.chunker.minecraft.impl.GetDedicatedServerClassName;
import org.iz.cs.chunker.minecraft.impl.GetDedicatedServerInstance;
import org.iz.cs.chunker.minecraft.impl.IsServerReady;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class BehaviorManager {


    public static enum BehaviorName {
        IS_SERVER_READY(new IsServerReady()),
        GET_DEDICATED_SERVER_CLASS_NAME(new GetDedicatedServerClassName()),
        GET_DEDICATED_SERVER_INSTANCE(new GetDedicatedServerInstance()),
        GENERATE_CHUNK(new GenerateChunk()),
        ;

        private BehaviorContainer behaviorContainer;

        private BehaviorName(BehaviorContainer behaviorContainer) {
            this.behaviorContainer = behaviorContainer;
        }

        public BehaviorContainer getBehaviorContainer() {
            return behaviorContainer;
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

    public Behavior get(BehaviorName behaviorName) {
        Behavior result = this.behaviorCache.get(behaviorName);
        if (result == null) {
            Class<? extends Behavior> last = null;
            for (Entry<String, Map<BehaviorName, Class<? extends Behavior>>> entry : LazyLoader.versionMap.entrySet()) {
                if (VersionUtils.compare(entry.getKey(), this.version) > 0) {
                    break;
                }
                Class<? extends Behavior> next = entry.getValue().get(behaviorName);
                if (next != null) {
                    last = next;
                }
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
