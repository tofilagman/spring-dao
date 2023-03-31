package org.r3al.springdao;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PropertyUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyUtil.class);

    private static final Map<String, String> cache = new HashMap<>();

    private static final List<String> propertyFileList = Arrays.asList(
            "application.properties", "bootstrap.properties"
    );

    public static String getValue(String propertyName, String defaultValue) {
        try {
            String value = cache.get(propertyName);
            if (value != null) {
                return value;
            }
            value = getProperty(propertyName, defaultValue);
            cache.put(propertyName, value);
            return value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Optional<String> getPropertyByConfig(String propertyName) {
        String mainClass = System.getProperty("sun.java.command");
        String packageMainClass = mainClass.substring(0, mainClass.lastIndexOf("."));
        Reflections reflections = new Reflections(packageMainClass);
        Set<Class<? extends DaoQueryConfig>> subTypesOfDaoQueryConfig = reflections.getSubTypesOf(DaoQueryConfig.class);
        for (Class<? extends DaoQueryConfig> subType : subTypesOfDaoQueryConfig) {
            try {
                DaoQueryConfig config = (DaoQueryConfig) subType.getConstructors()[0].newInstance();

                switch (propertyName) {
                    case "dao-query.package-scan":
                        return Optional.ofNullable(config.getPackageScan());
                    case "dao-query.sql.directory":
                        return Optional.ofNullable(config.getSQLDirectory());
                    case "dao-query.use-hibernate-types":
                        return Optional.of(String.valueOf(config.getUseHibernateTypes()));
                    default:
                        return Optional.ofNullable(config.getFileSufix());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return Optional.empty();
    }

    private static String getProperty(String propertyName, String defaultValue) throws IOException {
        return getPropertyValue(propertyName)
                        .orElseGet(() -> getPropertyByConfig(propertyName)
                                .orElse(defaultValue));
    }

    private static Optional<String> getPropertyValue(InputStream inputStream, String propertyName) throws IOException {
        Properties prop = new Properties();
        prop.load(inputStream);
        return Optional.ofNullable(prop.getProperty(propertyName));
    }

    private static Optional<String> getPropertyValue(String propertyName) throws IOException {
        for (String propertyFile : propertyFileList) {
            Optional<InputStream> inputStreamYml = getInputStream(propertyFile);
            if (inputStreamYml.isPresent()) {
                Optional<String> value = getPropertyValue(inputStreamYml.get(), propertyName);
                if (value.isPresent()) {
                    return value;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<InputStream> getInputStream(String s) {
        InputStream inputStream = PropertyUtil.class
                .getClassLoader()
                .getResourceAsStream(s);
        return Optional.ofNullable(inputStream);
    }

}
