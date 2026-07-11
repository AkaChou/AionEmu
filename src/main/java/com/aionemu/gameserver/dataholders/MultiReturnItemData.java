package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.teleport.MultiReturn;
import com.aionemu.gameserver.model.templates.teleport.MultiReturnLocationList;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 多目标返回物品数据容器，按物品 ID 索引返回地点列表。
 * Multi-return item data holder, indexing return location lists by item id.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "multi_returns")
@XmlAccessorType(XmlAccessType.FIELD)
public class MultiReturnItemData {
	@XmlElement(name = "item")
	private List<MultiReturn> ItemList;

	@XmlTransient
	private IntObjectHashMap<List<MultiReturnLocationList>> ItemLocationList = new IntObjectHashMap<>();
	@XmlTransient
	private IntObjectHashMap<MultiReturn> itemsById = new IntObjectHashMap<>();

	/**
	 * JAXB 反序列化完成后，按物品 ID 建立返回地点与模板索引。
	 * After JAXB unmarshalling, indexes return locations and templates by item id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		ItemLocationList.clear();
		itemsById.clear();
		for (MultiReturn template : ItemList) {
			ItemLocationList.put(template.getId(), template.getMultiReturnList());
			itemsById.put(template.getId(), template);
		}
	}

	/**
	 * 返回已加载的多目标返回物品数量。
	 * Returns the number of loaded multi-return items.
	 *
	 * item count
	 */
	public int size() {
		return ItemLocationList.size();
	}

	/**
	 * 按物品 ID 获取多目标返回模板。
	 * Returns the multi-return template for the given item id.
	 *
	 * @param id 物品 ID / item id
	 * @return 多目标返回模板或 null / multi-return template or null
	 */
	public MultiReturn getMultiReturnById(int id) {
		return itemsById.get(id);
	}

	/**
	 * 返回全部多目标返回物品列表。
	 * Returns the full multi-return item list.
	 *
	 * @return 多目标返回物品列表 / multi-return item list
	 */
	public List<MultiReturn> getMultiReturns() {
		return ItemList;
	}
}
