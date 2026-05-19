package org.r3al.springdao;

import org.junit.jupiter.api.Test;
import org.r3al.springdao.fixtures.SampleDaoQuery;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DaoQueryResourceLoaderTest {

    private static DaoQueryResourceLoader loaderFor(String packageName) throws Exception {
        // Constructor is package-private.
        Constructor<DaoQueryResourceLoader> ctor =
                DaoQueryResourceLoader.class.getDeclaredConstructor(String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(packageName);
    }

    @Test
    void loadsAllSqlPatternsFromXml() throws Exception {
        DaoQueryResourceLoader loader = loaderFor("org.r3al.springdao.fixtures");

        List<DaoQuerySqlPattern> patterns = loader.loadResource(SampleDaoQuery.class);

        Map<String, DaoQuerySqlPattern> byKey = patterns.stream()
                .collect(Collectors.toMap(DaoQuerySqlPattern::getKey, p -> p));
        assertThat(byKey).containsKeys("findAll", "findOneLine", "rawText");
    }

    @Test
    void onelineTrueCollapsesWhitespace() throws Exception {
        DaoQueryResourceLoader loader = loaderFor("org.r3al.springdao.fixtures");

        DaoQuerySqlPattern oneLine = loader.loadResource(SampleDaoQuery.class).stream()
                .filter(p -> "findOneLine".equals(p.getKey()))
                .findFirst()
                .orElseThrow();

        assertThat(oneLine.template).doesNotContain("\n");
        assertThat(oneLine.template).contains("SELECT id, name").contains("FROM USER").contains("WHERE id = :id");
    }

    @Test
    void onelineFalseKeepsNewlines() throws Exception {
        DaoQueryResourceLoader loader = loaderFor("org.r3al.springdao.fixtures");

        DaoQuerySqlPattern multi = loader.loadResource(SampleDaoQuery.class).stream()
                .filter(p -> "findAll".equals(p.getKey()))
                .findFirst()
                .orElseThrow();

        assertThat(multi.template).contains("SELECT id, name FROM USER");
    }

    @Test
    void templateLanguageAttributeCaptured() throws Exception {
        DaoQueryResourceLoader loader = loaderFor("org.r3al.springdao.fixtures");

        DaoQuerySqlPattern hbs = loader.loadResource(SampleDaoQuery.class).stream()
                .filter(p -> "findAll".equals(p.getKey()))
                .findFirst()
                .orElseThrow();

        assertThat(hbs.templateLanguage).isEqualTo("hbs");

        DaoQuerySqlPattern plain = loader.loadResource(SampleDaoQuery.class).stream()
                .filter(p -> "rawText".equals(p.getKey()))
                .findFirst()
                .orElseThrow();

        assertThat(plain.templateLanguage).isEqualTo("plain");
    }

    @Test
    void missingResourceThrows() throws Exception {
        DaoQueryResourceLoader loader = loaderFor("org.r3al.springdao");

        // SampleDaoQuery lives in org.r3al.springdao.fixtures, but we pass "org.r3al.springdao" as packageName.
        // The leftover ".fixtures.SampleDaoQuery" produces "daoQuery/fixtures/SampleDaoQuery.xml",
        // which does not exist, so loadResource raises.
        assertThatThrownBy(() -> loader.loadResource(SampleDaoQuery.class))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot be opened");
    }
}
