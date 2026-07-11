package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;

/**
 * 驱散物理减益效果：移除物理类 Debuff。
 * Dispel physical-debuff effect: removes physical debuffs.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DispelDebuffPhysicalEffect")
public class DispelDebuffPhysicalEffect extends AbstractDispelEffect {

	/**
	 * 按类别驱散目标效果。
	 * Dispels target effects by category.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void applyEffect(Effect effect) {
		super.applyEffect(effect, DispelCategoryType.DEBUFF_PHYSICAL, SkillTargetSlot.DEBUFF);
	}
}
