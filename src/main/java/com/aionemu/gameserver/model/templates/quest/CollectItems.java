package com.aionemu.gameserver.model.templates.quest;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Collect 物品模板（静态数据/XML）。
 * XML template.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CollectItems", propOrder = { "collectItem" })
public class CollectItems {

	@XmlElement(name = "collect_item")
	protected List<CollectItem> collectItem;

	/**
	 * 获取 collectItem 属性值。 / Gets the value of the collectItem property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the collectItem property. <p> For example, to add a new item, do as follows: <pre> getCollectItem().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link CollectItem }
	 */
	public List<CollectItem> getCollectItem() {
		if (collectItem == null) {
			collectItem = new ArrayList<CollectItem>();
		}
		return this.collectItem;
	}
}
