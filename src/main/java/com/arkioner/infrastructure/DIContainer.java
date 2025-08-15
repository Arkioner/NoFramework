package com.arkioner.infrastructure;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;

public class DIContainer {
    private static final String defaultBeanName = "bean";
    private final Map<String, Object> singletons = new HashMap<>();
    private final Map<String, Class<?>> bindings = new HashMap<>();
    private final Map<String, Class<?>> interfaceMappings = new HashMap<>();
    private final ThreadLocal<Set<String>> resolving = ThreadLocal.withInitial(HashSet::new);

    private String getKey(Class<?> type, String name) {
        return getKey(type.getName(), name);
    }

    private String getKey(String typeName, String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Binding name cannot be null or empty");
        }
        return typeName + ":" + name;
    }

    private String getDefaultKey(String typeName) {
        return getKey(typeName, defaultBeanName);
    }

    public <T> void register(Class<T> type) {
        register(type, defaultBeanName);
    }

    public <T> void register(Class<T> implementation, String name) {
        if (implementation == null) {
            throw new IllegalArgumentException("Implementation class cannot be null");
        }
        String key = getKey(implementation, name);
        bindings.put(key, implementation);

        registerInterfaceMappings(implementation, name);
    }

    public <T> void registerInstance(T instance) {
        registerInstance(instance, defaultBeanName);
    }

    public <T> void registerInstance(T instance, String name) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }
        Class<?> implementation = instance.getClass();
        String key = getKey(implementation, name);
        singletons.put(key, instance);

        registerInterfaceMappings(implementation, name);
    }

    public <T> T resolve(Class<T> type) {
        return resolve(type, defaultBeanName);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type, String name) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        String key = getKey(type, name);

        Set<String> resolvingSet = resolving.get();
        if (resolvingSet.contains(key)) {
            throw new IllegalStateException("Cyclic dependency detected for " + key);
        }
        resolvingSet.add(key);
        try {
            if (singletons.containsKey(key)) {
                return (T) singletons.get(key);
            }

            Class<?> implClass = bindings.get(key);
            if (implClass == null) {
                implClass = interfaceMappings.get(key);
                if (implClass == null) {
                    throw new IllegalArgumentException("No binding found for " + type.getName() + " with name " + name);
                }
            }

            T instance = createInstance(implClass, name);
            singletons.put(getKey(implClass, name), instance);
            if (type.isInterface()) {
                singletons.put(key, instance);
            }
            return instance;
        } finally {
            resolvingSet.remove(key);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T createInstance(Class<?> implClass, String bindingName) {
        return getSuitableConstructor(implClass)
                .map(constructor -> {
                    Object[] dependencies = getDependencies(implClass, constructor);
                    try {
                        return (T) constructor.newInstance(dependencies);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("No suitable constructor found for " + implClass.getName() + " with binding name " + bindingName));
    }

    private Object[] getDependencies(Class<?> implClass, Constructor<?> selectedConstructor) {
        Class<?>[] paramTypes = selectedConstructor.getParameterTypes();
        Parameter[] parameters = selectedConstructor.getParameters();
        Object[] params = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            String paramName = parameters[i].getName();
            String key = getKey(paramTypes[i], paramName);
            String defaultKey = getDefaultKey(paramTypes[i].getName());
            try {
                if (bindings.containsKey(key) || interfaceMappings.containsKey(key) || singletons.containsKey(key)) {
                    params[i] = resolve(paramTypes[i], paramName);
                } else if (bindings.containsKey(defaultKey) || interfaceMappings.containsKey(defaultKey) || singletons.containsKey(defaultKey)) {
                    params[i] = resolve(paramTypes[i], defaultBeanName);
                } else {
                    throw new IllegalArgumentException("No binding found for parameter " + paramName + " of type " + paramTypes[i].getName());
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Failed to resolve parameter " + paramName + " of type " + paramTypes[i].getName() + " for " + implClass.getName(), e);
            }
        }
        return params;
    }

    private Optional<Constructor<?>> getSuitableConstructor(Class<?> implClass) {
        for (Constructor<?> constructor : implClass.getDeclaredConstructors()) {
            constructor.setAccessible(true);
            Class<?>[] paramTypes = constructor.getParameterTypes();
            Parameter[] parameters = constructor.getParameters();
            boolean canResolveAll = true;
            for (int i = 0; i < paramTypes.length; i++) {
                String paramName = parameters[i].getName();
                String key = getKey(paramTypes[i], paramName);
                String defaultKey = getDefaultKey(paramTypes[i].getName());
                if (!bindings.containsKey(key) && !interfaceMappings.containsKey(key) && !singletons.containsKey(key) &&
                        !bindings.containsKey(defaultKey) && !interfaceMappings.containsKey(defaultKey) && !singletons.containsKey(defaultKey)) {
                    canResolveAll = false;
                    break;
                }
            }
            if (canResolveAll) {
                return Optional.of(constructor);
            }
        }
        return Optional.empty();
    }

    private <T> void registerInterfaceMappings(Class<T> implementation, String name) {
        for (Class<?> inheritedInterface : implementation.getInterfaces()) {
            if (!inheritedInterface.getName().startsWith("java.") && !inheritedInterface.getName().startsWith("javax.")) {
                interfaceMappings.put(getKey(inheritedInterface, name), implementation);
            }
        }
    }
}