package com.aionemu.gameserver.utils.xml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
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
 * XML 解析、转换、schema 校验与 .xml 文件枚举工具。
 * XML parse/transform, schema validation and .xml file listing helpers.
 *
 * @author Neon
 */
public abstract class XmlUtil {

	/**
	 * 缓存的 DocumentBuilderFactory。
	 * Cached DocumentBuilderFactory.
	 */
	private static volatile DocumentBuilderFactory dbf;
	/**
	 * 缓存的 TransformerFactory。
	 * Cached TransformerFactory.
	 */
	private static volatile TransformerFactory tf;

	/**
	 * 将 XML 字符串解析为 Document。
	 * Parse an XML string into a Document.
	 *
	 * @param xmlSource XML 源 / XML source
	 * @return Document
	 */
	public static Document getDocument(String xmlSource) {
		try {
			if (dbf == null)
				dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			Reader stream = new StringReader(xmlSource);
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse(new InputSource(stream));
		} catch (Exception e) {
			throw new RuntimeException("Error converting string to document", e);
		}
	}

	/**
	 * 将 Document 转为 XML 字符串。
	 * Convert a Document to an XML string.
	 *
	 * @param document DOM 文档 / DOM document
	 * @return XML 字符串 / XML string
	 */
	public static String getString(Document document) {
		try {
			if (tf == null)
				tf = TransformerFactory.newInstance();
			DOMSource domSource = new DOMSource(document);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			return writer.toString();
		} catch (TransformerException e) {
			throw new RuntimeException("Error converting document to string", e);
		}
	}

	/**
	 * 从 schema 文件路径创建 Schema。
	 * Create a Schema from a schema file path.
	 *
	 * @param schemaFile 相对/绝对 schema 路径 / Relative/absolute schema path
	 * @return Schema 对象 / Schema object
	 */
	public static Schema getSchema(String schemaFile) {
		return getSchema(schemaFile, false);
	}

	/**
	 * 从文件路径或源码创建 Schema。
	 * Create a Schema from a file path or source code.
	 *
	 * @param schemaFileOrSourceCode 文件路径或 schema 源码 / File path or schema source
	 * @param isSourceCode 是否为源码 / Whether the argument is source code
	 * @return Schema 对象 / Schema object
	 */
	public static Schema getSchema(String schemaFileOrSourceCode, boolean isSourceCode) {
		try {
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			if (isSourceCode)
				return sf.newSchema(new StreamSource(new StringReader(schemaFileOrSourceCode)));
			else
				return sf.newSchema(new File(schemaFileOrSourceCode));
		} catch (Exception e) {
			throw new RuntimeException("Failed to create schema from: " + schemaFileOrSourceCode, e);
		}
	}

	/**
	 * 用给定 Schema 校验 Document。
	 * Validate a Document against the given Schema.
	 *
	 * @param schema Schema
	 * @param document DOM 文档 / DOM document
	 */
	public static void validate(Schema schema, Document document) {
		Validator validator = schema.newValidator();
		try {
			validator.validate(new DOMSource(document));
		} catch (Exception e) {
			throw new RuntimeException("Failed to validate document", e);
		}
	}

	/**
	 * 枚举目录下的 .xml 文件。
	 * List .xml files under a directory.
	 *
	 * @param root 根路径 / Root path
	 * @param recursive 是否递归 / Whether recursive
	 * @return .xml 文件集合 / Collection of .xml files
	 * @see #listFiles(File, boolean)
	 */
	public static Collection<File> listFiles(String root, boolean recursive) {
		return listFiles(new File(root), recursive);
	}

	/**
	 * 搜索（非隐藏）.xml 文件并返回列表。
	 * Search for (non-hidden) .xml files and return them as a list.
	 *
	 * @param root 根目录（相对 / 绝对） / Root directory (relative/absolute)
	 * @param recursive 为 true 时包含子目录 / If true, include subdirectories
	 * @return 根目录下的 .xml 文件 / .xml files under root
	 */
	public static Collection<File> listFiles(File root, boolean recursive) {
		try {
			return Files.find(root.toPath(), recursive ? Integer.MAX_VALUE : 1, (path, attrs) -> attrs.isRegularFile() && path.toString().toLowerCase().endsWith(".xml")).map(Path::toFile).collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
