package org.r3al.springdao.filters;

import jakarta.persistence.Tuple;
import org.r3al.springdao.DaoQueryParameter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DaoQueryCondition {
    private List< DaoQueryConditionItem> conditions = new ArrayList<>();
    private Map<String, Object> keys = new TreeMap<>();

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

    private String generateKey() {
        return "FLD" + UUID.randomUUID();
    }

    private void process(String value, List<Object> parameters, DaoQueryConditionOperator operator, DaoQueryConditionType conditionType) {
        final String regex = "\\$([^\")]*)";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(value);

        String mpc = value;
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                final String key = generateKey();
                mpc = mpc.replace(matcher.group(i), ":" + key);
                if (conditionType == DaoQueryConditionType.LIKE) {
                    keys.put(key, "%" + parameters.get(i) + "%");
                } else {
                    keys.put(key, parameters.get(i));
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
            return "";

        DaoQueryConditionItem op = conditions.get(0);

        String npc = "";
        for (int i = 1; i < conditions.size(); i++) {
            npc = String.format(" %s %s %s ",npc ,  conditions.get(i).op.name(), conditions.get(i).sql);
        }
        return String.format(" %s (%s %s) ", op.op.name(), op.sql, npc);
    }

    public List<DaoQueryParameter> getParameters(){
        return keys.entrySet().stream().map(entry -> new DaoQueryParameter(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

   public static DaoQueryCondition INSTANCE() {
       return new DaoQueryCondition();
   }
}

