package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 外形变换效果：在变身基础上可对 NPC 保持中立，并移除隐身。
 * Shape-change effect: transform that may mark the player neutral to NPCs and strips hide.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShapeChangeEffect")
public class ShapeChangeEffect extends TransformEffect {

	/**
	 * 可选设置对 NPC 中立，移除施法者隐身，再执行变身开始逻辑。
	 * Optionally sets admin-neutral, removes hide on the effector, then starts the transform.
	 */
	@Override
	public void startEffect(Effect effect) {
		if ((effect.getEffector() instanceof Player)) {
			if (effect.getEffector().getEffectController().isAbnormalSet(AbnormalState.HIDE)) {
				effect.getEffector().getEffectController().removeHideEffects();
			}
		}
		super.startEffect(effect, null);
	}

	/**
	 * 取消对 NPC 中立并结束变身。
	 * Clears admin-neutral and ends the transform.
	 */
	@Override
	public void endEffect(Effect effect) {
		super.endEffect(effect, null);
	}
}
