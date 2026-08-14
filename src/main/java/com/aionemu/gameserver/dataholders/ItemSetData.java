package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.itemset.ItemPart;
import com.aionemu.gameserver.model.templates.itemset.ItemSetTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 物品套装数据容器，按套装 ID 与部件物品 ID 索引 {@link ItemSetTemplate}。
 * Item-set data holder, indexing {@link ItemSetTemplate} by set id and part item id.
 *
 * @author ATracer
 */
@XmlRootElement(name = "item_sets")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemSetData {

	@XmlElement(name = "itemset")
	protected List<ItemSetTemplate> itemsetList;

	private IntObjectHashMap<ItemSetTemplate> sets;

	// 键：物品 ID；值：关联物品套装模板 / key: item id, value: associated item set template
	// 这应能按物品 ID 更快搜索物品模板集。 / This should provide faster search of the item template set by item id
	private IntObjectHashMap<ItemSetTemplate> setItems;

	/**
	 * JAXB 反序列化完成后，建立套装 ID 与部件物品 ID 双重索引。
	 * After JAXB unmarshalling, builds dual indexes by set id and part item id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		sets = new IntObjectHashMap<ItemSetTemplate>();
		setItems = new IntObjectHashMap<ItemSetTemplate>();

		for (ItemSetTemplate set : itemsetList) {
			sets.put(set.getId(), set);

			// 添加对 ItemSetTemplate 的引用，来自 / Add reference to the ItemSetTemplate from
			for (ItemPart part : set.getItempart()) {
				setItems.put(part.getItemid(), set);
			}
		}
		itemsetList = null;
	}

	/**
	 * 按套装 ID 获取套装模板。
	 * Returns the item-set template for the given set id.
	 *
	 * @param itemSetId 物品套装 ID / item-set id
	 * @return 套装模板或 null / item-set template or null
	 */
	public ItemSetTemplate getItemSetTemplate(int itemSetId) {
		return sets.get(itemSetId);
	}

	/**
	 * 按部件物品 ID 获取所属套装模板。
	 * Returns the item-set template associated with the given part item id.
	 *
	 * @param itemId 部件物品 ID / part item id
	 * @return 套装模板或 null / item-set template or null
	 */
	public ItemSetTemplate getItemSetTemplateByItemId(int itemId) {
		return setItems.get(itemId);
	}

	/**
	 * 返回已加载的套装数量。
	 * Returns the number of loaded item sets.
	 *
	 * @return 已加载的物品套装数量 / Returns the number of loaded item sets.
	 */
	public int size() {
		return sets.size();
	}
}
