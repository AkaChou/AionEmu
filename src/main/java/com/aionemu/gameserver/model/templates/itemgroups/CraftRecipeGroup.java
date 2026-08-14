package com.aionemu.gameserver.model.templates.itemgroups;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.rewards.CraftRecipe;

/**
 * 制作配方奖励组：制作产出的配方条目。
 * Craft recipe reward group: recipe entries produced by crafting.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CraftRecipeGroup")
public class CraftRecipeGroup extends CraftGroup {

	@XmlElement(name = "item")
	protected List<CraftRecipe> items;

	/**
	 * 获取 item 属性值。 / Gets the value of the item property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the item property. <p> For example, to add a new item, do as follows: <pre> getItems().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link CraftRecipe }
	 */
	public List<CraftRecipe> getItems() {
		if (items == null) {
			items = new ArrayList<CraftRecipe>();
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
