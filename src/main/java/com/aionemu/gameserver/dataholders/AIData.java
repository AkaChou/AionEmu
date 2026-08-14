package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.ai.Ai;
import com.aionemu.gameserver.model.templates.ai.AITemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 模板数据容器，持有并索引全部 NPC AI 配置。
 * Container holding and indexing all NPC AI templates.
 *
 * @author xTz
 */
@XmlRootElement(name = "ai_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class AIData {

	@XmlElement(name = "ai", type = Ai.class)
	private List<Ai> templates;
	private Map<Integer, AITemplate> aiTemplate = new LinkedHashMap<Integer, AITemplate>();

	/**
	 * JAXB 反序列化完成后，将列表转为按 NPC ID 索引的 AI 模板映射。
	 * After JAXB unmarshalling, builds the NPC-id-indexed AI template map.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		aiTemplate.clear();
		for (Ai template : templates) {
			aiTemplate.put(template.getNpcId(), new AITemplate(template));
		}
	}

	/**
	 * 返回已加载的 AI 模板数量。
	 * Returns the number of loaded AI templates.
	 *
	 * @return 已加载的AI 模板数量 / Returns the number of loaded AI templates.
	 */
	public int size() {
		return aiTemplate.size();
	}

	/** 合并另一份 AI 模板数据。 / Merges another AI template data set. */
	public void merge(AIData data) {
		aiTemplate.putAll(data.aiTemplate);
	}

	/**
	 * 返回全部 AI 模板映射。
	 * Returns the full AI template map.
	 *
	 * @return NPC ID 到 AI 模板的映射 / map of NPC id to AI template
	 */
	public Map<Integer, AITemplate> getAiTemplate() {
		return aiTemplate;
	}
}
