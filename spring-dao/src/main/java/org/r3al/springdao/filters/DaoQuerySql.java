package org.r3al.springdao.filters;

import org.r3al.springdao.DaoQueryParameter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DaoQuerySql {

    private String sql;
    private Map<String, Object> keys = new TreeMap<>();

    public DaoQuerySql(String sql, Object... arguments) {
        process(sql, List.of(arguments));
    }

    public String getSql() {
        return sql == null ? "" : sql;
    }

    public List<DaoQueryParameter> getParameters() {
        return keys.entrySet().stream().map(entry -> new DaoQueryParameter(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    private String generateKey() {
        return "FQL" + UUID.randomUUID().toString().replace("-", "");
    }

    private void process(String value, List<Object> parameters) {
        final String regex = "\\$([^\")]*)";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(value);

        String mpc = value;
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                final String key = generateKey();
                mpc = mpc.replace("$" + matcher.group(i), ":" + key);
                keys.put(key, parameters.get(i - 1));
            }
        }
        this.sql = mpc;
    }

    public static DaoQuerySql of(String sql, Object... arguments) {
        return new DaoQuerySql(sql, arguments);
    }

}
