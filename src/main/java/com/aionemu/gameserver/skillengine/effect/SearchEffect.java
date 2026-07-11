package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureSeeState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE;
import com.aionemu.gameserver.services.player.PlayerVisualStateService;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 探查/识破效果：提升目标的可见状态，用于发现隐身单位。
 * Search effect: raises the target see-state so hidden units can be revealed.
 *
 * @author Sweetkr
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchEffect")
public class SearchEffect extends EffectTemplate {

	@XmlAttribute
	protected CreatureSeeState state;

	/**
	 * 将效果加入目标的效果控制器。
	 * Adds this effect to the target effect controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 取消可见状态并广播玩家状态。
	 * Unsets see-state, revalidates invis if configured, and broadcasts player state.
	 */
	@Override
	public void endEffect(Effect effect) {
		Creature effected = effect.getEffected();

		effected.unsetSeeState(state);

		if (SecurityConfig.INVIS && effected instanceof Player) {
			PlayerVisualStateService.seeValidate((Player) effected);
		}
		PacketSendUtility.broadcastPacketAndReceive(effected, new SM_PLAYER_STATE(effected));
	}

	/**
	 * 设置可见状态并广播玩家状态；必要时做隐身校验。
	 * Sets see-state, validates invis if configured, and broadcasts player state.
	 */
	@Override
	public void startEffect(final Effect effect) {
		final Creature effected = effect.getEffected();

		effected.setSeeState(state);

		if (SecurityConfig.INVIS && effected instanceof Player) {
			PlayerVisualStateService.seeValidate((Player) effected);
		}
		PacketSendUtility.broadcastPacketAndReceive(effected, new SM_PLAYER_STATE(effected));
	}
}
