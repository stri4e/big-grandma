package com.github.mapper.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Supplier;

public class MapperUtils {

    private static final Logger log = LoggerFactory.getLogger(MapperUtils.class);

    @SuppressWarnings(value = "unchecked")
    public static <T> T ofEntity(Map<String, Object> source, Class<T> czl) {
        try {
            Constructor<?> constructor = requiredEmptyConstructor(czl);
            Object target = constructor.newInstance();
            Field[] fields = czl.getDeclaredFields();
            String prefix = czl.getSimpleName();
            Arrays.stream(fields).forEach(field -> {
                var fieldName = findRequiredField(prefix, field, source);
                if (StringUtils.hasText(fieldName)) {
                    field.setAccessible(Boolean.TRUE);
                    ReflectionUtils.setField(field, target, source.get(fieldName));
                }
            });
            return (T) target;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            log.error("Enter: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public static Constructor<?> requiredEmptyConstructor(Class<?> clz) {
        try {
            return clz.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    String.format(
                            "Default constructor is not exist in this entity -> %s, in package -> %s",
                            clz.getSimpleName(), clz.getPackageName()
                    )
            );
        }
    }

    public static void setFields(Object source, Object target, String fieldName) {
        if (StringUtils.hasText(fieldName)) {
            Class<?> clz = target.getClass();
            Field field = ReflectionUtils.findField(clz, fieldName);
            if (Objects.nonNull(field)) {
                Objects.requireNonNull(field).setAccessible(Boolean.TRUE);
                try {
                    ReflectionUtils.setField(field, target, source);
                } catch (Throwable e) {
                    var message = String.format(
                            "Field type miss match in target: %s, field: [name: %s, type: %s], source: %s",
                            target.getClass().getSimpleName(),
                            field.getName(),
                            field.getType(),
                            source.getClass().getSimpleName()
                    );
                    log.error(message);
                    throw new IllegalArgumentException(message);
                }
            } else {
                log.error("Can't find field -> {} in: {} ", fieldName, target.getClass());
            }
        } else {
            log.error("Not existing field in -> {}, field name [ {} ], source: {}", target.getClass().getSimpleName(), fieldName, source);
        }
    }

    public static void logMessage(Supplier<Void> check, RuntimeException exception) {
        try {
            check.get();
        } catch (Throwable e) {
            throw exception;
        }
    }

    @SuppressWarnings("unchecked")
    public static Collection<Object> getCollections(String fieldName, Class<?> collectionType, Object target) {
        try {
            Field field = ReflectionUtils.findField(collectionType, fieldName);
            if (Objects.isNull(field)) {
                throw new IllegalArgumentException("Can't find field " + fieldName + " in " + target);
            }
            field.setAccessible(Boolean.TRUE);
            return (Collection<Object>) field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Can't get access field " + fieldName + " in " + target);
        }
    }

    public static void mapFields(Map<String, Object> values, Object target) {
        values.keySet().stream()
                .filter(key -> MapperUtils.isFieldExist(key, target))
                .forEach(key -> {
                    Object source = values.get(key);
                    MapperUtils.setFields(source, target, key);
                });
    }

    public static boolean isFieldExist(String name, Object target) {
        return isFieldExist(name, target.getClass());
    }

    public static boolean isFieldExist(String name, Class<?> target) {
        return StringUtils.hasText(name) && Objects.nonNull(ReflectionUtils.findField(target, name));
    }

    public static Collection<?> collFactory(Class<?> collType) {
        if (Map.class.equals(collType)) {
            throw new RuntimeException("Interface Map<?, ?> not exist in interface Collections<?>.");
        }
        try {
            return (Collection<?>) collType.getConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static Class<?> collTypeMapper(Class<?> clz) {
        if (clz.getTypeName().equals(List.class.getTypeName())) {
            return ArrayList.class;
        } else if (clz.getTypeName().equals(Set.class.getTypeName())) {
            return HashSet.class;
        }
        return clz;
    }

    @SuppressWarnings(value = "unchecked")
    public static Collection<Object> cast(Object val) {
        return (Collection<Object>) val;
    }

    public static boolean isColl(Class<?> coll) {
        var result = false;
        if (Objects.equals(coll, List.class)) {
            result = true;
        } else if (Objects.equals(coll, Set.class)) {
            result = true;
        } else if (Objects.equals(coll, ArrayList.class)) {
            result = true;
        } else if (Objects.equals(coll, LinkedList.class)) {
            result = true;
        } else if (Objects.equals(coll, HashSet.class)) {
            result = true;
        } else if (Objects.equals(coll, LinkedHashSet.class)) {
            result = true;
        } else if (Objects.equals(coll, TreeSet.class)) {
            result = true;
        }
        return result;
    }

    private static String findRequiredField(String prefix, Field field, Map<String, Object> source) {
        String name = field.getName();
        var requiredFieldName = name.contains(prefix) ||
                name.contains(prefix.toLowerCase(Locale.ROOT)) ||
                name.contains(prefix.toUpperCase(Locale.ROOT))
                ? name : String.format("%s%s", prefix, field.getName());
        return source.keySet().stream()
                .filter(key -> requiredFieldName.toUpperCase(Locale.ROOT).equalsIgnoreCase(key))
                .findFirst().orElse(null);
    }

}
