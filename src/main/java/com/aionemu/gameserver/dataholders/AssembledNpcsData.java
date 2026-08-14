package com.aionemu.gameserver.dataholders;

import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.assemblednpc.AssembledNpcTemplate;

import java.util.LinkedHashMap;

/**
 * 组装 NPC 数据容器，按编号索引组装 NPC 模板。
 * Assembled NPC data holder, indexing assembled NPC templates by number.
 *
 * @author xTz
 */
@XmlRootElement(name = "assembled_npcs")
@XmlAccessorType(XmlAccessType.FIELD)
public class AssembledNpcsData {

	@XmlElement(name = "assembled_npc", type = AssembledNpcTemplate.class)
	private List<AssembledNpcTemplate> templates;
	private final Map<Integer, AssembledNpcTemplate> assembledNpcsTemplates = new LinkedHashMap<Integer, AssembledNpcTemplate>();

	/**
	 * JAXB 反序列化完成后，按编号建立索引并释放列表。
	 * After JAXB unmarshalling, indexes templates by number and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (AssembledNpcTemplate template : templates) {
			assembledNpcsTemplates.put(template.getNr(), template);
		}
		templates.clear();
		templates = null;
	}

	/**
	 * 返回已加载的模板数量。
	 * Returns the number of loaded templates.
	 *
	 * @return 已加载的模板数量 / Returns the number of loaded templates.
	 */
	public int size() {
		return assembledNpcsTemplates.size();
	}

	/**
	 * 按编号获取组装 NPC 模板。
	 * Returns the assembled NPC template for the given number.
	 *
	 * @param i 组装编号 / assembly number
	 * @return 模板，不存在则为 null / template or null
	 */
	public AssembledNpcTemplate getAssembledNpcTemplate(Integer i) {
		return assembledNpcsTemplates.get(i);
	}
}
