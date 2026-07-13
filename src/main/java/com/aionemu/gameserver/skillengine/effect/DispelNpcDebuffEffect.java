package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;

/**
 * 驱散 NPC 减益效果：清除 NPC 物理或精神减益。
 * Dispel NPC debuff effect: removes physical or mental NPC debuffs.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DispelNpcDebuffEffect")
public class DispelNpcDebuffEffect extends AbstractDispelEffect {

	/**
	 * 驱散目标身上的 NPC 减益。
	 * Dispels NPC debuffs on the target.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void applyEffect(Effect effect) {
		super.applyEffect(effect, DispelCategoryType.NPC_DEBUFF, SkillTargetSlot.DEBUFF);
	}
}
