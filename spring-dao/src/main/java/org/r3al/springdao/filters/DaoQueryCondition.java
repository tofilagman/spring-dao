package org.r3al.springdao.filters;

import org.r3al.springdao.DaoQueryParameter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DaoQueryCondition {
    private List< DaoQueryConditionItem> conditions = new ArrayList<>();
    private Map<String, Object> keys = new TreeMap<>();
    private List<DaoQueryParameter> parameters = new ArrayList<>();

    public void and(String condition, Object value) {
        process(condition, List.of(value), DaoQueryConditionOperator.AND, DaoQueryConditionType.DEFAULT);
    }

    public void and(String condition, Object value, DaoQueryConditionType conditionType) {
        process(condition, List.of(value), DaoQueryConditionOperator.AND, conditionType);
    }

    public void and(String condition, List<Object> values) {
        process(condition, values, DaoQueryConditionOperator.AND, DaoQueryConditionType.DEFAULT);
    }

    public void or(String condition, Object value) {
        process(condition, List.of(value), DaoQueryConditionOperator.OR, DaoQueryConditionType.DEFAULT);
    }

    public void or(String condition, Object value, DaoQueryConditionType conditionType) {
        process(condition, List.of(value), DaoQueryConditionOperator.OR, conditionType);
    }

    public void or(String condition, List<Object> values) {
        process(condition, values, DaoQueryConditionOperator.OR, DaoQueryConditionType.DEFAULT);
    }

    public void add(String condition, Object value, DaoQueryConditionOperator op) {
        process(condition, List.of(value), op, DaoQueryConditionType.DEFAULT);
    }

    public void add(String condition, Object value, DaoQueryConditionType conditionType, DaoQueryConditionOperator op) {
        process(condition, List.of(value), op, conditionType);
    }

    public void add(String condition, List<Object> values, DaoQueryConditionOperator op) {
        process(condition, values, op, DaoQueryConditionType.DEFAULT);
    }

    public void group(DaoQueryCondition condition, DaoQueryConditionOperator op) {
        conditions.add(DaoQueryConditionItem.INSTANCE(condition, op));
    }

    private String generateKey() {
        return "FLD" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private void process(String value, List<Object> parameters, DaoQueryConditionOperator operator, DaoQueryConditionType conditionType) {
        final String regex = "\\$([^\") ]*)";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(value);

        String mpc = value;
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                final String key = generateKey();
                mpc = mpc.replace("$" + matcher.group(i), ":" + key);
                switch (conditionType) {
                    case LIKE -> keys.put(key, "%" + parameters.get(i - 1) + "%");
                    case BEGIN_LIKE -> keys.put(key, parameters.get(i - 1) + "%");
                    case END_LIKE -> keys.put(key, "%" + parameters.get(i - 1));
                    case DEFAULT -> keys.put(key, parameters.get(i - 1));
                }
            }
        }
        conditions.add(DaoQueryConditionItem.INSTANCE(mpc, operator));
    }

    /**
     * the initial operation will decide the group condition
     *
     * @return
     */
    public String getSql() {
        if (conditions.isEmpty())
            return "1=1";

        DaoQueryConditionItem op = conditions.get(0);

        String npc = "";
        for (int i = 1; i < conditions.size(); i++) {
            if(conditions.get(i).condition != null) {
                npc = String.format(" %s %s (%s) ", npc, conditions.get(i).op.name(), conditions.get(i).condition.getSql());
                parameters.addAll(conditions.get(i).condition.getParameters());
            } else {
                npc = String.format(" %s %s %s ", npc, conditions.get(i).op.name(), conditions.get(i).sql);
            }
        }
        return String.format(" (%s %s) ", op.sql, npc);
    }

    public List<DaoQueryParameter> getParameters(){
        parameters.addAll(keys.entrySet().stream().map(entry -> new DaoQueryParameter(entry.getKey(), entry.getValue())).toList());
        return parameters;
    }

   public static DaoQueryCondition INSTANCE() {
       return new DaoQueryCondition();
   }
}

