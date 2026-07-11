package com.aionemu.gameserver.model.ai;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Bomb 模板，用于 ai 相关逻辑。
 * Bomb Template for ai logic.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BombTemplate")
public class BombTemplate {
	@XmlAttribute(name = "skillId")
	private int SkillId = 0;
	@XmlAttribute(name = "cd")
	private int cd = 0;

	/** 返回 cd / Returns the cd */
	public int getCd() {
		return this.cd;
	}

	/** 返回技能 ID / Returns the skill id */
	public int getSkillId() {
		return this.SkillId;
	}
}
