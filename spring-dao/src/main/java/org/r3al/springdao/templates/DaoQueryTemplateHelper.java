package org.r3al.springdao.templates;

public class DaoQueryTemplateHelper {

    private Object object;

    DaoQueryTemplateHelper(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public static DaoQueryTemplateHelper register(Object object) {
        return new DaoQueryTemplateHelper(object);
    }

}
