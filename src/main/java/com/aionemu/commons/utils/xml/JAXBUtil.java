/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 * Aion-Lightning is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Aion-Lightning is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. *
 *
 * You should have received a copy of the GNU General Public License along with Aion-Lightning. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Credits goes to all Open Source Core Developer Groups listed below Please do not change here something, ragarding the developer credits, except the
 * "developed by XXXX". Even if you edit a lot of files in this source, you still have no rights to call it as "your Core". Everybody knows that this
 * Emulator Core was developed by Aion Lightning
 * 
 * @-Aion-Unique-
 * @-Aion-Lightning
 * @Aion-Engine
 * @Aion-Extreme
 * @Aion-NextGen
 * @Aion-Core Dev.
 */
package com.aionemu.commons.utils.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;

import org.w3c.dom.Document;

/**
 * JAXB 工具类 (JAXB Utility Class)
 * 提供XML序列化/反序列化功能，支持通过Schema验证 (Provides XML serialization/deserialization with schema validation support)
 */
public class JAXBUtil {

    /**
     * 将对象序列化为XML字符串 (Serialize object to XML string)
     * @param obj 要序列化的对象 (Object to be serialized)
     * @return 格式化的XML字符串 (Formatted XML string)
     * @throws RuntimeException 当序列化失败时抛出 (Thrown when serialization fails)
     */
    public static String serialize(Object obj) {
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
     * 将对象序列化为XML文档对象 (Serialize object to XML Document)
     * @param obj 要序列化的对象 (Object to be serialized)
     * @return 包含XML数据的Document对象 (Document object containing XML data)
     */
    public static Document serializeToDocument(Object obj) {
        String s = serialize(obj);
        return XmlUtils.getDocument(s);
    }

    /**
     * 反序列化XML字符串 (Deserialize XML string)
     * @param s XML字符串 (XML string)
     * @param clazz 目标类类型 (Target class type)
     * @return 反序列化的对象 (Deserialized object)
     */
    public static <T> T deserialize(String s, Class<T> clazz) {
        return deserialize(s, clazz, (Schema) null);
    }

    /**
     * 通过URL Schema验证反序列化 (Deserialize with schema validation from URL)
     * @param s XML字符串 (XML string)
     * @param clazz 目标类类型 (Target class type)
     * @param schemaURL Schema文件URL (Schema file URL)
     * @return 验证后的对象 (Validated object)
     */
    public static <T> T deserialize(String s, Class<T> clazz, URL schemaURL) {
        Schema schema = XmlUtils.getSchema(schemaURL);
        return deserialize(s, clazz, schema);
    }

    /**
     * 通过字符串Schema验证反序列化 (Deserialize with schema validation from string)
     * @param s XML字符串 (XML string)
     * @param clazz 目标类类型 (Target class type)
     * @param schemaString Schema定义字符串 (Schema definition string)
     * @return 验证后的对象 (Validated object)
     */
    public static <T> T deserialize(String s, Class<T> clazz, String schemaString) {
        Schema schema = XmlUtils.getSchema(schemaString);
        return deserialize(s, clazz, schema);
    }

    /**
     * 从XML文档反序列化 (Deserialize from XML Document)
     * @param xml XML文档对象 (XML Document object)
     * @param clazz 目标类类型 (Target class type)
     * @param schemaString Schema定义字符串 (Schema definition string)
     * @return 验证后的对象 (Validated object)
     */
    public static <T> T deserialize(Document xml, Class<T> clazz, String schemaString) {
        String xmlAsString = XmlUtils.getString(xml);
        return deserialize(xmlAsString, clazz, schemaString);
    }

    /**
     * 核心反序列化方法 (Core deserialization method)
     * @param s XML字符串 (XML string)
     * @param clazz 目标类类型 (Target class type)
     * @param schema 验证模式 (Validation schema)
     * @return 反序列化的对象 (Deserialized object)
     * @throws RuntimeException 当反序列化失败时抛出 (Thrown when deserialization fails)
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(String s, Class<T> clazz, Schema schema) {
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
     * 生成XML Schema (Generate XML Schema)
     * @param classes 要生成Schema的类 (Classes to generate schema for)
     * @return Schema定义字符串 (Schema definition string)
     * @throws RuntimeException 当生成失败时抛出 (Thrown when generation fails)
     */
    public static String generateSchema(Class<?>... classes) {
        try {
            JAXBContext jc = JAXBContext.newInstance(classes);
            StringSchemaOutputResolver ssor = new StringSchemaOutputResolver();
            jc.generateSchema(ssor);
            return ssor.getSchema();
        } catch (Exception e) {
            // 修正拼写错误：schemma -> schema
            throw new RuntimeException("Failed to generate schema", e);
        }
    }
}
