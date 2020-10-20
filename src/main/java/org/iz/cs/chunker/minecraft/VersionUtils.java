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
