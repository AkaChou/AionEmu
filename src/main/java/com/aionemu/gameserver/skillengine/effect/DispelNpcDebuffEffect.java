package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;

/**
 * 驱散 NPC 物理减益效果：按 NPC_DEBUFF_PHYSICAL 类别清除 DEBUFF 槽位效果。
 * Dispel NPC physical debuff effect: removes DEBUFF-slot effects in NPC_DEBUFF_PHYSICAL category.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DispelNpcDebuffEffect")
public class DispelNpcDebuffEffect extends AbstractDispelEffect {

	/**
	 * 驱散目标身上的 NPC 物理减益。
	 * Dispels NPC physical debuffs on the target.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void applyEffect(Effect effect) {
		super.applyEffect(effect, DispelCategoryType.NPC_DEBUFF_PHYSICAL, SkillTargetSlot.DEBUFF);
	}
}
