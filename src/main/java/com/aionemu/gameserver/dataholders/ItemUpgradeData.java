package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.item.upgrade.ItemUpgradeTemplate;
import com.aionemu.gameserver.model.templates.item.upgrade.UpgradeResultItem;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 物品升级数据容器，按基础物品 ID 索引升级模板与结果物品。
 * Item upgrade data holder, indexing upgrade templates and result items by base item id.
 *
 * @author Ranastic (Encom)
 */
@XmlRootElement(name = "item_upgrades")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemUpgradeData {

	@XmlElement(name = "item_upgrade")
	protected List<ItemUpgradeTemplate> itemUpgradeTemplates;

	@XmlTransient
	private IntObjectHashMap<ItemUpgradeTemplate> itemUpgradeSets;
	@XmlTransient
	private Map<Integer, Map<Integer, UpgradeResultItem>> upgradeResultItemMap;

	/**
	 * JAXB 反序列化完成后，建立基础物品 ID 到升级模板与结果物品的索引。
	 * After JAXB unmarshalling, indexes upgrade templates and result items by base item id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		itemUpgradeSets = new IntObjectHashMap<ItemUpgradeTemplate>();
		upgradeResultItemMap = new LinkedHashMap<Integer, Map<Integer, UpgradeResultItem>>();
		for (ItemUpgradeTemplate set : itemUpgradeTemplates) {
			itemUpgradeSets.put(set.getUpgrade_base_item_id(), set);
			upgradeResultItemMap.put(set.getUpgrade_base_item_id(), new LinkedHashMap<Integer, UpgradeResultItem>());
			if (!set.getUpgrade_result_item().isEmpty()) {
				for (UpgradeResultItem resultItem : set.getUpgrade_result_item()) {
					upgradeResultItemMap.get(set.getUpgrade_base_item_id()).put(resultItem.getItem_id(), resultItem);
				}
			}
		}
		itemUpgradeTemplates = null;
	}

	/**
	 * 按基础物品 ID 获取升级模板。
	 * Returns the upgrade template for the given base item id.
	 *
	 * base item id
	 *
	 * @param itemSetId @return 升级模板或 null / upgrade template or null
	 */
	public ItemUpgradeTemplate getItemUpgradeTemplate(int itemSetId) {
		return itemUpgradeSets.get(itemSetId);
	}

	/**
	 * 按基础物品 ID 获取结果物品映射。
	 * Returns the result-item map for the given base item id.
	 *
	 * base item id
	 *
	 * @param baseItemId @return 结果物品映射，为空或不存在则为 null / result-item map or null
	 */
	public Map<Integer, UpgradeResultItem> getResultItemMap(int baseItemId) {
		if (upgradeResultItemMap.containsKey(baseItemId)) {
			if (!upgradeResultItemMap.get(baseItemId).isEmpty()) {
				return upgradeResultItemMap.get(baseItemId);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * 返回已加载的升级模板数量。
	 * Returns the number of loaded upgrade templates.
	 *
	 * template count
	 */
	public int size() {
		return itemUpgradeSets.size();
	}
}
