package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;

/**
 * 驱散 NPC 增益效果：移除 NPC 类 Buff。
 * Dispel NPC-buff effect: removes NPC buffs.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DispelNpcBuffEffect")
public class DispelNpcBuffEffect extends AbstractDispelEffect {

	/**
	 * 按类别驱散目标效果。
	 * Dispels target effects by category.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void applyEffect(Effect effect) {
		super.applyEffect(effect, DispelCategoryType.NPC_BUFF, SkillTargetSlot.BUFF);
	}
}
