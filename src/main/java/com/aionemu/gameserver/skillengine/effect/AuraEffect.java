package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Collection;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MANTRA_EFFECT;
import com.aionemu.gameserver.services.DuelService;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 光环/真言效果：周期向范围内友方（含自身）施加关联技能。
 * Aura/mantra effect: periodically applies a linked skill to allies in range (including self).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AuraEffect")
public class AuraEffect extends EffectTemplate {
	@XmlAttribute
	protected int distance;
	@XmlAttribute(name = "distance_z")
	protected int distanceZ;
	@XmlAttribute(name = "skill_id")
	protected int skillId;

	/**
	 * 校验防滥用后将效果加入控制器。
	 * Validates against abuse then adds the effect to the controller.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		final Player effector = (Player) effect.getEffector();
		if (effector.getEffectController().isNoshowPresentBySkillId(effect.getSkillId())) {
			AuditLogger.info(effector, "Player might be abusing CM_CASTSPELL mantra effect Player kicked skill id: "
					+ effect.getSkillId());
			effector.getClientConnection().closeNow();
			return;
		}
		effect.addToEffectedController();
	}

	/**
	 * 周期动作：向范围内组队/联盟成员及自身施加光环技能。
	 * Periodic action: applies the aura skill to group/alliance members in range and self.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void onPeriodicAction(final Effect effect) {
		final Player effector = (Player) effect.getEffector();
		if (!effector.isOnline()) {
			return;
		}
		if (effector.isInGroup2() || effector.isInAlliance2()) {
			Collection<Player> onlinePlayers = effector.isInGroup2() ? effector.getPlayerGroup2().getOnlineMembers()
					: effector.getPlayerAllianceGroup2().getOnlineMembers();
			final int actualRange = (int) (distance
					* 100f);
			for (Player player : onlinePlayers) {
				if (MathUtil.isIn3dRange(effector, player, actualRange)) {
					if (!GameGameplayServices.duelService().isDueling(player.getObjectId()) && player != effector) {
						applyAuraTo(player, effect);
					}
					if (GameGameplayServices.duelService().isDueling(effector.getObjectId())
							&& GameGameplayServices.duelService().isDueling(player.getObjectId())) {
						applyAuraTo(effector, effect);
					} else {
						applyAuraTo(effector, effect);
					}
				}
			}
		} else {
			applyAuraTo(effector, effect);
		}
		PacketSendUtility.broadcastPacket(effector, new SM_MANTRA_EFFECT(effector, skillId), true);
	}

	/**
	 * 对指定玩家施加光环关联技能。
	 * Applies the aura-linked skill to the given player.
	 *
	 * target player
	 * @param effect 运行时效果 / runtime effect
	 */
	private void applyAuraTo(Player effected, Effect effect) {
		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		Effect e = new Effect(effected, effected, template, template.getLvl(), 0);
		e.initialize();
		e.applyEffect();
	}

	/**
	 * 启动周期任务（约 6.5 秒一次）。
	 * Starts the periodic task (about every 6.5 seconds).
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(final Effect effect) {
		effect.setPeriodicTask(GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new AuraTask(effect), 0, 6500),
				position);
	}

	/**
	 * 光环周期任务。
	 * Aura periodic task.
	 */
	private class AuraTask implements Runnable {
		private Effect effect;

		public AuraTask(Effect effect) {
			this.effect = effect;
		}

		@Override
		public void run() {
			onPeriodicAction(effect);
			Thread.yield();
		}
	}

	/**
	 * 结束光环效果。
	 * Ends the aura effect.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
	}
}
