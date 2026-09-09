package com.aionemu.gameserver.model.templates.itemgroups;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.rewards.IdLevelReward;
import lombok.Getter;

/**
 * 物品种族条目：物品 ID 与允许种族，并按模板校验种族。
 * Item race entry: item id and allowed race, validated against the template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemRaceEntry")
@XmlSeeAlso({ IdLevelReward.class })
public class ItemRaceEntry {

	/** 返回 ID / Returns the id */
	@Getter
	@XmlAttribute(name = "id", required = true)
	protected int id;

	/** 获取种族。 / Returns the race. */
	@Getter
	@XmlAttribute(name = "race")
	protected Race race;

	/** 检查种族。 / Check race. */
	public boolean checkRace(Race playerRace) {
		ItemTemplate template = DataManager.ITEM_DATA.getItemTemplate(id);
		return (template.getRace() == Race.PC_ALL && (race == null || race == playerRace))
				|| (template.getRace() != Race.PC_ALL && template.getRace() == playerRace);
	}
}
