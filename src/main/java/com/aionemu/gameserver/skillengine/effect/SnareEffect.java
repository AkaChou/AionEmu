package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_IMMOBILIZE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 减速效果：降低目标移动能力；飞行/滑翔时强制中止移动。
 * Snare effect: slows the target; aborts movement while flying or gliding.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SnareEffect")
public class SnareEffect extends BuffEffect {
	/**
	 * 将效果加入目标的效果控制器。
	 * Adds this effect to the target effect controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 按减速抗性结算是否命中。
	 * Resolves hit chance against snare resistance.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.SNARE_RESISTANCE, null);
	}

	/**
	 * 结束 Buff 并清除 SNARE 异常。
	 * Ends the buff and clears the SNARE abnormal.
	 */
	@Override
	public void endEffect(Effect effect) {
		super.endEffect(effect);
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.SNARE.getId());
	}

	/**
	 * 应用 Buff 修饰并施加 SNARE；飞行/滑翔时广播定身并中止移动。
	 * Applies buff modifiers and SNARE; immobilizes if flying/gliding.
	 */
	@Override
	public void startEffect(Effect effect) {
		super.startEffect(effect);
		effect.getEffected().getEffectController().setAbnormal(AbnormalState.SNARE.getId());
		effect.setAbnormal(AbnormalState.SNARE.getId());
		if (effect.getEffected().isFlying() || effect.getEffected().isInState(CreatureState.GLIDING)) {
			PacketSendUtility.broadcastPacketAndReceive(effect.getEffected(),
					new SM_TARGET_IMMOBILIZE(effect.getEffected()));
			effect.getEffected().getMoveController().abortMove();
		}
	}
}
