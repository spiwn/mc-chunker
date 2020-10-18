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
package org.iz.cs.chunker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.iz.cs.chunker.minecraft.Constants;

import javassist.ByteArrayClassPath;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.Modifier;
import javassist.NotFoundException;

public class JarClassLoader extends URLClassLoader implements ClassPath {

    private URL url;

    private String classNameToDecorate;

    public JarClassLoader(String jarLocation) throws MalformedURLException {
        this(new URL("jar:" + new File(jarLocation).toURI().toString() + "!/"));
    }

    private JarClassLoader(URL url) {
        super(new URL[] { url },
                JarClassLoader.class.getClassLoader());
        this.url = url;
    }

    public JarFile getJarFile() throws IOException {
        return ((JarURLConnection) url.openConnection()).getJarFile();
    }

    public void setClassToDecorate(String classNameToDecorate) {
        this.classNameToDecorate = classNameToDecorate;
    }

    public String getMain() throws IOException {
        JarURLConnection uc = (JarURLConnection) url.openConnection();
        Attributes attr = uc.getMainAttributes();
        return attr != null
                ? attr.getValue(Attributes.Name.MAIN_CLASS)
                : null;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (classNameToDecorate.equals(name)) {
            try {
                InputStream is = this.getResourceAsStream(name.replace('.', '/') + ".class");
                byte[] classFile = readStream(is);
                classFile = decorate(name, classFile);
                return defineClass(name, classFile, 0, classFile.length);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return super.findClass(name);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null && !delegateToParent(name)) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException e) {}
            }
            if (c == null) {
                c = getParent().loadClass(name);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    private boolean delegateToParent(String name) {
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("sun.")
                || name.startsWith("com.sun.")
                || name.startsWith("org.w3c.");
    }

    private static byte[] readStream(InputStream fin) throws IOException {
        byte[][] bufs = new byte[8][];
        int bufsize = 4096;

        for (int i = 0; i < 8; ++i) {
            bufs[i] = new byte[bufsize];
            int size = 0;
            int len = 0;
            do {
                len = fin.read(bufs[i], size, bufsize - size);
                if (len >= 0)
                    size += len;
                else {
                    byte[] result = new byte[bufsize - 4096 + size];
                    int s = 0;
                    for (int j = 0; j < i; ++j) {
                        System.arraycopy(bufs[j], 0, result, s, s + 4096);
                        s = s + s + 4096;
                    }

                    System.arraycopy(bufs[i], 0, result, s, size);
                    return result;
                }
            } while (size < bufsize);
            bufsize *= 2;
        }

        throw new IOException("too much data");
    }

    private byte[] decorate(String name, byte[] classFile) throws Exception {
        ClassPool cp = new ClassPool();

        cp.insertClassPath(new ByteArrayClassPath(name, classFile));
        cp.insertClassPath(this);
        cp.appendSystemPath();
        CtClass sc = cp.get(name);

        CtField field = new CtField(sc, "instance", sc);
        field.setModifiers(Modifier.STATIC | Modifier.PUBLIC | Modifier.VOLATILE);
        sc.addField(field);

        for (CtConstructor scc : sc.getDeclaredConstructors()) {
            scc.insertAfter("this." + Constants.INSTANCE + " = this;");
        }

        byte[] bc = sc.toBytecode();
        return bc;
    }

    @Override
    public URL find(String classname) {
        try {
            URLConnection con = openClassfile0(classname);
            if (con == null) {
                return null;
            }
            InputStream is = con.getInputStream();
            if (is != null) {
                is.close();
                return con.getURL();
            } else {
                throw new RuntimeException();
            }
        }
        catch (IOException e) {}
        return null;
    }

    private URLConnection openClassfile0(String name) throws IOException {
        if (delegateToParent(name)) {
            return getParent().getResource(name.replace('.', '/') + ".class").openConnection();
        }
        URL r = findResource(name.replace('.', '/') + ".class");
        if (r == null) {
            return null;
        }
        return r.openConnection();
    }

    @Override
    public InputStream openClassfile(String classname) throws NotFoundException {
        try {
            URLConnection con = openClassfile0(classname);
            if (con != null) {
                return con.getInputStream();
            }
        } catch (IOException e) {}
        return null;        // not found
    }

}
