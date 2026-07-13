package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 掉落稀有度加成效果：标记运行中效果启用 DR 提升。
 * Drop-rarity boost effect: marks the runtime effect to enable DR boost.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DRBoostEffect")
public class DRBoostEffect extends BuffEffect {

	@XmlAttribute
	protected int minlevel;
	@XmlAttribute
	protected int maxlevel;

	/**
	 * 标记本效果启用 DR 加成并记为成功。
	 * Marks DR boost and records this effect as successful.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		int level = effect.getEffected().getLevel();
		if (minlevel > 0 && level < minlevel || maxlevel > 0 && level > maxlevel) {
			return;
		}
		effect.setDrBoost(true);
		effect.addSucessEffect(this);
	}
}
