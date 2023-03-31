package org.r3al.springdao.mapper;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.StandardBasicTypes;
import org.r3al.springdao.DaoQueryAccessField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class HibernateTypesMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateTypesMapper.class);

    private static final Map<String, Map<String, BasicTypeReference<?>>> CACHE = new HashMap<>();

    public static void map(Query<?> query, Class<?> dto) {
        Map<String, BasicTypeReference<?>> map = CACHE.get(dto.getName());
        if (map == null) {
            map = new HashMap<>();
            for (Field field : dto.getDeclaredFields()) {
                BasicTypeReference<?> hibernateType = getHibernateType(field.getType());
                var accessField = new DaoQueryAccessField(field);
                map.put(accessField.getSqlName(), hibernateType);
            }
            CACHE.put(dto.getName(), map);
        }

        if (query instanceof NativeQuery<?>) {
            map.forEach(((NativeQuery) query)::addScalar);
        }
    }

    private static BasicTypeReference<?> getHibernateType(Class<?> fieldType) {
        return switch (fieldType.getCanonicalName()) {
            case "java.lang.Integer" -> StandardBasicTypes.INTEGER;
            case "java.lang.Long" -> StandardBasicTypes.LONG;
            case "java.math.BigDecimal" -> StandardBasicTypes.BIG_DECIMAL;
            case "java.lang.Float" -> StandardBasicTypes.FLOAT;
            case "java.math.BigInteger" -> StandardBasicTypes.BIG_INTEGER;
            case "java.lang.Short" -> StandardBasicTypes.SHORT;
            case "java.lang.Boolean" -> StandardBasicTypes.BOOLEAN;
            case "java.lang.Character" -> StandardBasicTypes.CHARACTER;
            case "java.util.Date" -> StandardBasicTypes.DATE;
            case "java.lang.Number" -> StandardBasicTypes.DOUBLE;
            default -> StandardBasicTypes.STRING;
        };
    }

}