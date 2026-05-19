package org.r3al.springdao;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;
import org.r3al.springdao.annotations.DaoQueryParam;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryAccessMethodTest {

    @SuppressWarnings("unused")
    static class Bean {
        public String getName() { return null; }

        public boolean isActive() { return false; }

        @Column(name = "user_id")
        public Long getId() { return null; }

        @DaoQueryParam("aliased")
        public String getAliased() { return null; }

        public List<String> getTags() { return null; }
    }

    private Method method(String name) throws NoSuchMethodException {
        return Bean.class.getDeclaredMethod(name);
    }

    @Test
    void getPrefixStrippedFromName() throws NoSuchMethodException {
        DaoQueryAccessMethod am = new DaoQueryAccessMethod(method("getName"));

        assertThat(am.getName()).isEqualTo("Name");
        assertThat(am.getType()).isEqualTo(String.class);
        assertThat(am.getColumn()).isNull();
        assertThat(am.getParam()).isNull();
    }

    @Test
    void isPrefixStrippedFromName() throws NoSuchMethodException {
        DaoQueryAccessMethod am = new DaoQueryAccessMethod(method("isActive"));

        assertThat(am.getName()).isEqualTo("Active");
        assertThat(am.getType()).isEqualTo(boolean.class);
    }

    @Test
    void columnAnnotationDrivesSqlName() throws NoSuchMethodException {
        DaoQueryAccessMethod am = new DaoQueryAccessMethod(method("getId"));

        assertThat(am.getColumn()).isNotNull();
        assertThat(am.getColumn().name()).isEqualTo("user_id");
        assertThat(am.getSqlName()).isEqualTo("user_id");
    }

    @Test
    void paramAnnotationExposedButSqlNameStaysAsParsedName() throws NoSuchMethodException {
        DaoQueryAccessMethod am = new DaoQueryAccessMethod(method("getAliased"));

        assertThat(am.getParam()).isNotNull();
        assertThat(am.getParam().value()).isEqualTo("aliased");
        assertThat(am.getSqlName()).isEqualTo("Aliased");
    }

    @Test
    void parameterizedReturnTypeUsesRawType() throws NoSuchMethodException {
        DaoQueryAccessMethod am = new DaoQueryAccessMethod(method("getTags"));

        assertThat(am.getType()).isEqualTo(List.class);
    }

    @Test
    void methodReferencePreserved() throws NoSuchMethodException {
        Method m = method("getName");
        DaoQueryAccessMethod am = new DaoQueryAccessMethod(m);

        assertThat(am.getMethod()).isSameAs(m);
    }
}
