package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.items.RandomBonusResult;
import com.aionemu.gameserver.model.templates.item.bonuses.RandomBonus;
import com.aionemu.gameserver.model.templates.item.bonuses.StatBonusType;
import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 物品随机加成数据容器，按加成类型与选项集 ID 索引 {@link RandomBonus}。
 * Item random-bonus data holder, indexing {@link RandomBonus} by bonus type and option-set id.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "randomBonuses" })
@XmlRootElement(name = "random_bonuses")
public class ItemRandomBonusData {
	@XmlElement(name = "random_bonus", required = true)
	protected List<RandomBonus> randomBonuses;

	@XmlTransient
	private IntObjectHashMap<RandomBonus> inventoryRandomBonusData = new IntObjectHashMap<RandomBonus>();

	@XmlTransient
	private IntObjectHashMap<RandomBonus> polishRandomBonusData = new IntObjectHashMap<RandomBonus>();

	/**
	 * JAXB 反序列化完成后，按加成类型建立索引并释放列表。
	 * After JAXB unmarshalling, indexes bonuses by type and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (RandomBonus bonus : randomBonuses) {
			getBonusMap(bonus.getBonusType()).put(bonus.getId(), bonus);
		}
		randomBonuses.clear();
		randomBonuses = null;
	}

	private IntObjectHashMap<RandomBonus> getBonusMap(StatBonusType bonusType) {
		if (bonusType == StatBonusType.INVENTORY) {
			return inventoryRandomBonusData;
		}
		return polishRandomBonusData;
	}

	/**
	 * 按权重随机抽取一组随机加成修正。
	 * Randomly selects a modifiers set for the given bonus type and option set by weight.
	 *
	 * bonus type
	 *
	 * @param rndOptionSet 随机选项集 ID / random option-set id
	 * @param rndOptionSet @return 随机加成结果，无匹配则为 null / random bonus result or null
	 */
	public RandomBonusResult getRandomModifiers(StatBonusType bonusType, int rndOptionSet) {
		RandomBonus bonus = getBonusMap(bonusType).get(rndOptionSet);
		if (bonus == null) {
			return null;
		}
		List<ModifiersTemplate> modifiersGroup = bonus.getModifiers();
		int chance = Rnd.get(10000);
		int current = 0;
		ModifiersTemplate template = null;
		int number = 0;
		for (int i = 0; i < modifiersGroup.size(); i++) {
			ModifiersTemplate modifiers = modifiersGroup.get(i);
			current += modifiers.getChance() * 100;
			if (current >= chance) {
				template = modifiers;
				number = i + 1;
				break;
			}
		}
		return template == null ? null : new RandomBonusResult(template, number);
	}

	/**
	 * 按加成类型、选项集与序号获取修正模板。
	 * Returns the modifiers template for the given bonus type, option set and 1-based index.
	 *
	 * bonus type
	 *
	 * @param rndOptionSet 随机选项集 ID / random option-set id
	 * @param number 1 起始的修正序号 / 1-based modifiers index
	 * @param number @return 修正模板或 null / modifiers template or null
	 */
	public ModifiersTemplate getTemplate(StatBonusType bonusType, int rndOptionSet, int number) {
		RandomBonus bonus = getBonusMap(bonusType).get(rndOptionSet);
		if (bonus == null) {
			return null;
		}
		return bonus.getModifiers().get(number - 1);
	}

	/**
	 * 返回背包与抛光随机加成的总数量。
	 * Returns the total number of inventory and polish random bonuses.
	 *
	 * total bonus count
	 */
	public int size() {
		return inventoryRandomBonusData.size() + polishRandomBonusData.size();
	}
}
