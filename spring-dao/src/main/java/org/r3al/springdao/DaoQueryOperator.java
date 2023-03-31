package org.r3al.springdao;



import java.util.function.Function;

public enum DaoQueryOperator {

    DEFAULT(value -> value);
//    CONTAINING(new TransformParamContaining()),
//    STARTS_WITH(new TransformParamStartsWith()),
//    ENDS_WITH(new TransformParamEndsWith());

    private final Function<Object, Object> transformParam;

    DaoQueryOperator(Function<Object, Object> transformParam) {
        this.transformParam = transformParam;
    }

    public Function<Object, Object> getTransformParam() {
        return transformParam;
    }

}
