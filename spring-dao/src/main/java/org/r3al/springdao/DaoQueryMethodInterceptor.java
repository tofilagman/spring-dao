package org.r3al.springdao;

import java.io.IOException;

public interface DaoQueryMethodInterceptor {

    Object executeQuery(DaoQueryInfo info) throws IOException;

}
