package com.aionemu.gameserver.model.templates.item.upgrade;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.stats.calc.StatOwner;

/**
 * 物品升级模板（静态数据/XML）。
 * XML template.
 *
 * @author Ranastic (Encom)
 */

@XmlRootElement(name = "ItemUpgrade")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemUpgradeTemplate implements StatOwner {
	protected List<UpgradeResultItem> upgrade_result_item;

	@XmlAttribute(name = "upgrade_base_item")
	private int upgrade_base_item_id;

	void afterUnmarshal(Unmarshaller u, Object parent) {
	}

	/** 获取升级结果物品。 / Returns the upgrade result item. */
	public List<UpgradeResultItem> getUpgrade_result_item() {
		return upgrade_result_item;
	}

	/** 返回 upgrade base item id / Returns the upgrade base item id */
	public int getUpgrade_base_item_id() {
		return upgrade_base_item_id;
	}
}
