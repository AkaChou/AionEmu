package com.aionemu.gameserver.model.templates.itemgroups;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.rewards.BonusType;

/**
 * 加成物品队伍模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BonusItemGroup")
@XmlSeeAlso({ CraftItemGroup.class, CraftRecipeGroup.class, ManastoneGroup.class, FoodGroup.class, MedicineGroup.class,
		OreGroup.class, GatherGroup.class, EnchantGroup.class, BossGroup.class })
public abstract class BonusItemGroup {

	@XmlAttribute(name = "bonusType", required = true)
	protected BonusType bonusType;

	@XmlAttribute(name = "chance")
	protected Float chance;

	/** 获取加成类型。 / Returns the bonus type. */
	public BonusType getBonusType() {
		return bonusType;
	}

	/** 返回概率 / Returns the chance*/
	public float getChance() {
		if (chance == null) {
			return 0.0f;
		}
		return chance.floatValue();
	}

	/** 获取奖励。 / Returns the rewards. */
	public abstract ItemRaceEntry[] getRewards();
}
