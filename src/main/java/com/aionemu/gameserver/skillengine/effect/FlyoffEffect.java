package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillMoveType;

/**
 * 击飞效果：通知客户端按技能数据执行抛射。
 * Fly-off effect: tells the client to perform the launch from skill data.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FlyOffEffect")
public class FlyoffEffect extends EffectTemplate {

	@XmlAttribute
	protected int distance;

	/**
	 * 击飞位移由客户端根据技能的 distance/value 参数执行。
	 * The client performs the displacement from the skill distance/value parameters.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
	}

	@Override
	public void calculate(Effect effect) {
		if (super.calculate(effect, null, null)) {
			effect.setSkillMoveType(SkillMoveType.FLYOFF);
		}
	}
}
