package org.r3al.springdao.templates;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.r3al.springdao.ApplicationContextProvider;
import org.r3al.springdao.DaoQueryTemplateDataType;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandleBarTemplateTest {

    private static ApplicationContext previousContext;

    @BeforeAll
    static void stubApplicationContext() throws Exception {
        previousContext = readStaticContext();

        // process() asks the context for a DaoQueryTemplateHelper bean and gracefully handles its absence.
        // Provide a stub that simulates "no such bean".
        ApplicationContext stub = mock(ApplicationContext.class);
        when(stub.getBean(any(Class.class)))
                .thenThrow(new NoSuchBeanDefinitionException("DaoQueryTemplateHelper"));
        writeStaticContext(stub);
    }

    @AfterAll
    static void restoreApplicationContext() throws Exception {
        writeStaticContext(previousContext);
    }

    @Test
    void queryBlockEmittedWhenTypeIsQuery() throws IOException {
        String out = render(
                "{{#query queryType}}SELECT 1{{/query}}{{#count queryType}}SELECT count(1){{/count}}",
                Map.of("queryType", DaoQueryTemplateDataType.QUERY)
        );

        assertThat(out).contains("SELECT 1").doesNotContain("count(1)");
    }

    @Test
    void countBlockEmittedWhenTypeIsCount() throws IOException {
        String out = render(
                "{{#query queryType}}SELECT 1{{/query}}{{#count queryType}}SELECT count(1){{/count}}",
                Map.of("queryType", DaoQueryTemplateDataType.COUNT)
        );

        assertThat(out).contains("count(1)").doesNotContain("SELECT 1");
    }

    @Test
    void returnBlockEmittedWhenTypeIsReturn() throws IOException {
        String out = render(
                "{{#return queryType}}SELECT last_insert_id(){{/return}}",
                Map.of("queryType", DaoQueryTemplateDataType.RETURN)
        );

        assertThat(out).contains("last_insert_id()");
    }

    @Test
    void returnBlockSuppressedWhenTypeIsQuery() throws IOException {
        String out = render(
                "{{#return queryType}}IGNORED{{/return}}",
                Map.of("queryType", DaoQueryTemplateDataType.QUERY)
        );

        assertThat(out).doesNotContain("IGNORED");
    }

    @Test
    void nullOrZeroFiresFnBranchWhenValueIsNull() throws IOException {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("v", null);

        String out = render("{{#nullOrZero v}}WAS_NULL{{else}}HAD_VALUE{{/nullOrZero}}", ctx);

        assertThat(out).contains("WAS_NULL");
    }

    @Test
    void nullOrZeroFiresFnBranchWhenValueIsZero() throws IOException {
        String out = render("{{#nullOrZero v}}WAS_ZERO{{else}}HAD_VALUE{{/nullOrZero}}", Map.of("v", 0));

        assertThat(out).contains("WAS_ZERO");
    }

    @Test
    void nullOrZeroFiresInverseWhenValuePositive() throws IOException {
        String out = render("{{#nullOrZero v}}WAS_NULL{{else}}HAD_VALUE{{/nullOrZero}}", Map.of("v", 5));

        assertThat(out).contains("HAD_VALUE");
    }

    @Test
    void queryNullableRendersValueAsString() throws IOException {
        String out = render("name={{queryNullable v}}", Map.of("v", "alice"));

        assertThat(out).contains("name=alice");
    }

    @Test
    void queryNullableRendersEmptyWhenNull() throws IOException {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("v", null);

        String out = render("name=[{{queryNullable v}}]", ctx);

        assertThat(out).contains("name=[]");
    }

    @Test
    void plainSubstitutionWorks() throws IOException {
        String out = render("SELECT * FROM USER WHERE id = :{{id}}", Map.of("id", "userId"));

        assertThat(out).isEqualTo("SELECT * FROM USER WHERE id = :userId");
    }

    private static String render(String template, Map<String, Object> data) throws IOException {
        return new HandleBarTemplate().process(template, data);
    }

    // --- ApplicationContextProvider has a private static field that's set by Spring's lifecycle.
    // For pure unit tests we install/restore the field directly.
    private static ApplicationContext readStaticContext() throws Exception {
        Field f = ApplicationContextProvider.class.getDeclaredField("context");
        f.setAccessible(true);
        return (ApplicationContext) f.get(null);
    }

    private static void writeStaticContext(ApplicationContext ctx) throws Exception {
        Field f = ApplicationContextProvider.class.getDeclaredField("context");
        f.setAccessible(true);
        f.set(null, ctx);
    }
}
