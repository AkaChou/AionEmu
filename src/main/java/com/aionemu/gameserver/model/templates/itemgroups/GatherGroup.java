package com.aionemu.gameserver.model.templates.itemgroups;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.rewards.IdReward;

/**
 * 采集奖励组：采集产出的物品种族条目。
 * Gather reward group: item race entries from gathering.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GatherGroup")
public class GatherGroup extends BonusItemGroup {

	@XmlElement(name = "item")
	protected List<ItemRaceEntry> items;

	/**
	 * 获取 item 属性值。 / Gets the value of the item property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the item property. <p> For example, to add a new item, do as follows: <pre> getItems().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link IdReward }
	 */
	public List<ItemRaceEntry> getItems() {
		if (items == null) {
			items = new ArrayList<ItemRaceEntry>();
		}
		return this.items;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aionemu.gameserver.model.templates.itemgroups.ItemGroup#getRewards()
	 */
	@Override
	public ItemRaceEntry[] getRewards() {
		return getItems().toArray(new ItemRaceEntry[0]);
	}
}
