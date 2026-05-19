package org.r3al.springdao;

import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    /**
     * Resolves a bean by type, optionally restricted to a Spring {@code @Qualifier}.
     * When {@code qualifier} is null or blank, falls back to unqualified type lookup
     * (which honours {@code @Primary}).
     */
    public static <T> T getBean(Class<T> type, String qualifier) {
        if (qualifier == null || qualifier.isBlank()) {
            return context.getBean(type);
        }
        return BeanFactoryAnnotationUtils.qualifiedBeanOfType(
                context.getAutowireCapableBeanFactory(), type, qualifier);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        context = ctx;
    }

}