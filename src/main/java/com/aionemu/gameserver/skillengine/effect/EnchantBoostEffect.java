package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 附魔成功率加成效果：标记运行中效果启用附魔提升。
 * Enchant boost effect: marks the runtime effect to enable enchant success increase.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EnchantBoostEffect")
public class EnchantBoostEffect extends BuffEffect {

	/**
	 * 标记本效果启用附魔加成并记为成功。
	 * Marks enchant boost and records this effect as successful.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		effect.setEnchantBoost(true);
		effect.addSucessEffect(this);
	}
}
