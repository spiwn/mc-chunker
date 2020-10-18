package org.iz.cs.chunker.minecraft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iz.cs.chunker.Mapping;

public class VersionUtils {

    private static final String v1_14_4 = "1.14.4";
    private static final String v19w36a = "19w36a";

    public static boolean isSupported(String version) {
        return v1_14_4.equals(version) || compare(v19w36a, version) <= 0;
    }

    public static int compare(String o1, String o2) {
        return Integer.compare(LazyLoader.allVersions.get(o2), LazyLoader.allVersions.get(o1));
    }

    @SuppressWarnings("unchecked")
    private static class LazyLoader {
        private static Map<String, Integer> allVersions;

        static {
            List<Object> versions = (List<Object>) Mapping.readManifestJson().get("versions");
            allVersions = new HashMap<String, Integer>(versions.size());
            int i = 0;
            for (Object elem : versions) {
                Map<String, Object> current = (Map<String, Object>) elem;
                allVersions.put((String) current.get("id"), i++);
            }
        }
    }
}
