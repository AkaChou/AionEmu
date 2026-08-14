package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.chest.ChestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 宝箱模板数据容器，按 NPC ID 与名称索引宝箱配置。
 * Chest template data holder, indexing chest configs by NPC id and name.
 *
 * @author Wakizashi
 */
@XmlRootElement(name = "chest_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class ChestData {

	@XmlElement(name = "chest")
	private List<ChestTemplate> chests;

	/** 宝箱模板索引 / chest template index */
	private IntObjectHashMap<ChestTemplate> chestData = new IntObjectHashMap<ChestTemplate>();
	private IntObjectHashMap<ArrayList<ChestTemplate>> instancesMap = new IntObjectHashMap<ArrayList<ChestTemplate>>();
	private Map<String, ChestTemplate> namedChests = new LinkedHashMap<String, ChestTemplate>();

	/**
	 * JAXB 反序列化完成后重建索引；保留原始列表以支持热重载。
	 * After JAXB unmarshalling, rebuilds indexes; keeps the source list for reloads.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		chestData.clear();
		instancesMap.clear();
		namedChests.clear();

		for (ChestTemplate chest : chests) {
			chestData.put(chest.getNpcId(), chest);
			if (chest.getName() != null && !chest.getName().isEmpty()) {
				namedChests.put(chest.getName(), chest);
			}
		}
	}

	/**
	 * 返回已加载的宝箱数量。
	 * Returns the number of loaded chests.
	 *
	 * @return 已加载的宝箱数量 / Returns the number of loaded chests.
	 */
	public int size() {
		return chestData.size();
	}

	/**
	 * 按 NPC ID 获取宝箱模板。
	 * Returns the chest template for the given NPC id.
	 *
	 * @param npcId NPC ID / npc id
	 * @return 模板，不存在则为 null / template or null
	 */
	public ChestTemplate getChestTemplate(int npcId) {
		return chestData.get(npcId);
	}

	/**
	 * 返回原始宝箱模板列表。
	 * Returns the raw chest template list.
	 *
	 * @return 宝箱模板列表 / chest template list
	 */
	public List<ChestTemplate> getChests() {
		return chests;
	}

	/**
	 * 设置宝箱列表并重建索引（用于热重载）。
	 * Sets the chest list and rebuilds indexes (for reload).
	 *
	 * @param chests 宝箱模板列表 / chest template list
	 */
	public void setChests(List<ChestTemplate> chests) {
		this.chests = chests;
		afterUnmarshal(null, null);
	}
}
