package com.aionemu.gameserver.model.templates.itemgroups;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.rewards.BonusType;
import com.aionemu.gameserver.model.templates.rewards.IdReward;

/**
 * 物品奖励组抽象基类：加成类型、概率与奖励条目。
 * Abstract item group: bonus type, chance and reward entries.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemGroup")
@XmlSeeAlso({ CraftItemGroup.class, CraftRecipeGroup.class, ManastoneGroup.class, FoodGroup.class, MedicineGroup.class,
		OreGroup.class, GatherGroup.class, EnchantGroup.class, BossGroup.class })
public abstract class ItemGroup {

	@XmlAttribute(name = "bonusType", required = true)
	protected BonusType bonusType;

	@XmlAttribute(name = "chance")
	protected Float chance;

	 /**
	  * 获取 bonusType 属性值。
	  * Gets the value of the bonusType property
	  * @return possible object is {@link BonusType }
	  */
	public BonusType getBonusType() {
		return bonusType;
	}

	 /**
	  * 获取 chance 属性值。
	  * Gets the value of the chance property
	  * @return possible object is {@link Float }
	  */
	public float getChance() {
		if (chance == null) {
			return 0.0F;
		} else {
			return chance;
		}
	}

	/** 获取奖励。 / Returns the rewards. */
	public abstract IdReward[] getRewards();
}
