package org.r3al.springdao;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;
import org.r3al.springdao.annotations.DaoQueryParam;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryAccessFieldTest {

    static class Holder {
        @Column(name = "snake_case")
        String columnField;

        @DaoQueryParam("alias")
        Integer paramField;

        String plain;

        @Column(name = "isActive")
        Boolean isActive;

        Boolean isEnabled;

        String isName;
    }

    private Field field(String name) throws NoSuchFieldException {
        return Holder.class.getDeclaredField(name);
    }

    @Test
    void plainFieldExposesNameAndType() throws NoSuchFieldException {
        DaoQueryAccessField af = new DaoQueryAccessField(field("plain"));

        assertThat(af.getName()).isEqualTo("plain");
        assertThat(af.getType()).isEqualTo(String.class);
        assertThat(af.getParam()).isNull();
        assertThat(af.getColumn()).isNull();
        assertThat(af.getSqlName()).isEqualTo("plain");
    }

    @Test
    void columnAnnotationExposedAndDrivesSqlName() throws NoSuchFieldException {
        DaoQueryAccessField af = new DaoQueryAccessField(field("columnField"));

        assertThat(af.getColumn()).isNotNull();
        assertThat(af.getColumn().name()).isEqualTo("snake_case");
        assertThat(af.getSqlName()).isEqualTo("snake_case");
    }

    @Test
    void isPrefixBooleanFieldExposesStrippedAccessorName() throws NoSuchFieldException {
        DaoQueryAccessField af = new DaoQueryAccessField(field("isActive"));

        assertThat(af.getName()).isEqualTo("isActive");
        assertThat(af.getAccessorName()).isEqualTo("Active");
        assertThat(af.getSqlName()).isEqualTo("isActive");
    }

    @Test
    void isPrefixBooleanWithoutColumnStillStripsAccessorName() throws NoSuchFieldException {
        DaoQueryAccessField af = new DaoQueryAccessField(field("isEnabled"));

        assertThat(af.getAccessorName()).isEqualTo("Enabled");
    }

    @Test
    void isPrefixNonBooleanKeepsRawName() throws NoSuchFieldException {
        DaoQueryAccessField af = new DaoQueryAccessField(field("isName"));

        assertThat(af.getAccessorName()).isEqualTo("isName");
    }

    @Test
    void plainFieldAccessorNameMatchesRawName() throws NoSuchFieldException {
        DaoQueryAccessField af = new DaoQueryAccessField(field("plain"));

        assertThat(af.getAccessorName()).isEqualTo("plain");
    }

    @Test
    void paramAnnotationExposedButDoesNotDriveSqlName() throws NoSuchFieldException {
        // DaoQueryAccessField.getSqlName only consults column, not param.
        DaoQueryAccessField af = new DaoQueryAccessField(field("paramField"));

        assertThat(af.getParam()).isNotNull();
        assertThat(af.getParam().value()).isEqualTo("alias");
        assertThat(af.getSqlName()).isEqualTo("paramField");
        assertThat(af.getType()).isEqualTo(Integer.class);
    }
}
