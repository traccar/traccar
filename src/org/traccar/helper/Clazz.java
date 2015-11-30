/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.helper;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class Clazz implements Serializable {

    private static final long serialVersionUID = 4983299355055144885L;

    private Clazz() {
    }

    public static Class getGenericArgumentType(Class currentClass, Class genericSuperClass) {
        return getGenericArgumentType(currentClass, genericSuperClass, 0);
    }

    public static Class getGenericArgumentType(Class currentClass, int argumentIndex) {
        return getGenericArgumentType(currentClass, null, argumentIndex);
    }

    public static Class getGenericArgumentType(Class currentClass) {
        return getGenericArgumentType(currentClass, null, 0);
    }

    public static Class getGenericArgumentType(Class currentClass, Class genericSuperClass, int argumentIndex) {
        Type superType = currentClass.getGenericSuperclass();
        if (superType == null) {
            throw new IllegalArgumentException();
        }
        if (!(superType instanceof ParameterizedType)
                || genericSuperClass != null
                && ((ParameterizedType) superType).getRawType() != genericSuperClass) {
            return getGenericArgumentType(currentClass.getSuperclass(), genericSuperClass, argumentIndex);
        }
        Object[] args = ((ParameterizedType) superType).getActualTypeArguments();
        if (argumentIndex >= args.length) {
            throw new IllegalArgumentException();
        }
        return cast(Class.class, args[argumentIndex]);
    }

    public static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException();
        }
    }

    public static <T> T cast(Class<T> classe, Object objeto) {
        if (classe.isAssignableFrom(objeto.getClass())) {
            return classe.cast(objeto);
        }
        throw new ClassCastException();
    }

    public static Class forName(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> long getId(T entity) throws Exception {
        Method[] methods = entity.getClass().getMethods();
        for (final Method method : methods) {
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                final String name = Introspector.decapitalize(method.getName().substring(3));
                if (name.equals("id")) {
                    return Long.parseLong(method.invoke(entity).toString());
                }
            }
        }
        throw new IllegalArgumentException();
    }

    public static <T, I> void setId(T entity, I id) throws Exception {
        Method[] methods = entity.getClass().getMethods();
        for (final Method method : methods) {
            if (method.getName().startsWith("set") && method.getParameterTypes().length == 1) {
                final String name = Introspector.decapitalize(method.getName().substring(3));
                if (name.equals("id")) {
                    method.invoke(entity, id);
                    break;
                }
            }
        }
    }
}
