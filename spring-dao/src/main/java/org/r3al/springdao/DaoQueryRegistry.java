package org.r3al.springdao;

import java.util.Set;

public interface DaoQueryRegistry {

    void registry(Set<Class<? extends DaoQuery>> DaoQueryList);

}
