package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 物品改进（强化）模板：充能方式、价格与燃烧加成。
 * Item improvement template: charge way, prices and burn bonuses.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Improvement")
public class Improvement {
	@XmlAttribute(name = "way", required = true)
	private int way;

	/**
	 * @return 价格 2 / the price2
	 */
	@XmlAttribute(name = "price2")
	private int price2;

	/**
	 * @return 价格 1 / the price1
	 */
	@XmlAttribute(name = "price1")
	private int price1;

	/** 返回 burn defend / Returns the burn defend */
	@XmlAttribute(name = "burn_defend")
	private int burnDefend;

	/** 返回 burn attack / Returns the burn attack */
	@XmlAttribute(name = "burn_attack")
	private int burnAttack;

	/**
	 * @return 强化等级 / the level
	 */
	@XmlAttribute(name = "level")
	private int level;

	@XmlAttribute(name = "recommend_rank")
	private int recommend_rank;

	/**
	 * @return 充能方式 / the charge way
	 */
	public int getChargeWay() {
		return way;
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
