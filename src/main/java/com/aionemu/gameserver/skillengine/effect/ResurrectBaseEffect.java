package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_SPAWN;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 基础复活挂载效果：死亡时按基础复活逻辑处理并清理状态。
 * Base resurrect attach effect: handles death via base resurrect logic and cleanup.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResurrectBaseEffect")
public class ResurrectBaseEffect extends ResurrectEffect {
	/**
	 * 计算本效果是否成功命中/生效，并写入效果上下文。
	 * Calculates whether this effect succeeds and writes into the effect context.
	 */
	@Override
	public void calculate(Effect effect) {
		calculate(effect, null, null);
	}

	/**
	 * 将效果应用到目标（加入控制器或立即结算）。
	 * Applies the effect to the target (controller attach or immediate settlement).
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 挂载死亡观察以触发基础复活。
	 * Attaches death observer to trigger base resurrect.
	 */
	@Override
	public void startEffect(final Effect effect) {
		final Creature effected = effect.getEffected();
		if (effected instanceof Player) {
			ActionObserver observer = new ActionObserver(ObserverType.DEATH) {
				@Override
				public void died(Creature creature) {
					Player effected = (Player) effect.getEffected();
					if (effected.isInInstance()) {
						PlayerReviveService.instanceRevive(effected, skillId);
					} else if (effected.getKisk() != null) {
						PlayerReviveService.kiskRevive(effected, skillId);
					} else {
						PlayerReviveService.bindRevive(effected, skillId);
					}
					PacketSendUtility.broadcastPacket(effected, new SM_EMOTION(effected, EmotionType.RESURRECT), true);
					PacketSendUtility.sendPacket(effected, new SM_PLAYER_SPAWN(effected));
				}
			};
			effect.getEffected().getObserveController().attach(observer);
			effect.setActionObserver(observer, position);
		}
	}

	/**
	 * 清理基础复活相关状态。
	 * Cleans up base-resurrect related state.
	 */
	@Override
	public void endEffect(Effect effect) {
	}
}
