package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 神石信息模板：触发技能与概率、破损概率。
 * Godstone info template: trigger skill and probability, break probability.
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "Godstone")
public class GodstoneInfo {
	@XmlAttribute
	private int skillid;
	@XmlAttribute
	private int skilllvl;
	@XmlAttribute
	private int probability;
	@XmlAttribute
	private int probabilityleft;
	@XmlAttribute
	private int breakprob;
	@XmlAttribute
	private int breakcount;
	@XmlAttribute
	private boolean breakable = true; // 默认 true，保持原有行为 / default true to keep original behavior

	/** 返回 breakable / Returns the breakable */
	public boolean getBreakable() {
		return breakable;
	}

	/** 返回 skillid / Returns the skillid */
	public int getSkillid() {
		return skillid;
	}

	/** 返回 skilllvl / Returns the skilllvl */
	public int getSkilllvl() {
		return skilllvl;
	}

	/** 返回 probability / Returns the probability */
	public int getProbability() {
		return probability;
	}

	/** 返回 probabilityleft / Returns the probabilityleft */
	public int getProbabilityleft() {
		return probabilityleft;
	}

	/** 返回 breakprob / Returns the breakprob */
	public int getBreakprob() {
		return breakprob;
	}

	/** 返回 breakcount / Returns the breakcount */
	public int getBreakcount() {
		return breakcount;
	}
}
