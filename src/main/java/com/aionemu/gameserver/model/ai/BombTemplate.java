package com.aionemu.gameserver.model.ai;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 炸弹模板：定义自爆技能的 ID 与冷却时间。
 * Bomb template: defines the self-destruct skill id and cooldown.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BombTemplate")
public class BombTemplate {
	/**
	 * 返回技能 ID / Returns the skill id
	 * <p>
	 * 注意：该字段由 JAXB 反射赋值，不能声明为 final，否则在新版 JDK 上会触发
	 * final field mutation 警告（未来版本将直接拒绝）。
	 * Note: assigned reflectively by JAXB; must not be final, otherwise newer JDKs
	 * warn about (and will eventually block) final field mutation.
	 * </p>
	 */
	@Getter
	@XmlAttribute(name = "skillId")
	private int SkillId = 0;
	/** 返回 cd / Returns the cd */
	@Getter
	@XmlAttribute(name = "cd")
	private int cd = 0;
}
