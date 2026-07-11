package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 附魔选项加成效果：标记运行中效果启用附魔选项提升。
 * Enchant-option boost effect: marks the runtime effect to enable enchant-option increase.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EnchantOptionBoostEffect")
public class EnchantOptionBoostEffect extends BuffEffect {

	/**
	 * 标记本效果启用附魔选项加成并记为成功。
	 * Marks enchant-option boost and records this effect as successful.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		effect.setEnchantOptionBoost(true);
		effect.addSucessEffect(this);
	}
}
