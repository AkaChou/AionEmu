package com.aionemu.gameserver.model.templates.restriction;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 物品清理模板（静态数据/XML）。
 * Item Cleanup Template (static data/XML).
 *
 * @author KID
 */
@XmlRootElement(name = "cleanup")
@XmlAccessorType(XmlAccessType.NONE)
public class ItemCleanupTemplate {

	@XmlAttribute(name = "id", required = true)
	private int id;
	@XmlAttribute
	private byte trade = -1;
	@XmlAttribute
	private byte sell = -1;
	@XmlAttribute
	private byte wh = -1;
	@XmlAttribute
	private byte awh = -1;
	@XmlAttribute
	private byte lwh = -1;

	/** 结果交易。 / Result Trade. */
	public byte resultTrade() {
		return trade;
	}

	/** 结果出售。 / Result Sell. */
	public byte resultSell() {
		return sell;
	}

	/** 结果：仓库。 / Result WH. */
	public byte resultWH() {
		return wh;
	}

	/** 结果：账号仓库。 / Result Account WH. */
	public byte resultAccountWH() {
		return awh;
	}

	/** 结果军团仓库 / Result Legion WH */
	public byte resultLegionWH() {
		return lwh;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
