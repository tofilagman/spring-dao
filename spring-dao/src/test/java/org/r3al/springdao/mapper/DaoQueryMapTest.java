package org.r3al.springdao.mapper;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryMapTest {

    static class User {
        public String getName() { return null; }
        public Integer getAge() { return null; }
    }

    @Test
    void factoryReturnsInstanceWithOneItem() {
        Predicate<User> nameField = u -> u.getName() != null;

        DaoQueryMap map = DaoQueryMap.of(nameField, DaoQueryMapOperator.EQ, List.of("alice"));

        assertThat(map).isNotNull();
        assertThat(itemsOf(map)).hasSize(1);
        DaoQueryMapItem only = itemsOf(map).values().iterator().next();
        assertThat(only.getOperator()).isEqualTo(DaoQueryMapOperator.EQ);
        assertThat(only.getValues()).containsExactly("alice");
    }

    @Test
    @SuppressWarnings("unchecked")
    void andChainsAndAccumulatesItems() {
        Predicate<User> nameField = u -> u.getName() != null;
        Predicate<User> ageField = u -> u.getAge() != null;

        DaoQueryMap map = DaoQueryMap.of(nameField, DaoQueryMapOperator.EQ, List.of("alice"))
                .and(ageField, DaoQueryMapOperator.GT, List.of(18));

        assertThat(itemsOf(map)).hasSize(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void orChainsAndAccumulatesItems() {
        Predicate<User> nameField = u -> u.getName() != null;
        Predicate<User> ageField = u -> u.getAge() != null;

        DaoQueryMap map = DaoQueryMap.of(nameField, DaoQueryMapOperator.EQ, List.of("alice"))
                .or(ageField, DaoQueryMapOperator.LT, List.of(99));

        assertThat(itemsOf(map)).hasSize(2);
    }

    @Test
    void samePredicateInstanceOverwritesRatherThanDuplicates() {
        // Items are keyed by Predicate.toString(); the same lambda has the same toString,
        // so adding twice with the same predicate replaces.
        Predicate<User> nameField = u -> u.getName() != null;

        DaoQueryMap map = DaoQueryMap.of(nameField, DaoQueryMapOperator.EQ, List.of("first"))
                .and(nameField, DaoQueryMapOperator.EQ, List.of("second"));

        assertThat(itemsOf(map)).hasSize(1);
        assertThat(itemsOf(map).values().iterator().next().getValues()).containsExactly("second");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, DaoQueryMapItem> itemsOf(DaoQueryMap map) {
        try {
            Field f = DaoQueryMap.class.getDeclaredField("items");
            f.setAccessible(true);
            return (Map<String, DaoQueryMapItem>) f.get(map);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
