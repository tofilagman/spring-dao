# spring-dao
![Github Issues](https://img.shields.io/github/issues/tofilagman/spring-dao.svg) ![Github Stars](https://img.shields.io/github/stars/tofilagman/spring-dao.svg) ![Java](https://img.shields.io/badge/java-100%25-brightgreen.svg) ![LICENSE](https://img.shields.io/badge/license-MIT-blue.svg)

# about spring-dao

Running native queries to relational database using Java often leaves the source code confusing and extensive, when one has too many filter conditions and also changes in table bindings.
 
By default dao query xml files must be added to a folder named "daoQuery" inside the resource folder. Remember, the file name must be the same as the class name.

I recommend enabling jdbc as it was tested for production use. jdbc is enabled by default.

This project was inspired and most of the classes are acquired from ![gasparbarancelli/spring-native-query](https://github.com/gasparbarancelli/spring-native-query)

minimum jdk requirements: 17.0.2
 
# Example

In your project add the dependency of the library, let's take an example using maven.
  
```
<dependency>
    <groupId>com.github.tofilagman</groupId>
    <artifactId>spring-dao</artifactId>
    <version>0.0.2</version>
</dependency>
```   
If you are using Spring Boot 3, you must tell Spring to scan the io.github package, as follows:
@ComponentScan(basePackages = {"io.github", "here is your application package"})

Inside the resource folder create a file named data.sql and insert the script.

```sql
CREATE TABLE USER (
  cod INT NOT NULL,
  full_name VARCHAR(45) NULL,
  active INT NULL,
  PRIMARY KEY (cod)
);

INSERT INTO USER (cod, full_name, active)
VALUES (1, 'Gaspar', 1),
       (2, 'Elton', 1),
       (3, 'Lucini', 1),
       (4, 'Diogo', 1),
       (5, 'Daniel', 1),
       (6, 'Marcos', 1),
       (7, 'Fernanda', 1),
       (8, 'Maicon', 1),
       (9, 'Rafael', 0);
```

First define in your configuration file the package scan of your project, The files application.properties, bootstrap.properties, application.yaml, application.yml, bootstrap.yml and bootstrap.yaml are supported, the property.

If you use properties file

``` properties
logging.level.org.r3al.springdao=debug
dao-query.package-scan=com.example.project1
dao-query.use-hibernate-types=true
dao-query.use-jdbc=true
spring.jpa.hibernate.naming.implicit-strategy=org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
```
  
We can also define programatically implementing the interface DaoQueryConfig.

``` java
import org.r3al.springdao.DaoQueryConfig;

public class DaoQueryDefaultConfig implements DaoQueryConfig {

    @Override
    public String getPackageScan() {
        return "com.example.project1";
    } 

    @Override
    public boolean getUseHibernateTypes() {
        return false;
    }

}
```

UserTO file example

```java
import lombok.*;
import org.r3al.springdao.DaoQueryDomain
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Data
@Entity
public class UserTO extends DaoQueryDomain {
   @Id
  private Number id;
  private String name;

}
```

```kotlin
 
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.r3al.springdao.DaoQueryDomain
import com.example.project1.NoArg

@NoArg
@Entity
data class UserTO(
    @Id
    var id: Long, 
    var name: String
) : DaoQueryDomain()
```

**Using jdbc** : no need to add jakarta and NoArg annotations

```kotlin

import jakarta.persistence.Column 
import org.r3al.springdao.annotations.DaoQueryRowMapper

@DaoQueryRowMapper(mapper = UserTOMapper::class)
data class UserTO( 
    var id: Long, 
    var name: String,
    @Column(name = "active")
    var active: Boolean
)  

class UserTOMapper : RowMapper<UserTO> {
    override fun mapRow(rs: ResultSet, rowNum: Int): UserTO {
       return UserTO(
           id = rs.getLong("id"), 
           name = rs.getString("name")
       )
    } 
}

```
 
UserDaoQuery file example

```java
import org.r3al.springdao.DaoQuery
import org.r3al.springdao.DaoQueryListResult
import org.r3al.springdao.DaoQueryListToken

import java.util.List;


public interface UserDaoQuery extends DaoQuery {

  List<UserTO> findUsers();
 
  @DaoQuerySql("SELECT cod as \"id\", full_name as \"name\" FROM USER")
  List<UserTO> findBySqlInline();

  List<UserTO> findWithCondition(DaoQueryCondition filter);
   
  List<UserTO> findUsersBySql(DaoQuerySql filter);
  
  // Add pagination
  DaoQueryListResult<UserTO> findActiveUsers(DaoQueryListToken pageable);
   
  List<UserTO> findbyId(long id); 
}
```

UserDaoQuery.xml file example

```xml
<?xml version="1.0" ?>

<database>
    <sql id="findUsers" lang="hbs" oneline="true">
        <![CDATA[
            SELECT cod as "id", full_name as "name" FROM USER
        ]]>
    </sql>
    <sql id="findWithCondition" lang="hbs" oneline="true">
        <![CDATA[
            SELECT cod as "id", full_name as "name" FROM USER Where {{filter}}
        ]]>
    </sql>
    <sql id="findUsersBySql" lang="hbs" oneline="true">
        <![CDATA[
            SELECT cod as "id", full_name as "name" FROM USER Where {{filter}}
        ]]>
    </sql>
    <sql id="findActiveUsers" lang="hbs" oneline="true">
        <![CDATA[
            {{#query queryType}}
                SELECT cod as "id", full_name as "name" FROM USER
                    limit {{skip}}, {{take}}
            {{/query}}
            {{#count queryType}}
                SELECT count(1) FROM USER
            {{/count}}
        ]]>
    </sql>
     <sql id="findbyId" lang="hbs" oneline="true">
        <![CDATA[
            SELECT cod as "id", full_name as "name" FROM USER 
            Where id = :id
        ]]>
    </sql>
</database>
 
```

**Configure custom Handlebar helpers**

SpringDaoConfiguration.java file example

```java
import org.springframework.context.annotation.Configuration
import org.r3al.springdao.templates.DaoQueryTemplateHelper
import org.springframework.context.annotation.Bean

@Configuration
public class SpringDaoConfiguration {

    @Bean
    public DaoQueryTemplateHelper templateHelper(){
        return new DaoQueryTemplateHelper.register(new HandleBarHelpers());
    }
}

```

**Kotlin NoArg requirements**

```xml
<build>
		<sourceDirectory>${project.basedir}/src/main/kotlin</sourceDirectory>
		<testSourceDirectory>${project.basedir}/src/test/kotlin</testSourceDirectory>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>org.jetbrains.kotlin</groupId>
				<artifactId>kotlin-maven-plugin</artifactId>
				<configuration>
					<args>
						<arg>-Xjsr305=strict</arg>
					</args>
					<compilerPlugins>
						<plugin>spring</plugin>
						<plugin>no-arg</plugin>
					</compilerPlugins>
					<pluginOptions>
						<option>no-arg:annotation=com.example.project1.NoArg</option>
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
		</plugins>
	</build>
```

```kotlin
package com.example.project1

annotation class NoArg
```

**Enum Conversion**
```kotlin

///Option A
@Configuration
class WebConfiguration {

    @Bean
    fun conversion(): ConversionServiceFactoryBean {
        val bean = ConversionServiceFactoryBean()
        bean.setConverters(
            setOf(
                SyncTypeConverter(),
                SyncTypeIntConverter()
            )
        )
        return bean
    }
}

///Option B
@Configuration
class WebConfiguration : WebMvcConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(SyncTypeConverter())
        //spring dao
        registry.addConverter(SyncTypeIntConverter())
        super.addFormatters(registry)
    }
}


class SyncTypeConverter : Converter<String, SyncType> {
    override fun convert(source: String): SyncType? {
        return if (source.isNumeric())
            SyncType.getByValue(Integer.parseInt(source))
        else
            SyncType.valueOf(source)
    }
}

class SyncTypeIntConverter : Converter<SyncType, Int> {
    override fun convert(source: SyncType): Int {
        return source.toValue()
    }
}

 fun String.isNumeric(): Boolean {
    return try {
        Integer.parseInt(this)
        true
    } catch (e: NumberFormatException) {
        false
    }
}

```
