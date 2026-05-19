package org.r3al.springdao;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(DaoQueryAutoRegistrar.class)
public class DaoQueryAutoConfiguration {

    public static final String SQL_DIRECTORY = "daoQuery";

}
