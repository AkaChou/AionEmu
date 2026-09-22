package com.aionemu.gameserver.model.templates.item.upgrade;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 升级结果物品模板（静态数据/XML）。
 * Upgrade result item template (static data/XML).
 * @author Ranastic (Encom)
 */

@Getter
@XmlRootElement(name = "UpgradeResultItem")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpgradeResultItem {
	/** 返回物品 ID / Returns the item id */
	@XmlAttribute(name = "item_id")
	private int item_id;

	/** 返回检查强化数量 / Returns the check enchant count*/
	@XmlAttribute(name = "check_enchant_count")
	private int check_enchant_count;

	/** 返回 check authorize count / Returns the check authorize count */
	@XmlAttribute(name = "check_authorize_count")
	private int check_authorize_count;

	/** 获取升级材料。 / Returns the upgrade materials. */
	private UpgradeMaterials upgrade_materials;

	/** 返回 need abyss point / Returns the need abyss point */
	private NeedAbyssPoint need_abyss_point;

	/** 返回 need kinah / Returns the need kinah */
	private NeedKinah need_kinah;
}
