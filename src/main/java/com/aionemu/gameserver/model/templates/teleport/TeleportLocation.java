package com.aionemu.gameserver.model.templates.teleport;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 传送位置模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlRootElement(name = "telelocation")
@XmlAccessorType(XmlAccessType.FIELD)
public class TeleportLocation {

	/** 返回 loc id / Returns the loc id */
	@XmlAttribute(name = "loc_id", required = true)
	private int locId;

	@XmlAttribute(name = "teleportid")
	private int teleportid = 0;

	/** 获取价格。 / Returns the price. */
	@XmlAttribute(name = "price", required = true)
	private int price = 0;

	/** 返回 PvP 价格 / Returns the pvp price */
	@XmlAttribute(name = "pricePvp")
	private int pricePvp = 0;

	/** 返回所需任务 / Returns the required quest */
	@XmlAttribute(name = "requiredQuest")
	private int requiredQuest = 0;

	/** 返回允许使用传送的最低任务步骤；0 表示必须完成任务。 / Returns the minimum active quest step; 0 requires completion. */
	@XmlAttribute(name = "requiredQuestStep")
	private int requiredQuestStep = 0;

	/** 获取类型。 / Returns the type. */
	@XmlAttribute(name = "type", required = true)
	private TeleportType type;

	/** 返回传送 ID / Returns the teleport id */
	public int getTeleportId() {
		return teleportid;
	}
}
