package org.r3al.springdao;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DaoQueryResourceLoader {

    private String packageName;

    DaoQueryResourceLoader(String packageName) {
        this.packageName = packageName;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryResourceLoader.class);
    private final ClassLoader resourceClassLoader = getClass().getClassLoader();

//    private void setFile(Class<? extends DaoQuery> classe, MethodInvocation invocation, DaoQueryInfo info) throws IOException {
//
//        final Method method = invocation.getMethod();
//
//        info.sqlPatterns = loadResource(classe.getName());
//
//        LOGGER.debug("sql obtained through the {} file", classe.getName());
//    }

    private String canonicalResourceName(String resourceName) {
        // Normalize the resource name. Remove leading slash.
        return resourceName.charAt(0) == '/' ? resourceName.substring(1) : resourceName;
    }


    public List<DaoQuerySqlPattern> loadResource(Class<? extends DaoQuery> classe) throws IOException {
        String rName = canonicalResourceName(classe.getName());
        String rootDir = PropertyUtil.getValue("Dao-query.sql.directory", DaoQueryAutoConfiguration.SQL_DIRECTORY);
        String pName = rootDir + "/" + StringUtils.stripStart(rName.replaceAll(packageName, ""), ".")
                .replace('.', '/')  + ".xml";

        InputStream stream = resourceClassLoader.getResourceAsStream(pName);
        if (stream == null) {
            throw new RuntimeException("Resource (" + rName + ") cannot be opened");
        }

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            //XMLHelper.setupSafeSAXParserFactory(factory, true);
            SAXParser parser = factory.newSAXParser();
            Handler handler = new Handler();
            parser.parse(stream, handler);
            return handler.patternList;
        } catch(FactoryConfigurationError e) {
            throw new RuntimeException("Unable to get a SAX parser factory.", e);
        } catch(ParserConfigurationException e) {
            throw new RuntimeException("Unable to get a SAX parser.", e);
        } catch(SAXException e) {
            throw new RuntimeException("Unable to parse stream.", e);
        } catch(IOException e) {
            throw new RuntimeException("Unable to parse stream.", e);
        } finally {
            try {
                stream.close();
            } catch(Throwable e) {  //NOPMD
            }
        }
    }

    protected static class Handler extends DefaultHandler {

        List<DaoQuerySqlPattern> patternList;
        boolean attrTemplateOneline;
        DaoQuerySqlPattern pattern;
        StringBuffer templateBuffer;

        public void startElement(String namespaceURI, String localName, String qName, Attributes attrs) {
            if(qName.equals("database")) {
                patternList = new ArrayList<>();
            } else if(qName.equals("sql")) {
                pattern = new DaoQuerySqlPattern();
                pattern.setKey(attrs.getValue("id"));
                pattern.templateLanguage = attrs.getValue("lang");
                attrTemplateOneline = "true".equals(attrs.getValue("oneline"));
                templateBuffer = new StringBuffer();
            } else {
                flushTemplateBuffer();
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) {
            if(qName.equals("sql")) {
                flushTemplateBuffer();
                patternList.add(pattern);
                pattern = null;
                attrTemplateOneline = false;
            }

            // do nothing for 'externalsql'
        }

        public void characters(char[] ch, int start, int length) {
            if(templateBuffer != null) {
                templateBuffer.append(ch, start, length);
            }
        }

        protected void flushTemplateBuffer() {
            if(templateBuffer != null) {
                pattern.template = attrTemplateOneline ? toOneLine(templateBuffer)
                        : templateBuffer.toString();
                templateBuffer = null;
            }
        }

        protected String toOneLine(StringBuffer sbuf) {
            int i = 0, j = 0, len = sbuf.length();
            char ch;
            char charray[] = new char[len];
            while(i < len) {
                // Move past any white space.
                for(; i < len && Character.isWhitespace(sbuf.charAt(i)); i++) {}

                // We're done.
                if(i >= len) {
                    break;
                }
                // Put a single space if it's not the beginning.
                if(j > 0) {
                    charray[j++] = ' ';
                }
                // Copy up to any newline character.
                for(; i < len && (ch = sbuf.charAt(i)) != '\n'; i++, j++) {
                    charray[j] = ch;
                }
                // Move back over any trailing whitespace.
                for(; (ch = charray[j]) == '\0' || Character.isWhitespace(ch); j--) {}
                j++;
            }
            return new String(charray, 0, j);
        }
    }
}
