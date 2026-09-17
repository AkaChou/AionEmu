package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 玩家属性模板（静态数据/XML）。
 * XML template.
 *
 * @author Luno
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "player_stats_template")
public class PlayerStatsTemplate extends StatsTemplate {

	/** 返回 power / Returns the power */
	@XmlAttribute(name = "power")
	private int power;
	/** 返回 health / Returns the health */
	@XmlAttribute(name = "health")
	private int health;
	/** 返回 agility / Returns the agility */
	@XmlAttribute(name = "agility")
	private int agility;
	/** 返回 accuracy / Returns the accuracy */
	@XmlAttribute(name = "accuracy")
	private int accuracy;
	/** 返回 knowledge / Returns the knowledge */
	@XmlAttribute(name = "knowledge")
	private int knowledge;
	/** 返回 will / Returns the will */
	@XmlAttribute(name = "will")
	private int will;
}
