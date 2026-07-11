package com.aionemu.gameserver.dataholders;

import java.util.EnumMap;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.templates.item.ItemSkillEnhance;

/**
 * 物品技能强化数据容器，按 ID 与职业索引 {@link ItemSkillEnhance}。
 * Item skill-enhance data holder, indexing {@link ItemSkillEnhance} by id and player class.
 *
 * Created by wanke on 01/03/2017.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "item_skill_enhances")
public class ItemSkillEnhanceData {
	@XmlElement(name = "item_skill_enhance", required = true)
	protected List<ItemSkillEnhance> skillEnhances;

	@XmlTransient
	protected IntObjectHashMap<ItemSkillEnhance> enhanceSkillsById = new IntObjectHashMap<ItemSkillEnhance>();

	@XmlTransient
	protected IntObjectHashMap<EnumMap<PlayerClass, ItemSkillEnhance>> enhanceSkillsByIdAndClass = new IntObjectHashMap<EnumMap<PlayerClass, ItemSkillEnhance>>();

	/**
	 * 按 ID 获取技能强化模板（忽略职业）。
	 * Returns the skill-enhance template for the given id (class-agnostic).
	 *
	 * @param id 强化 ID / enhance id
	 * @return 技能强化模板或 null / skill-enhance template or null
	 */
	public ItemSkillEnhance getSkillEnhance(int id) {
		return enhanceSkillsById.get(id);
	}

	/**
	 * 按 ID 与职业获取技能强化模板；无精确匹配时回退到 {@link PlayerClass#ALL}。
	 * Returns the skill-enhance template for the given id and class; falls back to {@link PlayerClass#ALL}.
	 *
	 * @param id 强化 ID / enhance id
	 * player class
	 * @return 技能强化模板或 null / skill-enhance template or null
	 */
	public ItemSkillEnhance getSkillEnhance(int id, PlayerClass playerClass) {
		EnumMap<PlayerClass, ItemSkillEnhance> enhanceSkillsByClass = enhanceSkillsByIdAndClass.get(id);
		if (enhanceSkillsByClass == null) {
			return null;
		}
		ItemSkillEnhance enhance = playerClass == null ? null : enhanceSkillsByClass.get(playerClass);
		if (enhance != null) {
			return enhance;
		}
		return enhanceSkillsByClass.get(PlayerClass.ALL);
	}

	/**
	 * JAXB 反序列化完成后，建立 ID 与 ID+职业双重索引并释放列表。
	 * After JAXB unmarshalling, builds id and id+class indexes and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		enhanceSkillsById.clear();
		enhanceSkillsByIdAndClass.clear();
		for (ItemSkillEnhance enhance : skillEnhances) {
			enhanceSkillsById.put(enhance.getId(), enhance);
			enhanceSkillsByIdAndClass.computeIfAbsent(enhance.getId(), id -> new EnumMap<PlayerClass, ItemSkillEnhance>(PlayerClass.class))
					.put(enhance.getClassId(), enhance);
		}
		skillEnhances.clear();
		skillEnhances = null;
	}

	/**
	 * 返回已加载的技能强化数量。
	 * Returns the number of loaded skill enhances.
	 *
	 * template count
	 */
	public int size() {
		return enhanceSkillsById.size();
	}
}
