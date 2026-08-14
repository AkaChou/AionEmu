package com.aionemu.gameserver.model.templates.rewards;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * ID 奖励模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdReward")
@XmlSeeAlso({ IdLevelReward.class })
public class IdReward {

	@XmlAttribute(name = "id", required = true)
	protected int id;

	@XmlAttribute(name = "race")
	protected Race race;

	/**
	 * 获取 id 属性值。
	 * Gets the value of the id property
	 */
	public int getId() {
		return id;
	}

	/**
	 * 获取 race 属性值。
	 * Gets the value of the race property
	 * @return 可能的对象类型 / possible object is {@link Race }
	 */
	public Race getRace() {
		return race;
	}

	/**
	 * 检查物品种族；部分 PC_ALL 物品实际并非双种族可用。 / Method is used to check item race; Some items having PC_ALL really are not for both races, like some foods and weapons.
	 */
	public boolean checkRace(Race playerRace) {
		ItemTemplate template = DataManager.ITEM_DATA.getItemTemplate(id);
		return template.getRace() == Race.PC_ALL && (race == null || race == playerRace)
				|| template.getRace() != Race.PC_ALL && template.getRace() == playerRace;
	}
}
