package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 变形效果：改变目标外观/形态，可配置对系统中立。
 * Polymorph effect: transforms the target's form; optional system neutrality.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PolymorphEffect")
public class PolymorphEffect extends TransformEffect {

	@XmlAttribute(name = "neutral_to_npc")
	private boolean neutralToNpc = false;

	/**
	 * 开始变形并应用形态变更。
	 * Starts polymorph and applies the form change.
	 */
	@Override
	public void startEffect(Effect effect) {
		if (neutralToNpc && effect.getEffected() instanceof Player) {
			((Player) effect.getEffected()).setAdminNeutral(1);
		}
		if ((effect.getEffector() instanceof Player)) {
			if (effect.getEffector().getEffectController().isAbnormalSet(AbnormalState.HIDE)) {
				effect.getEffector().getEffectController().removeHideEffects();
			}
		}
		super.startEffect(effect, AbnormalState.NOFLY);
	}

	/**
	 * 结束变形并恢复原形态。
	 * Ends polymorph and restores the original form.
	 */
	@Override
	public void endEffect(Effect effect) {
		if (neutralToNpc && effect.getEffected() instanceof Player) {
			((Player) effect.getEffected()).setAdminNeutral(0);
		}
		super.endEffect(effect, AbnormalState.NOFLY);
		effect.getEffected().getTransformModel().setActive(false);
	}
}
