package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.item.AssemblyItem;
import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 组装物品数据容器，按物品 ID 索引组装配方模板。
 * Assembly item data holder, indexing assembly item recipes by item id.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "item" })
@XmlRootElement(name = "assembly_items")
public class AssemblyItemsData {

	@XmlElement(required = true)
	protected List<AssemblyItem> item;

	@XmlTransient
	private IntObjectHashMap<AssemblyItem> itemsById = new IntObjectHashMap<AssemblyItem>();

	/**
	 * JAXB 反序列化完成后，按物品 ID 建立索引并释放列表。
	 * After JAXB unmarshalling, indexes templates by item id and clears the list.
	 */
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		itemsById.clear();
		for (AssemblyItem template : item) {
			itemsById.put(template.getId(), template);
		}
		item.clear();
		item = null;
	}

	/**
	 * 返回已加载的组装物品数量。
	 * Returns the number of loaded assembly items.
	 *
	 * @return 已加载的组装物品数量 / Returns the number of loaded assembly items.
	 */
	public int size() {
		return itemsById.size();
	}

	/**
	 * 按物品 ID 获取组装物品模板。
	 * Returns the assembly item template for the given item id.
	 *
	 * @param itemId 物品 ID / item id
	 * @return 模板，不存在则为 null / template or null
	 */
	public AssemblyItem getAssemblyItem(int itemId) {
		return itemsById.get(itemId);
	}
}
