package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 掉落率加成效果：标记运行中效果启用掉落率（BDR）提升。
 * Drop-rate boost effect: marks the runtime effect to enable boost drop rate (BDR).
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BoostDropRateEffect")
public class BoostDropRateEffect extends BuffEffect {

	/**
	 * 标记本效果启用掉落率加成并记为成功。
	 * Marks drop-rate boost and records this effect as successful.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		effect.setBdrBoost(true);
		effect.addSucessEffect(this);
	}
}
