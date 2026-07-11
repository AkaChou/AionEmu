package com.aionemu.gameserver.dataholders;

import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.atreian_bestiary.AtreianBestiaryTemplate;

import java.util.LinkedHashMap;

/**
 * 阿特里亚图鉴数据容器，分别按图鉴 ID 与 NPC ID 双索引。
 * Atreian bestiary data holder, dual-indexed by book id and NPC id.
 *
 * @author Ranastic
 */
@XmlRootElement(name = "monster_books")
@XmlAccessorType(XmlAccessType.FIELD)
public class AtreianBestiaryData {

	@XmlElement(name = "monster_book", type = AtreianBestiaryTemplate.class)
	private List<AtreianBestiaryTemplate> templates;

	private final Map<Integer, AtreianBestiaryTemplate> idsHolder = new LinkedHashMap<Integer, AtreianBestiaryTemplate>();
	private final Map<Integer, AtreianBestiaryTemplate> npcIdsHolder = new LinkedHashMap<Integer, AtreianBestiaryTemplate>();

	/**
	 * JAXB 反序列化完成后，按图鉴 ID 与 NPC ID 建立双索引并释放列表。
	 * After JAXB unmarshalling, dual-indexes by book id and NPC id, then clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (AtreianBestiaryTemplate template : templates) {
			idsHolder.put(template.getId(), template);
			if (!template.getNpcIds().isEmpty()) {
				for (int npcId : template.getNpcIds()) {
					npcIdsHolder.put(npcId, template);
				}
			}
		}
		templates.clear();
		templates = null;
	}

	/**
	 * 返回按图鉴 ID 索引的模板数量。
	 * Returns the number of templates indexed by book id.
	 *
	 * template count
	 */
	public int size() {
		return idsHolder.size();
	}

	/**
	 * 按图鉴 ID 获取模板。
	 * Returns the bestiary template for the given book id.
	 *
	 * @param id 图鉴 ID / book id
	 * @return 模板，不存在则为 null / template or null
	 */
	public AtreianBestiaryTemplate getAtreianBestiaryTemplate(int id) {
		return idsHolder.get(id);
	}

	/**
	 * 返回按 NPC ID 索引的条目数量。
	 * Returns the number of entries indexed by NPC id.
	 *
	 * NPC-index count
	 */
	public int sizeByNpcId() {
		return npcIdsHolder.size();
	}

	/**
	 * 按 NPC ID 获取图鉴模板。
	 * Returns the bestiary template for the given NPC id.
	 *
	 * npc id
	 *
	 * @param id @return 模板，不存在则为 null / template or null
	 */
	public AtreianBestiaryTemplate getAtreianBestiaryTemplateByNpcId(int id) {
		return npcIdsHolder.get(id);
	}
}
