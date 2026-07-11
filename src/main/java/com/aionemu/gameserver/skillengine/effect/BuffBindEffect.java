package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 增益定身效果：无抗性检定的定身，直接标记成功。
 * Buff-bind effect: bind without resistance check; always succeeds.
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BuffBindEffect")
public class BuffBindEffect extends BindEffect {

	/**
	 * 跳过抗性直接标记效果成功。
	 * Marks the effect successful without a resistance check.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		effect.addSucessEffect(this);
	}
}
