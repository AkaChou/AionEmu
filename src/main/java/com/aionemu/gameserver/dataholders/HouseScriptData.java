/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.dataholders;

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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "lboxes")
@Slf4j
public class HouseScriptData {
	private static Marshaller marshaller;

	@XmlElement(name = "lbox", required = true)
	protected List<LBox> scriptData;

	@XmlTransient
	private final Map<Integer, LBox> defaultTemplates;

	public HouseScriptData() {
		defaultTemplates = new HashMap<Integer, LBox>();
	}

	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (LBox template : scriptData) {
			defaultTemplates.put(template.getId(), template);
		}
		scriptData.clear();
		scriptData = null;
	}

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
			log.error("Could not instantiate HouseScriptData : \n" + e);
		}
	}

	@Slf4j
	public static class XmlFormatter {
		private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		private static DocumentBuilder db;

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
				log.error("Could not instantiate XmlFormatter : \n" + e);
			}
		}
	}
}
