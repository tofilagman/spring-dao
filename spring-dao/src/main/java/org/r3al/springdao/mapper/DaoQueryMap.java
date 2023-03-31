package org.r3al.springdao.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class DaoQueryMap<T> {

    private final Map<String, DaoQueryMapItem> items = new HashMap();

    public DaoQueryMap(Predicate<T> name, DaoQueryMapOperator operator, List<Object> values) {
        String mth = name.toString();
        items.put(mth, new DaoQueryMapItem(mth, operator, values));
    }

    public DaoQueryMap and(Predicate<T> name, DaoQueryMapOperator operator, List<Object> values) {
        String mth = name.toString();
        items.put(mth, new DaoQueryMapItem(mth, operator, values));
        return this;
    }

    public DaoQueryMap or(Predicate<T> name, DaoQueryMapOperator operator, List<Object> values) {
        String mth = name.toString();
        items.put(mth, new DaoQueryMapItem(mth, operator, values));
        return this;
    }

    public static <T> DaoQueryMap of(Predicate<T> name, DaoQueryMapOperator operator, List<Object> values) {
        return new DaoQueryMap(name, operator, values);
    }
}


