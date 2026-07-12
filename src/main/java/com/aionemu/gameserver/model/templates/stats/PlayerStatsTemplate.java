package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 玩家属性模板（静态数据/XML）。
 * XML template.
 *
 * @author Luno
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "player_stats_template")
public class PlayerStatsTemplate extends StatsTemplate {

	@XmlAttribute(name = "power")
	private int power;
	@XmlAttribute(name = "health")
	private int health;
	@XmlAttribute(name = "agility")
	private int agility;
	@XmlAttribute(name = "accuracy")
	private int accuracy;
	@XmlAttribute(name = "knowledge")
	private int knowledge;
	@XmlAttribute(name = "will")
	private int will;

	/** 返回 power / Returns the power */
	public int getPower() {
		return power;
	}

	/** 返回 health / Returns the health */
	public int getHealth() {
		return health;
	}

	/** 返回 agility / Returns the agility */
	public int getAgility() {
		return agility;
	}

	/** 返回 accuracy / Returns the accuracy */
	public int getAccuracy() {
		return accuracy;
	}

	/** 返回 knowledge / Returns the knowledge */
	public int getKnowledge() {
		return knowledge;
	}

	/** 返回 will / Returns the will */
	public int getWill() {
		return will;
	}
}
