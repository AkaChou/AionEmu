package com.aionemu.gameserver.model.templates.item.upgrade;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 升级结果物品模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Ranastic (Encom)
 */

@XmlRootElement(name = "UpgradeResultItem")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpgradeResultItem {
	@XmlAttribute(name = "item_id")
	private int item_id;

	@XmlAttribute(name = "check_enchant_count")
	private int check_enchant_count;

	@XmlAttribute(name = "check_authorize_count")
	private int check_authorize_count;

	private UpgradeMaterials upgrade_materials;

	private NeedAbyssPoint need_abyss_point;

	private NeedKinah need_kinah;

	/** 返回检查强化数量 / Returns the check enchant count*/
	public int getCheck_enchant_count() {
		return check_enchant_count;
	}

	/** 返回 check authorize count / Returns the check authorize count */
	public int getCheck_authorize_count() {
		return check_authorize_count;
	}

	/** 返回物品 ID / Returns the item id */
	public int getItem_id() {
		return item_id;
	}

	/** 获取升级材料。 / Returns the upgrade 材料. */
	public UpgradeMaterials getUpgrade_materials() {
		return upgrade_materials;
	}

	/** 返回 need abyss point / Returns the need abyss point */
	public NeedAbyssPoint getNeed_abyss_point() {
		return need_abyss_point;
	}

	/** 返回 need kinah / Returns the need kinah */
	public NeedKinah getNeed_kinah() {
		return need_kinah;
	}
}
