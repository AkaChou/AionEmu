package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillMoveType;
import com.aionemu.gameserver.skillengine.model.SpellStatus;

/**
 * 回避/闪身效果：移动到目标身后并可能解除浮空；飞行时落地。
 * Evade effect: moves behind the target, may close aerial, and grounds flying casters.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EvadeEffect")
public class EvadeEffect extends DispelEffect {
	/**
	 * 设置后移类型，按状态计算；飞行中则取消飞行。
	 * Sets move-behind type, calculates by state; cancels flight if flying.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		effect.setSkillMoveType(SkillMoveType.MOVEBEHIND);
		if (effect.getEffected().getState() == 3) {
			super.calculate(effect, null, null);
		} else {
			super.calculate(effect, null, SpellStatus.CLOSEAERIAL);
		}
		Player player = (Player) effect.getEffector();
		if (player.isFlying()) {
			player.setFlyState(0);
		}
	}
}
