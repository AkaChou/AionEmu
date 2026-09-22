package com.aionemu.gameserver.model.templates.item.upgrade;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.stats.calc.StatOwner;
import lombok.Getter;

/**
 * 物品升级模板（静态数据/XML）。
 * Item upgrade template (static data/XML).
 * @author Ranastic (Encom)
 */

@Getter
@XmlRootElement(name = "ItemUpgrade")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemUpgradeTemplate implements StatOwner {
	/** 获取升级结果物品。 / Returns the upgrade result item. */
	protected List<UpgradeResultItem> upgrade_result_item;

	/** 返回升级基础物品 ID / Returns the upgrade base item id */
	@XmlAttribute(name = "upgrade_base_item")
	private int upgrade_base_item_id;

	void afterUnmarshal(Unmarshaller u, Object parent) {
	}
}
