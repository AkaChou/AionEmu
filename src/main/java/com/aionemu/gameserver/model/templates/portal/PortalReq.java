package com.aionemu.gameserver.model.templates.portal;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.configs.main.GSConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * 传送门 Req 模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortalReq")
public class PortalReq {

	/** 返回 quest req / Returns the quest req */
	@Getter
	@XmlElement(name = "quest_req")
	protected List<QuestReq> questReq;
	/** 返回 item req / Returns the item req */
	@Getter
	@XmlElement(name = "item_req")
	protected List<ItemReq> itemReq;
	/** 获取最小等级。 / Returns the min level. */
	@Getter
	@Setter
	@XmlAttribute(name = "min_level")
	protected int minLevel;
	@XmlAttribute(name = "max_level")
	protected Integer maxLevel;
	/** 返回 kinah req / Returns the kinah req */
	@Getter
	@Setter
	@XmlAttribute(name = "kinah_req")
	protected int kinahReq;
	/** 返回标题 ID / Returns the title id */
	@Getter
	@XmlAttribute(name = "title_id")
	protected int titleId;
	/** 返回 err level / Returns the err level */
	@Getter
	@XmlAttribute(name = "err_level")
	protected int errLevel;

	/** 获取最大等级。 / Returns the max level. */
	public int getMaxLevel() {
		return maxLevel == null ? GSConfig.PLAYER_MAX_LEVEL : maxLevel;
	}

	/** 设置最大等级。 / Sets the max level. */
	public void setMaxLevel(int value) {
		this.maxLevel = value;
	}
}
