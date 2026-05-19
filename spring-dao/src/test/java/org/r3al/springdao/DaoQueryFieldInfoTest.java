package org.r3al.springdao;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;
import org.r3al.springdao.annotations.DaoQueryParam;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryFieldInfoTest {

    static class Holder {
        @Column(name = "user_id")
        String columnAnnotated;

        @DaoQueryParam("paramName")
        String paramAnnotated;

        String plain;
    }

    private static Column columnOf(String field) throws NoSuchFieldException {
        Field f = Holder.class.getDeclaredField(field);
        return f.getAnnotation(Column.class);
    }

    private static DaoQueryParam paramOf(String field) throws NoSuchFieldException {
        Field f = Holder.class.getDeclaredField(field);
        return f.getAnnotation(DaoQueryParam.class);
    }

    @Test
    void columnTakesPriorityOverEverything() throws NoSuchFieldException {
        DaoQueryFieldInfo info = new DaoQueryFieldInfo(
                "javaFieldName",
                paramOf("paramAnnotated"),
                String.class,
                columnOf("columnAnnotated"));

        assertThat(info.getSqlName()).isEqualTo("user_id");
    }

    @Test
    void paramFallbackWhenNoColumn() throws NoSuchFieldException {
        DaoQueryFieldInfo info = new DaoQueryFieldInfo(
                "javaFieldName",
                paramOf("paramAnnotated"),
                String.class,
                null);

        assertThat(info.getSqlName()).isEqualTo("paramName");
    }

    @Test
    void nameFallbackWhenNeitherAnnotationPresent() {
        DaoQueryFieldInfo info = new DaoQueryFieldInfo("javaFieldName", null, String.class, null);

        assertThat(info.getSqlName()).isEqualTo("javaFieldName");
    }

    @Test
    void gettersReturnConstructorArgs() throws NoSuchFieldException {
        DaoQueryParam param = paramOf("paramAnnotated");
        Column col = columnOf("columnAnnotated");

        DaoQueryFieldInfo info = new DaoQueryFieldInfo("n", param, Integer.class, col);

        assertThat(info.getName()).isEqualTo("n");
        assertThat(info.getParam()).isSameAs(param);
        assertThat(info.getType()).isEqualTo(Integer.class);
        assertThat(info.getColumn()).isSameAs(col);
    }
}
