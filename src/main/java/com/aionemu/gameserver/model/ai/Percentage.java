package com.aionemu.gameserver.model.ai;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Percentage，用于 ai 相关逻辑。
 * Percentage for ai logic.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Percentage")
public class Percentage {

	@XmlAttribute(name = "percent")
	protected int percent;
	@XmlAttribute(name = "skillId")
	protected int skillId = 0;
	@XmlAttribute(name = "isIndividual")
	protected boolean isIndividual = false;
	@XmlElement(name = "summonGroup")
	protected List<SummonGroup> summons;

	/** 返回 summons / Returns the summons */
	public List<SummonGroup> getSummons() {
		return summons;
	}

	/** 返回 percent / Returns the percent */
	public int getPercent() {
		return percent;
	}

	/** 返回技能 ID / Returns the skill id */
	public int getSkillId() {
		return skillId;
	}

	/**
	 * @return Whether individual / Whether individual
	 */
	public boolean isIndividual() {
		return isIndividual;
	}
}
