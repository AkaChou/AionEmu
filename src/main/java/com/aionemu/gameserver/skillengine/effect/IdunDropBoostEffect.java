package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 伊登掉落加成效果：标记运行中效果启用 Idun 掉落提升。
 * Idun drop boost effect: marks the runtime effect to enable Idun drop increase.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdunDropBoostEffect")
public class IdunDropBoostEffect extends BuffEffect {

	/**
	 * 标记本效果启用 Idun 掉落加成并记为成功。
	 * Marks Idun drop boost and records this effect as successful.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		effect.setIdunDropBoost(true);
		effect.addSucessEffect(this);
	}
}
