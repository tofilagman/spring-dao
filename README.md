# spring-dao

![Java](https://img.shields.io/badge/java-17%2B-brightgreen.svg)
![Spring Boot](https://img.shields.io/badge/spring--boot-4.x-6db33f.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Issues](https://img.shields.io/github/issues/tofilagman/spring-dao.svg)
![Stars](https://img.shields.io/github/stars/tofilagman/spring-dao.svg)

> **Write SQL outside your Java.** Declare a repository interface, drop the SQL alongside in an XML file, and `spring-dao` builds a Spring bean for it. No JPQL, no per-method annotations, no scaffolding.

Inspired by and forked from [`gasparbarancelli/spring-native-query`](https://github.com/gasparbarancelli/spring-native-query); modernised for **Spring Boot 4**, **Hibernate 7**, **Java 17+**.

---

## Table of contents

- [Why](#why)
- [Choosing this vs alternatives](#choosing-this-vs-alternatives)
- [Install](#install)
- [How it works in 30 seconds](#how-it-works-in-30-seconds)
- [Quick start](#quick-start)
- [Package scanning](#package-scanning)
- [Return-type cheatsheet](#return-type-cheatsheet)
- [Pagination](#pagination)
- [Dynamic filtering](#dynamic-filtering)
- [JdbcTemplate vs EntityManager](#jdbctemplate-vs-entitymanager)
- [Multiple databases](#multiple-databases)
- [Handlebars helpers](#handlebars-helpers)
- [Kotlin support](#kotlin-support)
- [Enum conversion](#enum-conversion)
- [Configuration reference](#configuration-reference)

---

## Why

Hand-written native SQL inside Java gets tangled fast — string concatenation for dynamic filters, schema changes that ripple through quoted column names, and giant method bodies whose actual business intent is buried. `spring-dao` moves the SQL into external **Handlebars-powered XML templates** keyed by method name, so your Java side stays a clean interface and your SQL side reads like, well, SQL.

## Choosing this vs alternatives

There are several mature options in this space; `spring-dao` occupies a deliberate niche. Use the table to self-qualify before reading the rest of the README.

| Tool | Style | Pick it when… |
|---|---|---|
| **`spring-dao`** *(this project)* | External SQL in XML, Handlebars templating, interface method = SQL id | You want SQL visible and external, but find MyBatis's XML grammar verbose and JPA's abstraction leaky. Spring Boot 4 / Java 17 first. |
| **Spring Data JPA** | Derived queries + `@Query`; entity-driven | The schema is yours to shape, SQL is incidental, and method-name magic (`findByEmailAndActiveTrue`) earns its keep. |
| **MyBatis** | XML mappers with `<if>` / `<foreach>` / `<choose>` tags | You need a mature ecosystem: plugins, type handlers, dialect packs, vendor support, long-term stability. |
| **JOOQ** | Type-safe SQL DSL in Java, generated from schema | Compile-time SQL correctness is worth a code-gen step in the build. |
| **`NamedParameterJdbcTemplate`** (raw) | SQL strings inline in repository code | One-off service, lowest dependency footprint, no abstraction wanted. |
| **JPA `@NamedNativeQuery`** | Native SQL via annotations on entities | You're already JPA-heavy and just need an occasional escape hatch. |

**What `spring-dao` trades away.** There is no IDE jump from interface method → XML file, no SQL syntax check at compile time, and a typo in a column name (or a method/sql-id mismatch) surfaces only at runtime. If those matter more than terse XML and zero ceremony, pick **JOOQ** for safety or **MyBatis** for tooling depth.

**The honest niche.** A small-to-mid Spring Boot 4 / Java 17 project where SQL is a first-class language, you don't want JPA's surprises, and MyBatis feels too XML-heavy for what you're trying to do.

## Install

```xml
<dependency>
  <groupId>com.github.tofilagman</groupId>
  <artifactId>spring-dao</artifactId>
  <version>0.0.2</version>
</dependency>
```

**Requirements:** JDK 17.0.2+, Spring Boot 4.x (uses `jakarta.persistence`). Kotlin 1.4+ optional.

## How it works in 30 seconds

1. You define an **interface** extending `DaoQuery`. Each method's signature declares its return shape (entity, list, optional, pageable, void, batch).
2. You drop an **XML file** named after the interface in `src/main/resources/daoQuery/`. Each `<sql id="..."/>` matches a method name.
3. `spring-dao` scans your packages on startup, builds a proxy for each interface, and registers it as a Spring bean. Inject it like any other repository.

```java
@Service
class UserService {
    private final UserDaoQuery userDao; // injected

    UserService(UserDaoQuery userDao) { this.userDao = userDao; }

    List<UserTO> activeUsers() {
        return userDao.findActiveUsers(new DaoQueryListToken(0, 50)).getData();
    }
}
```

## Quick start

### 1. Define the DAO interface

```java
import org.r3al.springdao.*;
import org.r3al.springdao.annotations.*;
import java.util.List;
import java.util.Optional;

public interface UserDaoQuery extends DaoQuery {

    List<UserTO> findUsers();

    UserTO findById(long id);

    Optional<UserTO> findByEmail(String email);

    DaoQueryListResult<UserTO> findActiveUsers(DaoQueryListToken pageable);

    @DaoQuerySql("SELECT cod AS \"id\", full_name AS \"name\" FROM USER")
    List<UserTO> findUsersInline();

    @DaoQueryBatch
    void insertBatch(List<UserTO> items);
}
```

### 2. Pair it with SQL

`src/main/resources/daoQuery/UserDaoQuery.xml`:

```xml
<?xml version="1.0" ?>
<database>
    <sql id="findUsers" lang="hbs" oneline="true">
        <![CDATA[
            SELECT cod AS "id", full_name AS "name" FROM USER
        ]]>
    </sql>

    <sql id="findById" lang="hbs" oneline="true">
        <![CDATA[
            SELECT cod AS "id", full_name AS "name" FROM USER WHERE cod = :id
        ]]>
    </sql>

    <sql id="findActiveUsers" lang="hbs" oneline="true">
        <![CDATA[
            {{#query queryType}}
                SELECT cod AS "id", full_name AS "name" FROM USER
                WHERE active = 1
                LIMIT {{skip}}, {{take}}
            {{/query}}
            {{#count queryType}}
                SELECT COUNT(1) FROM USER WHERE active = 1
            {{/count}}
        ]]>
    </sql>

    <sql id="insertBatch" lang="hbs" oneline="true">
        <![CDATA[
            INSERT INTO USER (name, active) VALUES (:name, :active)
        ]]>
    </sql>
</database>
```

### 3. Define the data class

```java
import org.r3al.springdao.DaoQueryDomain;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class UserTO extends DaoQueryDomain {
    @Id
    private Long id;
    private String name;
}
```

That's the entire setup. **No `@Repository`, no manual bean registration.** Inject `UserDaoQuery` anywhere Spring can.

## Package scanning

By default `spring-dao` scans the same packages as your `@SpringBootApplication` (via Spring's `AutoConfigurationPackages`). **If your DAO interfaces are inside the application's package tree, you don't need to configure anything.**

For DAOs that live elsewhere, mark a `@Configuration` class with `@DaoQueryScan`:

```java
@Configuration
@DaoQueryScan(basePackages = "com.example.repos")
public class DaoQueryConfig { }
```

You can also point at a class instead of a package string (refactor-safe):

```java
@DaoQueryScan(basePackageClasses = UserDaoQuery.class)
```

> **Legacy:** the property `dao-query.package-scan=com.example.repos` is still honored additively for backward compatibility, but `@DaoQueryScan` is preferred.

## Return-type cheatsheet

| Method return type | Behavior |
|---|---|
| `T` (single) | One row mapped to `T`. Empty result → `null`. |
| `Optional<T>` | One row, empty `Optional` on no match. |
| `List<T>` / `Iterable<T>` | Multi-row mapped to a list. |
| `DaoQueryListResult<T>` | Data **and** count. SQL must define both `{{#query}}` and `{{#count}}` blocks. |
| `void` | Executes update — `INSERT`/`UPDATE`/`DELETE`. |
| `void` + `@DaoQueryBatch` | Batch update; accepts a `List<T>` parameter and binds each element. |

## Pagination

Any argument that implements `DaoQueryListTokenBase` makes `:skip` and `:take` available inside the template:

```java
DaoQueryListResult<UserTO> page(DaoQueryListToken pageable);
```

```xml
<sql id="page" lang="hbs" oneline="true">
    <![CDATA[
        {{#query queryType}}
            SELECT cod AS "id", full_name AS "name" FROM USER LIMIT {{skip}}, {{take}}
        {{/query}}
        {{#count queryType}}
            SELECT COUNT(1) FROM USER
        {{/count}}
    ]]>
</sql>
```

```java
DaoQueryListResult<UserTO> page = userDao.page(new DaoQueryListToken(0, 20));
page.getData();   // List<UserTO>
page.getCount();  // total rows ignoring LIMIT
```

## Dynamic filtering

For runtime-built `WHERE` clauses without string concatenation, declare a `DaoQueryCondition` parameter and reference it in the template:

```java
List<UserTO> findWithCondition(DaoQueryCondition filter);
```

```xml
<sql id="findWithCondition" lang="hbs" oneline="true">
    <![CDATA[
        SELECT cod AS "id", full_name AS "name" FROM USER WHERE {{filter}}
    ]]>
</sql>
```

```java
DaoQueryCondition filter = DaoQueryCondition.INSTANCE();
filter.and("active = $active", 1);
filter.and("full_name like $name", "Tofi", DaoQueryConditionType.LIKE);
userDao.findWithCondition(filter);
```

Available condition types: `DEFAULT`, `LIKE`, `BEGIN_LIKE`, `END_LIKE`. Use `.group(otherCondition, AND|OR)` to nest. Use `DaoQuerySql.of("...", args...)` as a lower-level escape hatch when you need to inject a whole SQL fragment, not just a `WHERE`.

## JdbcTemplate vs EntityManager

`spring-dao` defaults to `NamedParameterJdbcTemplate`. Switch to Hibernate's `EntityManager` (so results flow through your `@Entity` registry and custom Hibernate user types) globally:

```properties
dao-query.use-jdbc=false
dao-query.use-hibernate-types=true
```

…or per-method:

```java
@DaoQueryUseJdbcTemplate          // force JDBC
@DaoQueryUseHibernateTypes        // force Hibernate types
```

When using JDBC mode you can skip the JPA annotations entirely and supply a `RowMapper` via `@DaoQueryRowMapper` on the data class:

```java
@DaoQueryRowMapper(mapper = UserTOMapper.class)
public class UserTO {
    private Long id;
    private String name;
    @Column(name = "active") private boolean active;
}

public class UserTOMapper implements RowMapper<UserTO> { /* … */ }
```

## Multiple databases

To talk to more than one database from the same application, declare each datasource (and its `EntityManager` / `NamedParameterJdbcTemplate`) with a Spring `@Qualifier`, then tag the DAO interface with `@DaoQueryDataSource("name")` matching that qualifier:

```java
@Configuration
public class DataSources {

    @Bean @Primary
    @ConfigurationProperties("spring.datasource.primary")
    DataSource primaryDs() { return DataSourceBuilder.create().build(); }

    @Bean @Qualifier("reporting")
    @ConfigurationProperties("spring.datasource.reporting")
    DataSource reportingDs() { return DataSourceBuilder.create().build(); }

    @Bean @Qualifier("reporting")
    NamedParameterJdbcTemplate reportingJdbc(@Qualifier("reporting") DataSource ds) {
        return new NamedParameterJdbcTemplate(ds);
    }
}
```

```java
public interface UserDaoQuery extends DaoQuery {
    // no annotation → uses the @Primary datasource
}

@DaoQueryDataSource("reporting")
public interface ReportingDaoQuery extends DaoQuery {
    // routes to the @Qualifier("reporting") bean
}
```

**Notes:**

- The qualifier is class-level (one DAO ↔ one database). For two databases, define two DAO interfaces.
- DAOs without `@DaoQueryDataSource` keep the previous behavior — unqualified `getBean` lookup, which finds the `@Primary` bean or the single match.
- For transactions, use Spring's per-manager qualifier as usual: `@Transactional("reportingTxManager")` on the service method. The library doesn't manage transactions itself; it just resolves the right `EntityManager` so it participates in whatever transaction context Spring sets up.
- Database vendor types may differ across qualifiers (e.g., PostgreSQL primary + Oracle reporting). The library is dialect-agnostic — you just write SQL appropriate for each target in its respective XML file.

## Handlebars helpers

Templates are processed by Handlebars. Built-in helpers:

| Helper | Purpose |
|---|---|
| `{{#query queryType}}…{{/query}}` | Emit only when rendering the **data query** |
| `{{#count queryType}}…{{/count}}` | Emit only when rendering the **count query** (used with `DaoQueryListResult`) |
| `{{#return queryType}}…{{/return}}` | Emit only when rendering a **post-write return query** (e.g., fetch the row just inserted) |
| `{{#nullOrZero v}}…{{else}}…{{/nullOrZero}}` | Branch on null-or-zero |
| `{{queryNullable v}}` | Render value, or empty string if `null` |

### Adding your own helpers

```java
@Configuration
public class SpringDaoConfiguration {

    @Bean
    public DaoQueryTemplateHelper templateHelper() {
        return DaoQueryTemplateHelper.register(new MyHelpers());
    }

    public static class MyHelpers {
        public CharSequence upper(String value) {
            return value == null ? "" : value.toUpperCase();
        }
    }
}
```

`MyHelpers` is registered with Handlebars automatically; each public method becomes a `{{upper foo}}` style helper.

## Kotlin support

```kotlin
@NoArg
@Entity
data class UserTO(
    @Id var id: Long,
    var name: String
) : DaoQueryDomain()
```

```kotlin
package com.example
annotation class NoArg
```

Kotlin's `data class` has no no-arg constructor by default; Hibernate needs one. The [`no-arg` compiler plugin](https://kotlinlang.org/docs/no-arg-plugin.html) generates one for any class annotated with `@NoArg`:

```xml
<plugin>
  <groupId>org.jetbrains.kotlin</groupId>
  <artifactId>kotlin-maven-plugin</artifactId>
  <configuration>
    <compilerPlugins>
      <plugin>spring</plugin>
      <plugin>no-arg</plugin>
    </compilerPlugins>
    <pluginOptions>
      <option>no-arg:annotation=com.example.NoArg</option>
    </pluginOptions>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>org.jetbrains.kotlin</groupId>
      <artifactId>kotlin-maven-allopen</artifactId>
      <version>${kotlin.version}</version>
    </dependency>
    <dependency>
      <groupId>org.jetbrains.kotlin</groupId>
      <artifactId>kotlin-maven-noarg</artifactId>
      <version>${kotlin.version}</version>
    </dependency>
  </dependencies>
</plugin>
```

If you use **JDBC mode** (default), you can skip the JPA + no-arg dance entirely and supply a `RowMapper`:

```kotlin
@DaoQueryRowMapper(mapper = UserTOMapper::class)
data class UserTO(
    var id: Long,
    var name: String,
    @Column(name = "active") var active: Boolean
)

class UserTOMapper : RowMapper<UserTO> {
    override fun mapRow(rs: ResultSet, rowNum: Int) = UserTO(
        id = rs.getLong("id"),
        name = rs.getString("name"),
        active = rs.getBoolean("active")
    )
}
```

## Enum conversion

For binding enum-valued parameters to SQL, register a Spring `Converter<MyEnum, Integer>` (or whatever your DB representation is). `spring-dao` looks up the `ConversionService` automatically and uses it to coerce enum arguments before binding.

```kotlin
@Configuration
class WebConfiguration : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(SyncTypeConverter())
        registry.addConverter(SyncTypeIntConverter())
    }
}

class SyncTypeConverter : Converter<String, SyncType> {
    override fun convert(source: String): SyncType =
        if (source.toIntOrNull() != null) SyncType.getByValue(source.toInt())!!
        else SyncType.valueOf(source)
}

class SyncTypeIntConverter : Converter<SyncType, Int> {
    override fun convert(source: SyncType): Int = source.value
}

enum class SyncType(val value: Int) {
    System(1), Administrator(2), Supervisor(3);

    companion object {
        @JvmStatic
        fun getByValue(value: Int?) = entries.firstOrNull { it.value == value }
    }
}
```

## Configuration reference

| Property | Default | Description |
|---|---|---|
| `dao-query.package-scan` | *(auto-detected)* | Extra package(s) to scan. Prefer `@DaoQueryScan`. |
| `dao-query.use-jdbc` | `true` | `true` → `NamedParameterJdbcTemplate`; `false` → Hibernate `EntityManager`. |
| `dao-query.use-hibernate-types` | `true` | Register Hibernate user types for `aliasToBean` (EntityManager path only). |
| `dao-query.sql.directory` | `daoQuery` | Resource directory holding the XML files. |
| `logging.level.org.r3al.springdao` | `info` | Set to `debug` to log every rendered SQL + bound parameters. |

You can also implement these programmatically:

```java
public class DaoQueryDefaultConfig implements DaoQueryConfig {
    @Override public String getPackageScan() { return "com.example.repos"; }
    @Override public boolean getUseHibernateTypes() { return false; }
}
```

Supported config files: `application.properties`, `bootstrap.properties`, `application.yaml`, `application.yml`, `bootstrap.yml`, `bootstrap.yaml`.

---

## License

MIT.
