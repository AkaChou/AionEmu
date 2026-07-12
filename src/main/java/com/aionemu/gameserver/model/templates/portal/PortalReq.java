package com.aionemu.gameserver.model.templates.portal;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.configs.main.GSConfig;

/**
 * 传送门 Req 模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortalReq")
public class PortalReq {

	@XmlElement(name = "quest_req")
	protected List<QuestReq> questReq;
	@XmlElement(name = "item_req")
	protected List<ItemReq> itemReq;
	@XmlAttribute(name = "min_level")
	protected int minLevel;
	@XmlAttribute(name = "max_level")
	protected Integer maxLevel;
	@XmlAttribute(name = "kinah_req")
	protected int kinahReq;
	@XmlAttribute(name = "title_id")
	protected int titleId;
	@XmlAttribute(name = "err_level")
	protected int errLevel;

	/** 返回 quest req / Returns the quest req */
	public List<QuestReq> getQuestReq() {
		return this.questReq;
	}

	/** 返回 item req / Returns the item req */
	public List<ItemReq> getItemReq() {
		return this.itemReq;
	}

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		return minLevel;
	}

	/** 设置最小等级。 / Sets the min level. */
	public void setMinLevel(int value) {
		this.minLevel = value;
	}

	/** 获取最大等级。 / Returns the max level. */
	public int getMaxLevel() {
		return maxLevel == null ? GSConfig.PLAYER_MAX_LEVEL : maxLevel;
	}

	/** 设置最大等级。 / Sets the max level. */
	public void setMaxLevel(int value) {
		this.maxLevel = value;
	}

	/** 返回 kinah req / Returns the kinah req */
	public int getKinahReq() {
		return kinahReq;
	}

	/** 设置 kinah req / Sets the kinah req */
	public void setKinahReq(int value) {
		this.kinahReq = value;
	}

	/** 返回标题 ID / Returns the title id */
	public int getTitleId() {
		return titleId;
	}

	/** 返回 err level / Returns the err level */
	public int getErrLevel() {
		return errLevel;
	}
}
