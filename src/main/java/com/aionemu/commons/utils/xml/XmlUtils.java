package com.aionemu.commons.utils.xml;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import lombok.experimental.UtilityClass;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * XML 文档解析、转换与 Schema 校验工具。
 * XML document parse, transform and schema validation helpers.
 */
@UtilityClass
public class XmlUtils {

    private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private static final TransformerFactory tf = TransformerFactory.newInstance();

    static {
        dbf.setNamespaceAware(true);
    }

    /**
     * 将 XML 字符串解析为 Document。
     * Parse an XML string into a Document.
     *
     * XML string
     * @return Document，输入为 null 时返回 null / Document, or null when input is null
     * On parse failure
     */
    public Document getDocument(String xmlSource) {
        synchronized (XmlUtils.class) {
            Document document = null;
            if (xmlSource != null) {
                try (Reader stream = new StringReader(xmlSource)) {
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    document = db.parse(new InputSource(stream));
                } catch (Exception e) {
                    throw new RuntimeException("Error converting string to document", e);
                }
            }
            return document;
        }
    }

    /**
     * 将 Document 转为 XML 字符串。
     * Convert a Document to an XML string.
     *
     * document
     * XML string
     * On transform failure
     */
    public String getString(Document document) {
        synchronized (XmlUtils.class) {
            try {
                DOMSource domSource = new DOMSource(document);
                StringWriter writer = new StringWriter();
                Transformer transformer = tf.newTransformer();
                transformer.transform(domSource, new StreamResult(writer));
                return writer.toString();
            } catch (TransformerException e) {
                throw new RuntimeException("Error converting document to string", e);
            }
        }
    }

    /**
     * 从 Schema 字符串创建 Schema。
     * Create a Schema from a schema string.
     *
     * Schema definition
     * @return Schema，输入为 null 时返回 null / Schema, or null when input is null
     * On creation failure
     */
    public Schema getSchema(String schemaString) {
        try {
            if (schemaString == null) {
                return null;
            }
            SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            return sf.newSchema(new StreamSource(new StringReader(schemaString)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create schema from string: " + schemaString, e);
        }
    }

    /**
     * 从 URL 创建 Schema。
     * Create a Schema from a URL.
     *
     * Schema file URL
     * @return Schema，输入为 null 时返回 null / Schema, or null when input is null
     * On creation failure
     */
    public Schema getSchema(URL schemaURL) {
        try {
            if (schemaURL == null) {
                return null;
            }
            SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            return sf.newSchema(schemaURL);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create schema from URL " + schemaURL, e);
        }
    }

    /**
     * 使用 Schema 校验 Document。
     * Validate a Document against a Schema.
     *
     * schema
     * document
     * On validation failure
     */
    public void validate(Schema schema, Document document) {
        Validator validator = schema.newValidator();

        try {
            validator.validate(new DOMSource(document));
        } catch (Exception var4) {
            throw new RuntimeException("Failed to validate document", var4);
        }
    }
}
