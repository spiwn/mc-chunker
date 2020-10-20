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
import java.util.Map;

import org.iz.cs.chunker.JarClassLoader;
import org.iz.cs.chunker.Mapping;

public class ClassCache {

    private Mapping mapping;
    private JarClassLoader loader;
    private Map<String, Class> cache;

    public ClassCache(Mapping mapping, JarClassLoader loader) {
        this.mapping = mapping;
        this.loader = loader;
        this.cache = new HashMap<>();
        this.cache.put("int", int.class);
    }

    public Class<?> get(String className) {
        Class<?> result = cache.get(className);
        if (result != null) {
            return result;
        }
        try {
            result = loader.loadClass(mapping.getClassName(className));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Probably not implented");
        }
        cache.put(className, result);
        return result;
    }

    public Mapping getMapping() {
        return mapping;
    }

}
