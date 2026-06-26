package com.onde.core.security;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * JPA 엔티티 persist/update 시 모든 String 필드(임베디드·컬렉션 포함)에 XSS 필터를 적용합니다.
 * Hibernate 전역 리스너에서 호출되며, 개별 API/엔티티 수정 없이 DB 저장 경로 전체를 커버합니다.
 */
public final class EntityFieldSanitizer {

    private static final Set<String> EXCLUDED_FIELD_NAMES = Set.of(
            "password",
            "secret",
            "hash",
            "accesstoken",
            "refreshtoken",
            "fcmtoken",
            "token"
    );

    private EntityFieldSanitizer() {
    }

    public static void sanitizeEntity(Object entity) {
        if (entity == null) {
            return;
        }
        sanitizeEntity(entity, null, null, new IdentityHashMap<>());
    }

    public static void sanitizeEntity(Object entity, Object[] state, String[] propertyNames) {
        if (entity == null) {
            return;
        }
        sanitizeEntity(entity, state, propertyNames, new IdentityHashMap<>());
    }

    private static void sanitizeEntity(
            Object entity,
            Object[] state,
            String[] propertyNames,
            Map<Object, Boolean> visited
    ) {
        if (visited.put(entity, Boolean.TRUE) != null) {
            return;
        }

        Class<?> type = entity.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (shouldSkipField(field)) {
                    continue;
                }

                field.setAccessible(true);
                try {
                    if (field.isAnnotationPresent(Embedded.class)) {
                        Object embedded = field.get(entity);
                        if (embedded != null) {
                            sanitizeEmbeddedObject(embedded, state, propertyNames, field.getName());
                        }
                        continue;
                    }

                    Object currentValue = field.get(entity);
                    Object sanitizedValue = sanitizeFieldValue(field, currentValue, visited);
                    if (sanitizedValue != currentValue) {
                        field.set(entity, sanitizedValue);
                        updateHibernateState(state, propertyNames, field.getName(), sanitizedValue);
                    }
                } catch (IllegalAccessException ignored) {
                    // 필드 접근 불가 시 해당 필드는 건너뜁니다.
                }
            }
            type = type.getSuperclass();
        }
    }

    private static Object sanitizeFieldValue(Field field, Object value, Map<Object, Boolean> visited) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return InputSanitizer.sanitize(text);
        }
        if (field.isAnnotationPresent(ElementCollection.class) && value instanceof Collection<?> collection) {
            return sanitizeStringCollection(collection);
        }
        if (value instanceof Collection<?> collection) {
            return sanitizeStringCollection(collection);
        }
        return value;
    }

    private static Collection<?> sanitizeStringCollection(Collection<?> collection) {
        if (collection instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Object> mutableList = (List<Object>) list;
            for (int i = 0; i < mutableList.size(); i++) {
                Object item = mutableList.get(i);
                if (item instanceof String text) {
                    mutableList.set(i, InputSanitizer.sanitize(text));
                }
            }
            return mutableList;
        }
        if (collection instanceof Set<?> set) {
            @SuppressWarnings("unchecked")
            Set<Object> mutableSet = (Set<Object>) set;
            Set<Object> sanitized = new java.util.LinkedHashSet<>();
            for (Object item : mutableSet) {
                sanitized.add(item instanceof String text ? InputSanitizer.sanitize(text) : item);
            }
            mutableSet.clear();
            mutableSet.addAll(sanitized);
            return mutableSet;
        }
        return collection;
    }

    private static void sanitizeEmbeddedObject(
            Object embedded,
            Object[] state,
            String[] propertyNames,
            String embeddedFieldName
    ) {
        Class<?> type = embedded.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (shouldSkipField(field)) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    if (!(field.get(embedded) instanceof String text)) {
                        continue;
                    }
                    String sanitized = InputSanitizer.sanitize(text);
                    if (!sanitized.equals(text)) {
                        field.set(embedded, sanitized);
                        updateHibernateState(state, propertyNames, field.getName(), sanitized);
                        updateHibernateState(
                                state,
                                propertyNames,
                                embeddedFieldName + "." + field.getName(),
                                sanitized
                        );
                    }
                } catch (IllegalAccessException ignored) {
                    // 필드 접근 불가 시 해당 필드는 건너뜁니다.
                }
            }
            type = type.getSuperclass();
        }
    }

    private static void updateHibernateState(
            Object[] state,
            String[] propertyNames,
            String fieldName,
            Object sanitizedValue
    ) {
        if (state == null || propertyNames == null) {
            return;
        }
        for (int i = 0; i < propertyNames.length; i++) {
            if (fieldName.equals(propertyNames[i])) {
                state[i] = sanitizedValue;
                return;
            }
        }
    }

    private static boolean shouldSkipField(Field field) {
        if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
            return true;
        }
        if (field.isAnnotationPresent(SkipInputSanitization.class)) {
            return true;
        }
        if (field.isAnnotationPresent(ManyToOne.class)
                || field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(OneToOne.class)
                || field.isAnnotationPresent(ManyToMany.class)) {
            return true;
        }

        String normalizedName = field.getName().toLowerCase(Locale.ROOT);
        if (EXCLUDED_FIELD_NAMES.contains(normalizedName)) {
            return true;
        }
        if (normalizedName.endsWith("token")
                || normalizedName.endsWith("url")
                || normalizedName.endsWith("uri")
                || normalizedName.endsWith("link")) {
            return true;
        }

        if (field.isAnnotationPresent(ElementCollection.class)) {
            return !isStringCollectionField(field);
        }
        return false;
    }

    private static boolean isStringCollectionField(Field field) {
        if (!Collection.class.isAssignableFrom(field.getType())) {
            return false;
        }
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return false;
        }
        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        return typeArguments.length == 1 && typeArguments[0] == String.class;
    }
}
