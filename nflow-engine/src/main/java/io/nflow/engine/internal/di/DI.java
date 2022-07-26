package io.nflow.engine.internal.di;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.PUBLIC;
public class DI {
    private final ConcurrentMap<Class<?>, Object> singletons = new ConcurrentHashMap<>();

    public DI() {
        store(this);
    }

    public <T> T store(Class<T> clazz, T instance) {
        if (singletons.put(clazz, instance) != null) {
            throw new IllegalStateException("Two implementations for " + clazz.getName() + " registered");
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T> T store(T instance) {
        return store((Class<T>) instance.getClass(), instance);
    }

    public <T> T get(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        var instance = (T) singletons.get(clazz);
        if (instance == null) {
            throw new IllegalStateException("No implementation for " + clazz.getName() + " registered");
        }
        return instance;
    }

    public <T> T getOrCreate(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        var instance = (T) singletons.computeIfAbsent(clazz, type -> {
            var ctor = Stream.of(type.getDeclaredConstructors()).filter(c -> (c.getModifiers() & PUBLIC) != 0).findFirst().orElseThrow(() -> new IllegalStateException("No public constructor found in " + type.getName()));
            var args = Stream.of(ctor.getParameterTypes()).map(this::getOrCreate).toArray();
            try {
                return ctor.newInstance(args);
            } catch (InstantiationException|IllegalAccessException|InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
        return instance;
    }
}
