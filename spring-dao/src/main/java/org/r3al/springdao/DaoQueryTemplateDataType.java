package org.r3al.springdao;

public enum DaoQueryTemplateDataType {
    QUERY(1),
    COUNT(2),
    RETURN(3);

    private Integer value;

    DaoQueryTemplateDataType(Integer value) {
        this.value = value;
    }
}
