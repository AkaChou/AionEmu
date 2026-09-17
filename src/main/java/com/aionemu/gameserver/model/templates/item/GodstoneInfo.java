package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 神石信息模板：触发技能与概率、破损概率。
 * Godstone info template: trigger skill and probability, break probability.
 */

@Getter
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "Godstone")
public class GodstoneInfo {
	/** 返回 skillid / Returns the skillid */
	@XmlAttribute
	private int skillid;
	/** 返回 skilllvl / Returns the skilllvl */
	@XmlAttribute
	private int skilllvl;
	/** 返回 probability / Returns the probability */
	@XmlAttribute
	private int probability;
	/** 返回 probabilityleft / Returns the probabilityleft */
	@XmlAttribute
	private int probabilityleft;
	/** 返回 breakprob / Returns the breakprob */
	@XmlAttribute
	private int breakprob;
	/** 返回 breakcount / Returns the breakcount */
	@XmlAttribute
	private int breakcount;
	@XmlAttribute
	private boolean breakable = true; // 默认 true，保持原有行为 / default true to keep original behavior

	/** 返回 breakable / Returns the breakable */
	public boolean getBreakable() {
		return breakable;
	}
}
