package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 物品 PurchableLimits 模板（静态数据/XML）。
 * XML template.
 *
 * @author Ranastic (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Purchable")
public class ItemPurchableLimits {
	@XmlAttribute(name = "rank_min")
	private int minRank;

	/** 返回最小军阶 / Returns the min rank*/
	public int getMinRank() {
		return minRank;
	}

	/** 校验军阶 / Verify Rank */
	public boolean verifyRank(int rank) {
		return minRank <= rank;
	}
}
