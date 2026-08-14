package com.aionemu.gameserver.dataholders.loadingutils.adapters;

import jakarta.xml.bind.annotation.XmlElement;

/**
 * NPC 装备列表 JAXB 绑定载体，仅持有装备物品 ID 数组。
 * NPC equipment-list JAXB binding carrier holding equipped item ids only.
 *
 * @author Luno
 */
public class NpcEquipmentList {

	/**
	 * 装备物品 ID 数组。
	 * Equipped item id array.
	 */
	@XmlElement(name = "item")
	public int[] itemIds;

}
