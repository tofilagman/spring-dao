package org.r3al.springdao

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.function.Supplier
import kotlin.collections.HashMap

object PropertyUtil {
    private val LOGGER = LoggerFactory.getLogger(PropertyUtil::class.java)
    private val cache: MutableMap<String, String?> = HashMap()
    private val mapper = ObjectMapper(YAMLFactory())
    private val yamlFileList = Arrays.asList(
        "application.yaml", "application.yml", "bootstrap.yaml", "bootstrap.yml"
    )
    private val propertyFileList = Arrays.asList(
        "application.properties", "bootstrap.properties"
    )

    fun getValue(propertyName: String, defaultValue: String): String? {
        return try {
            var value = cache[propertyName]
            if (value != null) {
                LOGGER.debug("property value obtained by cache")
                return value
            }
            value = getProperty(propertyName, defaultValue)
            LOGGER.debug("property {} contains the value {}", propertyName, value)
            LOGGER.debug("cached property value")
            cache[propertyName] = value
            value
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun getPropertyByConfig(propertyName: String): Optional<String> {
        val mainClass = System.getProperty("sun.java.command")
        val packageMainClass = mainClass.substring(0, mainClass.lastIndexOf("."))
        val reflections = Reflections(packageMainClass)
        val subTypesOfNativeQueryConfig = reflections.getSubTypesOf(
            DaoQueryConfig::class.java
        )
        for (subType in subTypesOfNativeQueryConfig) {
            return try {
                val config = subType.constructors[0].newInstance() as DaoQueryConfig
                LOGGER.debug("property value obtained by DaoQueryConfig class")
                when (propertyName) {
                    "spring-dao.package-scan" -> Optional.ofNullable(config.packageScan)
                    "spring-dao.xml.directory" -> Optional.ofNullable(config.xmlDirectory)
                    "spring-dao.use-hibernate-types" -> Optional.of(config.useHibernateTypes.toString())
                    else -> Optional.ofNullable(config.fileSufix)
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        return Optional.empty()
    }

    @Throws(IOException::class)
    private fun getProperty(propertyName: String, defaultValue: String): String {
        return getPropertyValue(propertyName)
            .orElseGet {
                getYamlValue(propertyName)
                    .orElseGet{
                        getPropertyByConfig(propertyName)
                            .orElseGet{
                                LOGGER.debug("property value obtained by default value")
                                defaultValue
                            }
                    }
            }
    }

    @Throws(IOException::class)
    private fun getPropertyValue(inputStream: InputStream, propertyName: String): Optional<String> {
        val prop = Properties()
        prop.load(inputStream)
        return Optional.ofNullable(prop.getProperty(propertyName))
    }

    @Throws(IOException::class)
    private fun getPropertyValue(propertyName: String): Optional<String> {
        for (propertyFile in propertyFileList) {
            val inputStreamYml = getInputStream(propertyFile)
            if (inputStreamYml.isPresent) {
                val value = getPropertyValue(inputStreamYml.get(), propertyName)
                if (value.isPresent) {
                    LOGGER.debug("property value obtained by application.properties")
                    return value
                }
            }
        }
        return Optional.empty()
    }

    private fun getYamlValue(inputStreamYml: InputStream, propertyName: String): Optional<String> {
        return try {
            val obj = mapper.readValue(inputStreamYml, HashMap::class.java)
            val map = obj["spring-dao"] as HashMap<String, String>?
            if (map != null) {
                Optional.ofNullable(map[propertyName.replace("spring-dao.", "")])
            } else Optional.empty()
        } catch (e: Exception) {
            Optional.empty()
        }
    }

    private fun getYamlValue(propertyName: String): Optional<String> {
        for (file in yamlFileList) {
            val inputStreamYml = getInputStream(file)
            if (inputStreamYml.isPresent) {
                val value = getYamlValue(inputStreamYml.get(), propertyName)
                if (value.isPresent) {
                    LOGGER.debug("property value obtained by application.yaml")
                    return value
                }
            }
        }
        return Optional.empty()
    }

    private fun getInputStream(s: String): Optional<InputStream> {
        val inputStream = PropertyUtil::class.java
            .classLoader
            .getResourceAsStream(s)
        return Optional.ofNullable(inputStream)
    }
}