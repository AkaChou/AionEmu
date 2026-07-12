package com.aionemu.commons.utils.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import lombok.experimental.UtilityClass;
import org.w3c.dom.Document;

/**
 * JAXB 序列化 / 反序列化与 Schema 生成工具。
 * deserialize and schema generation helpers.
 */
@UtilityClass
public class JAXBUtil {

    /**
     * 将对象序列化为格式化 XML 字符串。
     * Serialize an object to a formatted XML string.
     *
     * @param obj 待序列化对象 / Object to serialize
     * XML string
     *
     * @param obj
     * @throws RuntimeException 序列化失败 / On marshal failure
     */
    public String serialize(Object obj) {
        try {
            JAXBContext jc = JAXBContext.newInstance(obj.getClass());
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();
            m.marshal(obj, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to marshall object of class " + obj.getClass().getName(), e);
        }
    }

    /**
     * 将对象序列化为 DOM Document。
     * Serialize an object to a DOM Document.
     *
     * @param obj 待序列化对象 / Object to serialize
     * Document
     */
    public Document serializeToDocument(Object obj) {
        String s = serialize(obj);
        return XmlUtils.getDocument(s);
    }

    /**
     * 反序列化 XML 字符串（无 Schema）。
     * Deserialize an XML string without schema.
     *
     * XML string
     * @param clazz 目标类型 / Target type
     * @param <T>   类型参数 / Type parameter
     * Object
     */
    public <T> T deserialize(String s, Class<T> clazz) {
        return deserialize(s, clazz, (Schema) null);
    }

    /**
     * 使用 URL Schema 校验并反序列化。
     * Deserialize with schema validation from a URL.
     *
     * XML string
     * @param clazz     目标类型 / Target type
     * Schema URL
     * @param <T>       类型参数 / Type parameter
     * Object
     */
    public <T> T deserialize(String s, Class<T> clazz, URL schemaURL) {
        Schema schema = XmlUtils.getSchema(schemaURL);
        return deserialize(s, clazz, schema);
    }

    /**
     * 使用字符串 Schema 校验并反序列化。
     * Deserialize with schema validation from a schema string.
     *
     * XML string
     * @param clazz        目标类型 / Target type
     * Schema definition
     * @param <T>          类型参数 / Type parameter
     * Object
     */
    public <T> T deserialize(String s, Class<T> clazz, String schemaString) {
        Schema schema = XmlUtils.getSchema(schemaString);
        return deserialize(s, clazz, schema);
    }

    /**
     * 从 Document 反序列化（字符串 Schema）。
     * Deserialize from a Document using a schema string.
     *
     * XML document
     * @param clazz        目标类型 / Target type
     * Schema definition
     * @param <T>          类型参数 / Type parameter
     * Object
     */
    public <T> T deserialize(Document xml, Class<T> clazz, String schemaString) {
        String xmlAsString = XmlUtils.getString(xml);
        return deserialize(xmlAsString, clazz, schemaString);
    }

    /**
     * 核心反序列化方法。
     * Core deserialization method.
     *
     * XML string
     * @param clazz  目标类型 / Target type
     * Validation schema, may be null
     * @param <T>    类型参数 / Type parameter
     * Object
     * On unmarshal failure。
     */
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String s, Class<T> clazz, Schema schema) {
        try {
            JAXBContext jc = JAXBContext.newInstance(clazz);
            Unmarshaller u = jc.createUnmarshaller();
            u.setSchema(schema);
            return (T) u.unmarshal(new StringReader(s));
        } catch (Exception e) {
            throw new RuntimeException("Failed to unmarshall class " + clazz.getName() + " from xml:\n " + s, e);
        }
    }

    /**
     * 为给定类生成 XML Schema 字符串。
     * Generate an XML Schema string for the given classes.
     *
     * Target classes
     * Schema string
     * On generation failure
     */
    public String generateSchema(Class<?>... classes) {
        try {
            JAXBContext jc = JAXBContext.newInstance(classes);
            StringSchemaOutputResolver ssor = new StringSchemaOutputResolver();
            jc.generateSchema(ssor);
            return ssor.getSchema();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate schema", e);
        }
    }
}
