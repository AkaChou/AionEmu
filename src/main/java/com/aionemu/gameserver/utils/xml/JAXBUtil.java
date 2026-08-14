package com.aionemu.gameserver.utils.xml;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import java.net.URL;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

/**
 * 静态数据 JAXB 绑定/校验工具。
 * JAXB bind/validate helpers for static-data XML.
 *
 * @author ginho1
 * @author Dezalmado
 */
@Slf4j
public class JAXBUtil {

	/**
	 * 从输入流反序列化对象（带 schema 校验）。
	 * Unmarshal an object from an input stream (with schema validation).
	 *
	 * @param is 输入流 / Input stream
	 * @param clazz 目标类型 / Target class
	 *
	 * @param <T> 结果类型 / Result type
	 * @return 对象或 null / Object or null
	 */
	public static <T> T unmarshal(InputStream is, Class<T> clazz) {
		try {
			JAXBContext jc = JAXBContext.newInstance(clazz);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema(getSchema(clazz));
			unmarshaller.setEventHandler(new XmlValidationHandler());
			return (T) unmarshaller.unmarshal(is);
		}
		catch (JAXBException e) {
			log.error(I18n.get("log.b82c1d87c438", e));
		}
		return null;
	}

	/**
	 * 从文件反序列化对象。
	 * Unmarshal an object from a file.
	 *
	 * @param file 文件 / File
	 * @param clazz 目标类型 / Target class
	 *
	 * @param <T> 结果类型 / Result type
	 * @return 对象 / Object
	 * @throws JAXBException 读取/绑定失败时 / On read/bind failure
	 */
	public static <T> T unmarshal(File file, Class<T> clazz) throws JAXBException {
		try (InputStream is = new java.io.FileInputStream(file)) {
			return unmarshal(is, clazz);
		} catch (java.io.IOException e) {
			throw new JAXBException("Error reading file: " + file.getAbsolutePath(), e);
		}
	}

	/**
	 * 从 XML 字符串反序列化对象（带 schema 校验）。
	 * Unmarshal an object from an XML string (with schema validation).
	 *
	 * @param stream XML 字符串 / XML string
	 * @param clazz 目标类型 / Target class
	 *
	 * @param <T> 结果类型 / Result type
	 * @return 对象或 null / Object or null
	 */
	public static <T> T unmarshal(String stream, Class<T> clazz) {
		try {
			JAXBContext jc = JAXBContext.newInstance(clazz);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema(getSchema(clazz));
			unmarshaller.setEventHandler(new XmlValidationHandler());
			return (T) unmarshaller.unmarshal(new StringReader(stream));
		}
		catch (JAXBException e) {
			log.error(I18n.get("log.b82c1d87c438", e));
		}
		return null;
	}

	/**
	 * 将对象序列化到文件。
	 * Marshal an object to a file.
	 *
	 * @param file 输出路径 / Output path
	 * @param clazz 类型 / Class
	 * @param object 对象 / Object
	 *
	 * @param <T> 对象类型 / Object type
	 */
	public static <T> void marshal(String file, Class<T> clazz, T object) {
		try {
			JAXBContext jc = JAXBContext.newInstance(clazz);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setEventHandler(new XmlValidationHandler());
			marshaller.marshal(object, new File(file));
		}
		catch (JAXBException e) {
			log.error(I18n.get("log.adbf3f704f02", e));
		}
	}

	/**
	 * 将对象序列化为 XML 字符串。
	 * Marshal an object to an XML string.
	 *
	 * @param clazz 类型 / Class
	 * @param object 对象 / Object
	 *
	 * @param <T> 对象类型 / Object type
	 * @return XML 字符串或 null / XML string or null
	 */
	public static <T> String marshal(Class<T> clazz, T object) {
		try {
			StringWriter sw = new StringWriter();
			JAXBContext jc = JAXBContext.newInstance(clazz);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setEventHandler(new XmlValidationHandler());
			marshaller.marshal(object, sw);
			return sw.toString();
		}
		catch (JAXBException e) {
			log.error(I18n.get("log.adbf3f704f02", e));
		}
		return null;
	}

	/**
	 * 校验 XML 字符串是否符合类型 schema。
	 * Validate an XML string against the type's schema.
	 *
	 * @param xml XML 字符串 / XML string
	 * @param clazz 类型 / Class
	 *
	 * @param <T> 类型参数 / Type param
	 * @return 若有效则为 true / True if valid
	 */
	public static <T> boolean validate(String xml, Class<T> clazz) {
		try {
			JAXBContext jc = JAXBContext.newInstance(clazz);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema(getSchema(clazz));
			unmarshaller.setEventHandler(new XmlValidationHandler());
			unmarshaller.unmarshal(new StringReader(xml));
			return true;
		}
		catch (JAXBException e) {
			log.warn(I18n.get("log.33fe4a6282a7", e));
			return false;
		}
	}

	/**
	 * 校验输入流 XML 是否符合类型 schema。
	 * Validate input-stream XML against the type's schema.
	 *
	 * @param is 输入流 / Input stream
	 * @param clazz 类型 / Class
	 *
	 * @param <T> 类型参数 / Type param
	 * @return 若有效则为 true / True if valid
	 */
	public static <T> boolean validate(InputStream is, Class<T> clazz) {
		try {
			JAXBContext jc = JAXBContext.newInstance(clazz);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema(getSchema(clazz));
			unmarshaller.setEventHandler(new XmlValidationHandler());
			unmarshaller.unmarshal(is);
			return true;
		}
		catch (JAXBException e) {
			log.warn(I18n.get("log.33fe4a6282a7", e));
			return false;
		}
	}

	/**
	 * 由 JAXB 模型运行时生成 schema。
	 * Generate a schema at runtime from the JAXB model.
	 *
	 * @param clazz 类型 / Class
	 * @return Schema 或 null / Schema or null
	 */
	private static Schema getSchema(Class<?> clazz) {
		try {
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			StringSchemaOutputResolver ssor = new StringSchemaOutputResolver();
			JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
			jaxbContext.generateSchema(ssor);
			String schemaString = ssor.getSchemaString();
			if (schemaString != null && !schemaString.isEmpty()) {
				InputStream schemaStream = new ByteArrayInputStream(schemaString.getBytes(StandardCharsets.UTF_8));
				return sf.newSchema(new StreamSource(schemaStream));
			}
		} catch (JAXBException | SAXException | IOException e) {
			log.error(I18n.get("log.d834008aca15", clazz.getName(), e));
		}
		return null;
	}

	/**
	 * 用外部 XSD URL 校验 XML 字符串。
	 * Validate an XML string against an external XSD URL.
	 *
	 * @param xmlString XML 字符串 / XML string
	 * @param schemaUrl Schema URL
	 *
	 * @return 若有效则为 true / True if valid
	 */
	public static boolean validateSchema(String xmlString, URL schemaUrl) {
		try {
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(schemaUrl);
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8))));
			return true;
		} catch (SAXException | java.io.IOException e) {
			log.warn(I18n.get("log.5c43c98b7147", e.getMessage()));
			return false;
		}
	}
}
