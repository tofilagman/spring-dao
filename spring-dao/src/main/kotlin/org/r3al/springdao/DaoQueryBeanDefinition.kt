package org.r3al.springdao

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import java.util.function.Supplier

internal object DaoQueryBeanDefinition {
    fun of(classe: Class<out DaoQuery>, source: Any?): AbstractBeanDefinition {
        val builder = BeanDefinitionBuilder.rootBeanDefinition(classe.name)
        builder.rawBeanDefinition.source = source
        builder.setLazyInit(false)
        builder.setScope(BeanDefinition.SCOPE_SINGLETON)
        val beanDefinition = builder.beanDefinition
        beanDefinition.instanceSupplier = Supplier { source }
        beanDefinition.setAttribute("factoryBeanObjectType", classe.name)
        return beanDefinition
    }
}