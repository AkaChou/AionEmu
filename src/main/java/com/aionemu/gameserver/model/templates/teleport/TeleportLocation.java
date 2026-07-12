package com.aionemu.gameserver.model.templates.teleport;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 传送位置模板（静态数据/XML）。
 * XML template.
 */

@XmlRootElement(name = "telelocation")
@XmlAccessorType(XmlAccessType.FIELD)
public class TeleportLocation {

	@XmlAttribute(name = "loc_id", required = true)
	private int locId;

	@XmlAttribute(name = "teleportid")
	private int teleportid = 0;

	@XmlAttribute(name = "price", required = true)
	private int price = 0;

	@XmlAttribute(name = "pricePvp")
	private int pricePvp = 0;

	@XmlAttribute(name = "requiredQuest")
	private int requiredQuest = 0;

	@XmlAttribute(name = "type", required = true)
	private TeleportType type;

	/** 返回 loc id / Returns the loc id */
	public int getLocId() {
		return locId;
	}

	/** 返回传送 ID / Returns the teleport id */
	public int getTeleportId() {
		return teleportid;
	}

	/** 获取价格。 / Returns the price. */
	public int getPrice() {
		return price;
	}

	/** 返回 price pvp / Returns the price pvp */
	public int getPricePvp() {
		return pricePvp;
	}

	/** 返回 required quest / Returns the required quest */
	public int getRequiredQuest() {
		return requiredQuest;
	}

	/** 获取类型。 / Returns the type. */
	public TeleportType getType() {
		return type;
	}
}
