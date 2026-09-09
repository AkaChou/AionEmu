package com.aionemu.gameserver.model.templates.item.upgrade;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 升级结果物品模板（静态数据/XML）。
 * Upgrade result item template (static data/XML).
 *
 * @author Ranastic (Encom)
 */

@XmlRootElement(name = "UpgradeResultItem")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpgradeResultItem {
	/** 返回物品 ID / Returns the item id */
	@Getter
	@XmlAttribute(name = "item_id")
	private int item_id;

	/** 返回检查强化数量 / Returns the check enchant count*/
	@Getter
	@XmlAttribute(name = "check_enchant_count")
	private int check_enchant_count;

	/** 返回 check authorize count / Returns the check authorize count */
	@Getter
	@XmlAttribute(name = "check_authorize_count")
	private int check_authorize_count;

	/** 获取升级材料。 / Returns the upgrade materials. */
	@Getter
	private UpgradeMaterials upgrade_materials;

	/** 返回 need abyss point / Returns the need abyss point */
	@Getter
	private NeedAbyssPoint need_abyss_point;

	/** 返回 need kinah / Returns the need kinah */
	@Getter
	private NeedKinah need_kinah;
}
