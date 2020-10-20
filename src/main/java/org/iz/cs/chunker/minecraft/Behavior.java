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

import java.util.Arrays;
import java.util.function.Function;

import org.iz.cs.chunker.Mapping;
import org.iz.cs.chunker.minecraft.BehaviorManager.BehaviorName;

public abstract class Behavior<T, R> implements Function<T, R> {


    protected BehaviorManager behaviorManager;
    protected ClassCache classCache;
    protected Mapping mapping;
    protected ServerInterface server;

    protected Behavior() {
        //super();
    }

    public boolean checkMappings() {
        return true;
    }

    public boolean checkClasses() {
        return true;
    }

    protected void bootstrap() {
        //optional
    }

    void init(BehaviorManager behaviorManager, ClassCache classCache, Mapping mapping, ServerInterface server) {
        this.behaviorManager = behaviorManager;
        this.classCache = classCache;
        this.mapping = mapping;
        this.server = server;
    }

    @SuppressWarnings("unchecked")
    protected Object applyOther(BehaviorName behavior, Object arg) {
        return behavior.apply(arg, behaviorManager);
    }

    protected void validateClass(String className) {
        if (classCache.get(className) == null) {
            throw new CompatibilityException("Class not found " + className);
        }
    }

    protected void validateField(String className, String fieldName) {
        try {
            if (classCache.get(className).getDeclaredField(mapping.getField(className, fieldName)) == null) {
                throw new CompatibilityException("Field "+ fieldName + " not found in class" + className);
            }
        } catch (NoSuchFieldException | SecurityException e) {
            throw new CompatibilityException("Field " + fieldName + " not found in class " + className);
        }
    }

    protected void validateMethod(String className, String methodName) {
        try {
            if (classCache.get(className).getDeclaredMethod(mapping.getMethod(className, methodName)) == null) {
                throw new CompatibilityException("Method " + methodName + " not found in class" + className);
            }
        } catch (SecurityException  | NoSuchMethodException e) {
            throw new CompatibilityException("Method " + methodName + " not found in class" + className);
        }
    }

    protected void validateMethod(String className, String methodName, String...parameters) {
        try {
            @SuppressWarnings("rawtypes")
            Class[] params = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                params[i] = classCache.get(parameters[i]);
            }
            if (classCache.get(className)
                    .getDeclaredMethod(
                            mapping.getMethod(className, methodName, parameters),
                            params) == null) {
                throw new CompatibilityException("Method " + methodName
                        + "(" + Arrays.toString(parameters)+ ") not found in class" + className);
            }
        } catch (SecurityException  | NoSuchMethodException e) {
            throw new CompatibilityException("Method " + methodName
                    + "(" + Arrays.toString(parameters)+ ") not found in class" + className);
        }
    }



    protected void validateClassMapping(String className) {
        if (!mapping.checkClass(className)) {
            throw new CompatibilityException("Class not found in mapping: " + className);
        }
    }

    protected void validateFieldMapping(String className, String fieldName) {
        if (mapping.getField(className, fieldName) == null) {
            throw new CompatibilityException("Field " + fieldName + " mapping not found in class " + className);
        }

    }

    protected void validateMethoMapping(String className, String methodName) {
        if (mapping.getMethod(className, methodName) == null) {
            throw new CompatibilityException("Method " + methodName + " mapping not found in class " + className);
        }
    }

    protected void validateMethodMapping(String className, String methodName, String...parameters) {
        if (mapping.getMethod(className, methodName, parameters) == null) {
            throw new CompatibilityException("Method " + methodName
                    + "(" + Arrays.toString(parameters)+ ") mapping not found in class " + className);
        }
    }

}
