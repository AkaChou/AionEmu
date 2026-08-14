package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 物品购买限制模板：最低军阶要求。
 * Item purchasable limits template: minimum rank requirement.
 *
 * @author Ranastic (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Purchable")
public class ItemPurchableLimits {
	@XmlAttribute(name = "rank_min")
	private int minRank;

	/** 返回最小军阶 / Returns the min rank */
	public int getMinRank() {
		return minRank;
	}

	/** 校验军阶是否达标 / Verify rank */
	public boolean verifyRank(int rank) {
		return minRank <= rank;
	}
}
