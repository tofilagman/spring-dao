package org.r3al.springdao;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryParameterTest {

    enum Color { RED, GREEN }

    @Test
    void constructorAndGetters() {
        DaoQueryParameter p = new DaoQueryParameter("foo", 42);

        assertThat(p.getName()).isEqualTo("foo");
        assertThat(p.getValue()).isEqualTo(42);
    }

    @Test
    void toStringIncludesNameAndValue() {
        DaoQueryParameter p = new DaoQueryParameter("foo", "bar");

        assertThat(p.toString()).contains("foo").contains("bar");
    }

    @Test
    void ofMapEmitsHolderForKeysAndOneEntryPerMapEntry() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        List<DaoQueryParameter> result = invokeOfMap(source, "container");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("container");
        assertThat(result.get(0).getValue()).isInstanceOf(Set.class);
        @SuppressWarnings("unchecked")
        Set<Object> firstKeys = (Set<Object>) result.get(0).getValue();
        assertThat(firstKeys).containsExactlyInAnyOrder("a", "b");

        Map<String, Object> rest = DaoQueryParameter.toMap(result.subList(1, 3));
        assertThat(rest).containsEntry("a", 1).containsEntry("b", 2);
    }

    @Test
    void getListReturnsSingleNullEntryWhenEmpty() {
        Collection<?> result = DaoQueryParameter.getList(List.of());

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).isNull();
    }

    @Test
    void getListPassesThroughNonEnumValues() {
        @SuppressWarnings("unchecked")
        Collection<Object> result = (Collection<Object>) DaoQueryParameter.getList(List.of(1, "two", 3.0));

        assertThat(result).containsExactly(1, "two", 3.0);
    }

    @Test
    void toMapTurnsParameterListIntoMap() {
        List<DaoQueryParameter> list = List.of(
                new DaoQueryParameter("a", 1),
                new DaoQueryParameter("b", "two")
        );

        Map<String, Object> map = DaoQueryParameter.toMap(list);

        assertThat(map).containsEntry("a", 1).containsEntry("b", "two").hasSize(2);
    }

    @Test
    void toMapKeepsLastValueOnDuplicateNames() {
        List<DaoQueryParameter> list = List.of(
                new DaoQueryParameter("a", 1),
                new DaoQueryParameter("a", 2)
        );

        Map<String, Object> map = DaoQueryParameter.toMap(list);

        assertThat(map).containsEntry("a", 2);
    }

    // ofMap is package-private; invoke via reflection to keep tests in the same package without modifying production visibility.
    private static List<DaoQueryParameter> invokeOfMap(Map<?, ?> map, String name) {
        try {
            Method m = DaoQueryParameter.class.getDeclaredMethod("ofMap", Map.class, String.class);
            m.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<DaoQueryParameter> result = (List<DaoQueryParameter>) m.invoke(null, map, name);
            return result;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
