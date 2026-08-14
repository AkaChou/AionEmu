package com.aionemu.gameserver.dataholders;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.model.templates.housing.LBox;

/**
 * 房屋脚本（LBox）配置数据容器，负责默认脚本模板索引与脚本 XML 片段生成。
 * House script (LBox) configuration data holder for default script templates and script XML fragment generation.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "lboxes")
@Slf4j
public class HouseScriptData {
	private static Marshaller marshaller;

	@XmlElement(name = "lbox", required = true)
	protected List<LBox> scriptData;

	@XmlTransient
	private final Map<Integer, LBox> defaultTemplates;

	/**
	 * 初始化默认模板索引容器。
	 * Initializes the default template index container.
	 */
	public HouseScriptData() {
		defaultTemplates = new HashMap<Integer, LBox>();
	}

	/**
	 * JAXB 反序列化完成后，按脚本 ID 建立默认模板索引并释放原始列表。
	 * After JAXB unmarshalling, indexes default templates by script id and releases the raw list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (LBox template : scriptData) {
			defaultTemplates.put(template.getId(), template);
		}
		scriptData.clear();
		scriptData = null;
	}

	/**
	 * 基于默认脚本模板生成指定位置与图标的脚本 XML 片段。
	 * Builds a script XML fragment for the given position and icon from a default script template.
	 *
	 * @param scriptId 默认脚本模板 ID / default script template id
	 * @param position 脚本位置 ID / script position id
	 * @param iconId 图标 ID / icon id
	 * @return 格式化后的脚本 XML 字符串 / formatted script XML string
	 */
	public String createScript(int scriptId, int position, int iconId) {
		LBox template = defaultTemplates.get(scriptId);
		LBox result = (LBox) template.clone();
		result.setId(position);
		result.setIcon(iconId);
		HouseScriptData fragment = new HouseScriptData();
		fragment.scriptData = new ArrayList<LBox>();
		fragment.scriptData.add(result);
		Writer writer = new StringWriter();
		try {
			marshaller.marshal(fragment, writer);
		} catch (JAXBException e) {
		}
		return XmlFormatter.format(writer.toString());
	}

	/**
	 * 返回默认脚本模板数量。
	 * Returns the number of default script templates.
	 *
	 * @return 默认脚本模板数量 / Returns the number of default script templates.
	 */
	public int size() {
		return defaultTemplates.size();
	}

	static {
		SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		Schema schema = null;
		JAXBContext jc = null;
		try {
			schema = sf.newSchema(Config.dataFile("./data/static_data/housing/scripts.xsd"));
			jc = JAXBContext.newInstance(new Class[] { HouseScriptData.class });
			marshaller = jc.createMarshaller();
			marshaller.setSchema(schema);
			marshaller.setProperty("jaxb.encoding", "UTF-8");
		} catch (Exception e) {
			log.error(I18n.get("log.6cadba7ab22f", e));
		}
	}

	/**
	 * 简易 XML 格式化工具，用于缩进与 UTF-8 输出。
	 * Simple XML formatter utility for indentation and UTF-8 output.
	 */
	@Slf4j
	public static class XmlFormatter {
		private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		private static DocumentBuilder db;

		/**
		 * 将未格式化的 XML 字符串格式化为缩进输出。
		 * Formats an unformatted XML string into indented output.
		 *
		 * @param unformattedXml 原始 XML 字符串 / raw XML string
		 * @return 格式化后的 XML，失败则为 null / formatted XML, or null on failure
		 */
		public static String format(String unformattedXml) {
			try {
				Document document = parseXmlFile(unformattedXml);
				Writer out = new StringWriter();
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				transformer.transform(new DOMSource(document), new StreamResult(out));
				return out.toString();
			} catch (TransformerException e) {
			}
			return null;
		}

		/**
		 * 将 XML 字符串解析为 DOM 文档。
		 * Parses an XML string into a DOM document.
		 *
		 * @param in XML 输入字符串 / XML input string
		 * @return DOM 文档 / DOM document
		 */
		private static Document parseXmlFile(String in) {
			try {
				InputSource is = new InputSource(new StringReader(in));
				return db.parse(is);
			} catch (SAXException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		static {
			try {
				db = dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				log.error(I18n.get("log.dd62a8c2d8c4", e));
			}
		}
	}
}
