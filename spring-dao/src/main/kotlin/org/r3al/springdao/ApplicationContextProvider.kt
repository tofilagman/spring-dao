package org.r3al.springdao

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationContextProvider : ApplicationContextAware {
    override fun setApplicationContext(ctx: ApplicationContext) {
        applicationContext = ctx
    }

    companion object {
        lateinit var applicationContext: ApplicationContext
    }
}