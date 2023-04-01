package org.r3al.springdao;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

public class DaoQueryBeanDefinition {

    private DaoQueryBeanDefinition() {}

    public static AbstractBeanDefinition of(Class<? extends DaoQuery> classe, Object source) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(classe.getName());
        builder.getRawBeanDefinition().setSource(source);
        builder.setLazyInit(false);
        builder.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        beanDefinition.setInstanceSupplier(() -> source);
        beanDefinition.setAttribute("factoryBeanObjectType", classe.getName());
        return beanDefinition;
    }

}
