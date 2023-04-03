package org.r3al.springdao.templates;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.HelperFunction;
import org.apache.commons.text.StringEscapeUtils;
import org.r3al.springdao.ApplicationContextProvider;
import org.r3al.springdao.DaoQueryTemplateDataType;
import org.r3al.springdao.impl.DaoQueryMethodInterceptorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.IOException;
import java.util.Map;

public class HandleBarTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandleBarTemplate.class);

    public String process(String sql, Map<String, Object> data) throws IOException {
        try {
            DaoQueryTemplateHelper helper = getUserHelper();
            Handlebars hdl = new Handlebars();
            hdl.registerHelpers(new HandleBarHelpers());
            if (helper != null)
                hdl.registerHelpers(helper.getObject());
            Template sqltmpl = hdl.compileInline(sql);
            String mpc = sqltmpl.apply(data);
            return StringEscapeUtils.unescapeHtml4(mpc);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private DaoQueryTemplateHelper getUserHelper() {
        try {
            return ApplicationContextProvider.getApplicationContext().getBean(DaoQueryTemplateHelper.class);
        } catch (NoSuchBeanDefinitionException e) {
            LOGGER.debug(e.toString());
            return null;
        }
    }

    public class HandleBarHelpers {

        public CharSequence nullOrZero(Object data, Options options) throws IOException {
            if (data == null) return options.fn();
            return Long.parseLong(data.toString()) <= 0 ? options.fn() : options.inverse();
        }

        public CharSequence query(DaoQueryTemplateDataType type, Options options) throws IOException {
            if (type == DaoQueryTemplateDataType.QUERY) {
                return options.fn();
            }
            return "";
        }

        public CharSequence count(DaoQueryTemplateDataType type, Options options) throws IOException {
            if (type == DaoQueryTemplateDataType.COUNT) {
                return options.fn();
            }
            return "";
        }

        @HelperFunction("return")
        public CharSequence $return(DaoQueryTemplateDataType type, Options options) throws IOException {
            if (type == DaoQueryTemplateDataType.RETURN) {
                return options.fn();
            }
            return "";
        }

        public CharSequence dbType(String type, String value, Options options) throws IOException {
            if (type.equalsIgnoreCase(value)) {
                return options.fn();
            }
            return "";
        }

        public CharSequence queryNullable(Object data, Options options) {
            return data == null ? "" : data.toString();
        }
    }
}
