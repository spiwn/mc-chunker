package org.iz.cs.chunker.minecraft;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.HashMap;
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
        IS_DIMENSION_READY,
        GET_DEDICATED_SERVER_CLASS_NAME,
        GET_DEDICATED_SERVER_INSTANCE,
        GENERATE_CHUNK,
        ;
    }

    private static Map<String, Map<BehaviorName, Class<? extends Behavior>>> versionMap = new LinkedHashMap();

    static {
        EnumMap<BehaviorName, Class<? extends Behavior>> temp;
        temp = new EnumMap<>(BehaviorName.class);
        temp.put(BehaviorName.IS_DIMENSION_READY, IsServerReady.I_1_15_2.class);
        temp.put(BehaviorName.GET_DEDICATED_SERVER_CLASS_NAME, GetDedicatedServerClassName.I_1_15_2.class);
        temp.put(BehaviorName.GET_DEDICATED_SERVER_INSTANCE, GetDedicatedServerInstance.I_1_15_2.class);
        temp.put(BehaviorName.GENERATE_CHUNK, GenerateChunk.I_1_15_2.class);
        versionMap.put("1.15.2", temp);
    }

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
            for (Entry<String, Map<BehaviorName, Class<? extends Behavior>>> entry : versionMap.entrySet()) {
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

}
