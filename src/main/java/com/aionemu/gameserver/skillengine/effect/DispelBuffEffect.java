package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;

/**
 * 驱散增益效果：移除目标 Buff 槽位上的增益。
 * Dispel-buff effect: removes buffs from the target's buff slot.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DispelBuffEffect")
public class DispelBuffEffect extends AbstractDispelEffect {

	/**
	 * 按类别驱散目标效果。
	 * Dispels target effects by category.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void applyEffect(Effect effect) {
		super.applyEffect(effect, DispelCategoryType.BUFF, SkillTargetSlot.BUFF);
	}
}
