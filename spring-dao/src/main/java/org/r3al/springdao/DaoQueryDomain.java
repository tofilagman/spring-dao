package org.r3al.springdao;

import java.io.Serializable;

public abstract class DaoQueryDomain implements Cloneable {

    @Override
    public DaoQueryDomain clone() {
        try {
            return (DaoQueryDomain) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
