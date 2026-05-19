package org.r3al.springdao;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.r3al.springdao.annotations.DaoQueryBatch;
import org.r3al.springdao.annotations.DaoQuerySql;
import org.r3al.springdao.annotations.DaoQueryUseJdbcTemplate;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DaoQueryInfoTest {

    static class Bean {
        Long id;
        String name;
    }

    interface SampleDao extends DaoQuery {
        Bean findOne(long id);

        List<Bean> findAll();

        Optional<Bean> findOptional(long id);

        DaoQueryListResult<Bean> page(DaoQueryListToken token);

        void deleteAll();

        @DaoQuerySql("SELECT 1")
        Integer inline();

        @DaoQueryBatch
        void insertBatch(List<Bean> items);

        @DaoQueryUseJdbcTemplate
        Bean withJdbc();
    }

    private static MethodInvocation invocationOf(String methodName, Class<?>... paramTypes) throws Exception {
        Method method = SampleDao.class.getMethod(methodName, paramTypes);
        MethodInvocation inv = mock(MethodInvocation.class);
        when(inv.getMethod()).thenReturn(method);
        return inv;
    }

    @Test
    void getPackageNameForJavaStringIsJavaLang() {
        assertThat(DaoQueryInfo.getPackageName(String.class)).isEqualTo("java.lang");
    }

    @Test
    void getPackageNameForPrimitiveIntIsJavaLang() {
        assertThat(DaoQueryInfo.getPackageName(int.class)).isEqualTo("java.lang");
    }

    @Test
    void getPackageNameForArrayUsesComponentTypePackage() {
        assertThat(DaoQueryInfo.getPackageName(Bean[].class)).isEqualTo("org.r3al.springdao");
    }

    @Test
    void singleReturnTypeIsNotIterable() throws Exception {
        DaoQueryInfo info = DaoQueryInfo.of(SampleDao.class, invocationOf("findOne", long.class));

        assertThat(info.isSingleResult()).isTrue();
        assertThat(info.returnTypeIsOptional()).isFalse();
        assertThat(info.returnTypeIsListResult()).isFalse();
        assertThat(info.getAliasToBean()).isEqualTo(Bean.class);
        assertThat(info.getReturnType()).isEqualTo(Bean.class);
    }

    @Test
    void listReturnTypeUnwrappedToComponent() throws Exception {
        DaoQueryInfo info = DaoQueryInfo.of(SampleDao.class, invocationOf("findAll"));

        assertThat(info.isSingleResult()).isFalse();
        assertThat(info.returnTypeIsListResult()).isFalse();
        assertThat(info.getAliasToBean()).isEqualTo(Bean.class);
    }

    @Test
    void optionalReturnTypeRecognized() throws Exception {
        DaoQueryInfo info = DaoQueryInfo.of(SampleDao.class, invocationOf("findOptional", long.class));

        assertThat(info.returnTypeIsOptional()).isTrue();
        assertThat(info.getAliasToBean()).isEqualTo(Bean.class);
    }

    @Test
    void daoQueryListResultRecognizedAndComponentExtracted() throws Exception {
        DaoQueryInfo info = DaoQueryInfo.of(SampleDao.class, invocationOf("page", DaoQueryListToken.class));

        assertThat(info.returnTypeIsListResult()).isTrue();
        assertThat(info.getAliasToBean()).isEqualTo(Bean.class);
        assertThat(info.getReturnType()).isEqualTo(DaoQueryListResult.class);
    }

    @Test
    void daoQuerySqlAnnotationMarksInline() throws Exception {
        DaoQueryInfo info = DaoQueryInfo.of(SampleDao.class, invocationOf("inline"));

        assertThat(info.isUseSqlInline()).isTrue();
        assertThat(info.getSql()).isEqualTo("SELECT 1");
    }

    @Test
    void daoQueryBatchAnnotationFlagsBatch() throws Exception {
        DaoQueryInfo info = DaoQueryInfo.of(SampleDao.class, invocationOf("insertBatch", List.class));

        assertThat(info.isBatch()).isTrue();
    }

    @Test
    void aliasToBeanFallsBackToReturnTypeForVoid() throws Exception {
        DaoQueryInfo info = DaoQueryInfo.of(SampleDao.class, invocationOf("deleteAll"));

        // Void.TYPE is in java.lang.
        assertThat(info.isJavaObject()).isTrue();
        assertThat(info.isSingleResult()).isTrue();
    }

    @Test
    void isJavaObjectFalseForUserDefinedAliasBean() throws Exception {
        DaoQueryInfo info = DaoQueryInfo.of(SampleDao.class, invocationOf("findOne", long.class));

        assertThat(info.isJavaObject()).isFalse();
    }

    @Test
    void isEntityFalseWhenAliasBeanHasNoEntityAnnotation() throws Exception {
        DaoQueryInfo info = DaoQueryInfo.of(SampleDao.class, invocationOf("findOne", long.class));

        assertThat(info.isEntity()).isFalse();
    }

    @Test
    void daoQueryUseJdbcTemplateAnnotationEnablesJdbcPath() throws Exception {
        DaoQueryInfo info = DaoQueryInfo.of(SampleDao.class, invocationOf("withJdbc"));

        assertThat(info.isUseJdbcTemplate()).isTrue();
    }
}
