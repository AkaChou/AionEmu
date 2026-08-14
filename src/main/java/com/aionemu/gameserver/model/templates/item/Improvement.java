package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 物品改进（强化）模板：充能方式、价格与燃烧加成。
 * Item improvement template: charge way, prices and burn bonuses.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Improvement")
public class Improvement {
	@XmlAttribute(name = "way", required = true)
	private int way;

	@XmlAttribute(name = "price2")
	private int price2;

	@XmlAttribute(name = "price1")
	private int price1;

	@XmlAttribute(name = "burn_defend")
	private int burnDefend;

	@XmlAttribute(name = "burn_attack")
	private int burnAttack;

	@XmlAttribute(name = "level")
	private int level;

	@XmlAttribute(name = "recommend_rank")
	private int recommend_rank;

	/**
	 * @return 强化等级 / the level
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * @return 充能方式 / the charge way
	 */
	public int getChargeWay() {
		return way;
	}

	/**
	 * @return 价格 1 / the price1
	 */
	public int getPrice1() {
		return price1;
	}

	/**
	 * @return 价格 2 / the price2
	 */
	public int getPrice2() {
		return price2;
	}

	/** 返回 burn attack / Returns the burn attack */
	public int getBurnAttack() {
		return burnAttack;
	}

	/** 返回 burn defend / Returns the burn defend */
	public int getBurnDefend() {
		return burnDefend;
	}

	/**
	 * @return 推荐军阶 / the recommend rank
	 */
	public int getRecomendRank() {
		return recommend_rank;
	}

	/** 校验军阶是否达标 / Verify recommend rank */
	public boolean verifyRecomendRank(int rank) {
		return recommend_rank <= rank;
	}
}
