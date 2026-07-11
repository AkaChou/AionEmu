package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 重生效果：挂载后允许以配置百分比与技能进行重生。
 * Rebirth effect: while active, allows rebirth at a configured percent and skill.
 *
 * @author Sarynth
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RebirthEffect")
public class RebirthEffect extends EffectTemplate {

	@XmlAttribute(name = "resurrect_percent", required = true)
	protected int resurrectPercent;

	@XmlAttribute(name = "skill_id")
	protected int skillId;

	/**
	 * 将重生效果加入目标控制器。
	 * Attaches the rebirth effect to the controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 返回重生恢复生命百分比。
	 * Returns the rebirth HP restore percent.
	 */
	public int getResurrectPercent() {
		return resurrectPercent;
	}

	/**
	 * 返回重生关联技能 ID。
	 * Returns the rebirth-related skill id.
	 */
	public int getSkillId() {
		return skillId;
	}
}
