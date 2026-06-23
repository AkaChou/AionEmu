/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 * Aion-Lightning is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Aion-Lightning is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aion-Lightning. If not, see <http://www.gnu.org/licenses/>.
 */
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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * XML工具类，提供XML文档处理的通用方法
 * XML Utilities class providing common methods for XML document processing
 */
public abstract class XmlUtils {
    private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private static final TransformerFactory tf = TransformerFactory.newInstance();

   /**
    * 将XML字符串转换为Document对象
    * Converts XML string to Document object
    * 
    * @param xmlSource XML字符串内容 (XML string content)
    * @return 解析后的Document对象，如果输入为null则返回null (Parsed Document object, returns null if input is null)
    */
    /**
     * 将XML字符串转换为Document对象
     * Converts XML string to Document object
     * 
     * @param xmlSource XML字符串内容 (XML string content)
     * @return 解析后的Document对象，如果输入为null则返回null (Parsed Document object, returns null if input is null)
     * @throws RuntimeException 解析失败时抛出异常 (Thrown when parsing fails)
     */
    public static Document getDocument(String xmlSource) {
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
     * 将Document对象转换为XML字符串
     * Converts Document object to XML string
     * 
     * @param document 需要转换的Document对象 (Document object to convert)
     * @return 转换后的XML字符串 (Converted XML string)
     * @throws RuntimeException 转换失败时抛出异常 (Thrown when transformation fails)
     */
    public static String getString(Document document) {
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
     * 从XML Schema字符串创建Schema对象
     * Creates Schema object from XML Schema string
     * 
     * @param schemaString XML Schema字符串 (XML Schema string)
     * @return 创建的Schema对象 (Created Schema object)
     * @throws RuntimeException 创建失败时抛出异常 (Thrown when schema creation fails)
     */
    public static Schema getSchema(String schemaString) {
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
     * 从URL创建Schema对象
     * Creates Schema object from URL
     * 
     * @param schemaURL Schema文件URL (Schema file URL)
     * @return 创建的Schema对象 (Created Schema object)
     * @throws RuntimeException 创建失败时抛出异常 (Thrown when schema creation fails)
     */
    public static Schema getSchema(URL schemaURL) {
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

   public static void validate(Schema schema, Document document) {
      Validator validator = schema.newValidator();

      try {
         validator.validate(new DOMSource(document));
      } catch (Exception var4) {
         throw new RuntimeException("Failed to validate document", var4);
      }
   }

   static {
      dbf.setNamespaceAware(true);
   }
}
