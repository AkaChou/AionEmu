package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 经验加成效果：标记运行中效果启用 XP 提升。
 * XP boost effect: marks the runtime effect to enable XP gain increase.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "XPBoostEffect")
public class XPBoostEffect extends BuffEffect {
	@Override
	public void calculate(Effect effect) {
		effect.setXpBoost(true);
		effect.addSucessEffect(this);
	}
}
