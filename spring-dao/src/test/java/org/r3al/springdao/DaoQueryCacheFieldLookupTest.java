package org.r3al.springdao;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryCacheFieldLookupTest {

    static class IsPrefixHolder {
        @Column(name = "isActive")
        Boolean isActive;

        Boolean isEnabled;

        String name;
    }

    @Test
    void isPrefixBooleanIndexedUnderBothRawAndStrippedName() {
        Map<String, DaoQueryFieldInfo> info = DaoQueryCache.getFieldInfo(IsPrefixHolder.class);

        // Raw field name resolves (case-insensitive map).
        DaoQueryFieldInfo viaField = info.get("isActive");
        assertThat(viaField).isNotNull();
        assertThat(viaField.getSqlName()).isEqualTo("isActive");

        // Stripped JavaBean accessor name resolves to the same FieldInfo —
        // this is what DaoQueryParameter.ofDeclaredMethods uses, because the
        // getter isActive() produces accessor name "Active".
        DaoQueryFieldInfo viaAccessor = info.get("Active");
        assertThat(viaAccessor).isNotNull();
        assertThat(viaAccessor.getSqlName()).isEqualTo("isActive");
        assertThat(viaAccessor.getColumn()).isNotNull();
    }

    @Test
    void isPrefixBooleanWithoutColumnAlsoIndexedUnderStrippedName() {
        Map<String, DaoQueryFieldInfo> info = DaoQueryCache.getFieldInfo(IsPrefixHolder.class);

        assertThat(info.get("isEnabled")).isNotNull();
        assertThat(info.get("Enabled")).isNotNull();
        assertThat(info.get("Enabled").getSqlName()).isEqualTo("isEnabled");
    }

    @Test
    void plainFieldNotDuplicatedUnderAltKey() {
        Map<String, DaoQueryFieldInfo> info = DaoQueryCache.getFieldInfo(IsPrefixHolder.class);

        assertThat(info).containsKey("name");
        // No surprise aliases for non-is-prefixed fields.
        assertThat(info.get("name").getSqlName()).isEqualTo("name");
    }
}
