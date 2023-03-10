package org.r3al.springdao

import org.r3al.springdao.impl.DaoQueryRegistryImpl
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DaoQueryAutoConfiguration {

    @Bean
    fun beanFactoryPostProcessor(): BeanFactoryPostProcessor {
        return BeanFactoryPostProcessor { bf: ConfigurableListableBeanFactory ->
            LOGGER.debug("starting configuration")
            val beanDefinitionRegistry = bf as BeanDefinitionRegistry
            val packageScan = PropertyUtil.getValue("spring-dao.package-scan", "org.r3al.springdao")
            LOGGER.debug("packageScan {}", packageScan)
            val reflections = Reflections(packageScan)
            LOGGER.debug("looking for interfaces that implement NativeQuery")
            val nativeQueryList = reflections.getSubTypesOf(DaoQuery::class.java)
            LOGGER.debug("{} found interfaces", nativeQueryList.size)
            val nativeQueryRegistry = DaoQueryRegistryImpl(beanDefinitionRegistry)
            nativeQueryRegistry.registry(nativeQueryList)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DaoQueryAutoConfiguration::class.java)
        const val XML_DIRECTORY = "daoQuery"
    }
}