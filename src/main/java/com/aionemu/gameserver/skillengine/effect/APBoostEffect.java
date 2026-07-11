package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 欧比斯点数加成效果：标记运行中效果启用 AP 提升。
 * Abyss Point boost effect: marks the runtime effect to enable AP gain increase.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "APBoostEffect")
public class APBoostEffect extends BuffEffect {

	/**
	 * 标记本效果启用 AP 加成并记为成功。
	 * Marks AP boost and records this effect as successful.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		effect.setApBoost(true);
		effect.addSucessEffect(this);
	}
}
