package com.aionemu.gameserver.model.templates.npcskill;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * NPC 技能模板（静态数据/XML）。
 * XML template.
 *
 * @author AionChs Master, nrg
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "npcskill")
public class NpcSkillTemplate {

	@XmlAttribute(name = "id")
	protected int id;
	@XmlAttribute(name = "skillid")
	protected int skillid;
	@XmlAttribute(name = "skilllevel")
	protected int skilllevel;
	@XmlAttribute(name = "probability")
	protected int probability;
	@XmlAttribute(name = "minhp")
	protected int minhp = 0;
	@XmlAttribute(name = "maxhp")
	protected int maxhp = 0;
	@XmlAttribute(name = "maxtime")
	protected int maxtime = 0;
	@XmlAttribute(name = "mintime")
	protected int mintime = 0;
	@XmlAttribute(name = "conjunction")
	protected ConjunctionType conjunction = ConjunctionType.AND;
	@XmlAttribute(name = "cooldown")
	protected int cooldown = 0;
	@XmlAttribute(name = "useinspawned")
	protected boolean useinspawned = false;

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the skillid
	 */
	public int getSkillid() {
		return skillid;
	}

	/**
	 * @return the skilllevel
	 */
	public int getSkillLevel() {
		return skilllevel;
	}

	/**
	 * @return the probability
	 */
	public int getProbability() {
		return probability;
	}

	/**
	 * @return the minhp
	 */
	public int getMinhp() {
		return minhp;
	}

	/**
	 * @return the maxhp
	 */
	public int getMaxhp() {
		return maxhp;
	}

	/**
	 * @return the mintime
	 */
	public int getMinTime() {
		return mintime;
	}

	/**
	 * @return the maxtime
	 */
	public int getMaxTime() {
		return maxtime;
	}

	 /**
	  * 获取 conjunction 属性值。
	  * Gets the value of the conjunction property
	  * @return possible object is {@link ConjunctionType }
	  */
	public ConjunctionType getConjunctionType() {
		return conjunction;
	}

	/**
	 * @return the cooldown
	 */
	public int getCooldown() {
		return cooldown;
	}

	/**
	 * @return the useinspawned
	 */
	public boolean getUseInSpawned() {
		return useinspawned;
	}
}
