package com.aionemu.gameserver.model.templates.itemgroups;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.rewards.MedalItem;

/**
 * 勋章奖励组：勋章物品条目。
 * Medal reward group: medal item entries.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MedalGroup")
public class MedalGroup extends BonusItemGroup {

	@XmlElement(name = "item")
	protected List<MedalItem> items;

	/** 获取物品。 / Returns the items. */
	public List<MedalItem> getItems() {
		if (items == null) {
			items = new ArrayList<MedalItem>();
		}
		return items;
	}

	/** 获取奖励。 / Returns the rewards. */
	@Override
	public ItemRaceEntry[] getRewards() {
		return (ItemRaceEntry[]) getItems().toArray(new ItemRaceEntry[0]);
	}
}
