package io.nflow.engine.internal.di;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.stream.Stream;

import static java.lang.reflect.Modifier.PUBLIC;

public class DI {
    private final ConcurrentMap<Class<?>, Object> singletons = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, Object> inProgressSingletons = new ConcurrentHashMap<>();

    public DI() {
        store(this);
    }

    @SuppressWarnings("unchecked")
    public <T> T store(Class<T> clazz, T instance) {
        return (T) storeImpl(clazz, instance);
    }

    public <T> void storeProvider(Class<T> clazz, SimpleProvider<T> generator) {
        storeImpl(clazz, generator);
    }

    public <T> void storeProvider(Class<T> clazz, DIProvider<T> generator) {
        storeImpl(clazz, generator);
    }

    private Object storeImpl(Class<?> clazz, Object obj) {
        return storeImpl(singletons, clazz, obj, obj);
    }

    private static Object storeImpl(ConcurrentMap<Class<?>, Object> storage, Class<?> clazz, Object obj, Object allowedValue) {
        var prev = storage.put(clazz, obj);
        if (prev != null && prev != allowedValue) {
            throw new IllegalStateException("Two implementations for " + clazz.getName() + " registered");
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    public <T> T store(T instance) {
        return store((Class<T>) instance.getClass(), instance);
    }

    public <T> T fetch(Class<T> clazz) {
        var object = singletons.get(clazz);
        if (object == null) {
            object = inProgressSingletons.get(clazz);
            if (object == null) {
                throw new IllegalStateException("No implementation for " + clazz.getName() + " registered");
            }
        }
        return resolveProvider(clazz, object, true);
    }

    public <T> T get(Class<T> clazz) {
        return getImpl(clazz, true);
    }

    private <T> T getInProgress(Class<T> clazz) {
        return getImpl(clazz, false);
    }

    private <T> T getImpl(Class<T> clazz, boolean topLevel) {
        Object object;
        if (topLevel) {
            object = singletons.computeIfAbsent(clazz, type -> {
                synchronized (type) {
                    return computeNewInstance(type);
                }
            });
            for (var i = inProgressSingletons.entrySet().iterator(); i.hasNext(); ) {
                var e = i.next();
                singletons.put(e.getKey(), e.getValue());
                i.remove();
            }
        } else {
            synchronized (clazz) {
                var obj = singletons.get(clazz);
                if (obj != null) {
                    object = obj;
                } else {
                    object = computeNewInstance(clazz);
                }
            }
        }
        return resolveProvider(clazz, object, topLevel);
    }

    private <T> Object computeNewInstance(Class<?> type) {
        var obj = inProgressSingletons.get(type);
        if (obj != null) {
            return obj;
        }
        var ctor = Stream.of(type.getDeclaredConstructors()).filter(c -> (c.getModifiers() & PUBLIC) != 0)
                .findFirst().orElseThrow(() -> new IllegalStateException("No public constructor found in " + type.getName()));
        var args = Stream.of(ctor.getParameterTypes()).map(this::getInProgress).toArray();
        try {
            obj = ctor.newInstance(args);
            inProgressSingletons.put(type, obj);
            return obj;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Problem instantiating " + type + " using constructor", e);
        }
    }

    public interface ProviderBase<T> {

    }

    @FunctionalInterface
    public interface SimpleProvider<T> extends ProviderBase<T> {
        T newInstance();
    }

    @FunctionalInterface
    public interface DIProvider<T> extends ProviderBase<T> {
        T newInstance(DI di);
    }

    @SuppressWarnings("unchecked")
    private <T> T resolveProvider(Class<T> clazz, Object object, boolean topLevel) {
        // normal instance
        if (!(object instanceof ProviderBase)) {
            return (T) object;
        }
        synchronized (clazz) {
            var refetch = singletons.get(clazz);
            if (refetch != object) {
                return (T) refetch;
            }
            var instance = instantiate((ProviderBase<T>) object);
            var map = topLevel ? singletons : inProgressSingletons;
            storeImpl(map, clazz, instance, object);
            return instance;
        }
    }

    private <T> T instantiate(ProviderBase<T> object) {
        if (object instanceof SimpleProvider) {
            return ((SimpleProvider<T>) object).newInstance();
        }
        if (object instanceof DIProvider) {
            return ((DIProvider<T>) object).newInstance(this);
        }
        throw new IllegalStateException("Unsupported provider type " + object.getClass());
    }
}
