package com.aionemu.gameserver.dataholders;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NPC 巡逻路径静态数据容器，按路线 ID 索引 Walker 模板，并支持回写生成 XML。
 * NPC walker-route static-data holder, indexing walker templates by route id and supporting XML export.
 *
 * @author KKnD, Rolandas
 */
@XmlRootElement(name = "npc_walker")
@XmlAccessorType(XmlAccessType.FIELD)
@Slf4j
public class WalkerData {


	@XmlElement(name = "walker_template")
	private List<WalkerTemplate> walkerlist;

	@XmlTransient
	private Map<String, WalkerTemplate> walkerlistData = new LinkedHashMap<String, WalkerTemplate>();

	/**
	 * JAXB 反序列化完成后，将路线按 ID 索引并跳过重复项，随后释放列表。
	 * After JAXB unmarshalling, indexes routes by id (skipping duplicates) and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (WalkerTemplate route : walkerlist) {
			if (walkerlistData.containsKey(route.getRouteId())) {
				log.warn(I18n.get("log.3bd46a3bd18f", route.getRouteId()));
				continue;
			}
			walkerlistData.put(route.getRouteId(), route);
		}
		walkerlist.clear();
		walkerlist = null;
	}

	/**
	 * 返回已加载的巡逻路线数量。
	 * Returns the number of loaded walker routes.
	 *
	 * route count
	 */
	public int size() {
		return walkerlistData.size();
	}

	/**
	 * 按路线 ID 获取巡逻模板。
	 * Returns the walker template for the given route id.
	 *
	 * route id
	 *
	 * @param routeId
	 * @return 巡逻模板，不存在或参数为 null 则为 null / walker template, or null if missing/null id
	 */
	public WalkerTemplate getWalkerTemplate(String routeId) {
		if (routeId == null)
			return null;
		return walkerlistData.get(routeId);
	}

	/** 将另一份完整路径数据合并进当前索引，同名路径以后加载的数据为准。 */
	public void merge(WalkerData data) {
		walkerlistData.putAll(data.walkerlistData);
	}

	/**
	 * 追加一条待导出的巡逻模板到内部列表。
	 * Appends a walker template to the internal list for later export.
	 *
	 * new template
	 */
	public void AddTemplate(WalkerTemplate newTemplate) {
		if (walkerlist == null)
			walkerlist = new ArrayList<WalkerTemplate>();
		walkerlist.add(newTemplate);
	}

	/**
	 * 将当前待导出模板按指定路线 ID 序列化为生成的 NPC Walker XML。
	 * Marshals pending templates into a generated NPC walker XML for the given route id.
	 *
	 * @param routeId 用于命名输出文件的路线 ID / route id used to name the output file
	 */
	public void saveData(String routeId) {
		Schema schema = null;
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		try {
			schema = sf.newSchema(Config.dataFile("./data/static_data/npc_walker/npc_walker.xsd"));
		} catch (SAXException e1) {
			log.error(I18n.get("log.a52b870058c9", e1.getMessage(), e1.getCause()), e1);
			return;
		}

		File xml = Config.dataFile("./data/static_data/npc_walker/generated_npc_walker_" + routeId + ".xml");
		JAXBContext jc;
		Marshaller marshaller;
		try {
			jc = JAXBContext.newInstance(WalkerData.class);
			marshaller = jc.createMarshaller();
			marshaller.setSchema(schema);
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(this, xml);
		} catch (JAXBException e) {
			log.error(I18n.get("log.a52b870058c9", e.getMessage(), e.getCause()), e);
			return;
		} finally {
			if (walkerlist != null) {
				walkerlist.clear();
				walkerlist = null;
			}
		}
	}

	/**
	 * 返回全部已加载的巡逻模板集合。
	 * Returns the collection of all loaded walker templates.
	 *
	 * template collection
	 */
	public Collection<WalkerTemplate> getTemplates() {
		return walkerlistData.values();
	}
}
