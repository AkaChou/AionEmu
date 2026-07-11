package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.ScheduledFuture;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_IMMOBILIZE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.PositionUtil;

/**
 * 恐惧效果：强制目标逃跑/失控，可按抗性与抵抗几率被打断。
 * Fear effect: forces the target to flee/lose control; may break by resist chance.
 *
 * @author Sarynth
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FearEffect")
public class FearEffect extends EffectTemplate {

	@XmlAttribute
	protected int resistchance;

	public FearEffect() {
		resistchance = 100;
	}

	/**
	 * 移除隐身类效果后将恐惧加入目标控制器。
	 * Removes hide effects then attaches fear to the controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Creature effected = effect.getEffected();
		effected.getEffectController().removeHideEffects();
		if (effected instanceof Player && effected.isInState(CreatureState.GLIDING)) {
			((Player) effected).getFlyController().onStopGliding(true);
		}
		effect.addToEffectedController();
	}

	/**
	 * 按恐惧抗性计算是否生效。
	 * Calculates success against fear resistance.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.FEAR_RESISTANCE, null);
	}

	/**
	 * 启动恐惧：设置异常、AI 逃跑与抗性观察者。
	 * Starts fear: sets abnormal, flee AI, and resist observers.
	 */
	@Override
	public void startEffect(final Effect effect) {
		final Creature effector = effect.getEffector();
		final Creature effected = effect.getEffected();
		effected.getController().cancelCurrentSkill();
		effect.setAbnormal(AbnormalState.FEAR.getId());
		effected.getEffectController().setAbnormal(AbnormalState.FEAR.getId());
		effected.getController().stopMoving();

		if (effected instanceof Npc) {
			((NpcAI2) effected.getAi2()).setStateIfNot(AIState.FEAR);
		}
		if (GeoDataConfig.FEAR_ENABLE) {
			ScheduledFuture<?> fearTask = GameThreadPoolServices.threadPoolManager()
					.scheduleAtFixedRate(new FearTask(effector, effected), 0, 1000);
			effect.setPeriodicTask(fearTask, position);
		}

		// 恐惧效果对伤害的抵抗几率；若值低于 100，恐惧可 / resistchance of fear effect to damage, if value is lower than 100, fear can
		// 可被伤害打断 / be interrupted bz damage
		// 示例 skillId: 540 恐怖嚎叫 / example skillId: 540 Terrible howl
		if (resistchance < 100) {
			ActionObserver observer = new ActionObserver(ObserverType.ATTACKED) {

				@Override
				public void attacked(Creature creature) {
					if (Rnd.get(0, 100) > resistchance) {
						effected.getEffectController().removeEffect(effect.getSkillId());
					}
				}
			};
			effected.getObserveController().addObserver(observer);
			effect.setActionObserver(observer, position);
		}
	}

	/**
	 * 结束恐惧：恢复 AI 与异常状态。
	 * Ends fear: restores AI and clears the abnormal state.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.FEAR.getId());

		// 目前仅支持玩家 / for now we support only players
		if (GeoDataConfig.FEAR_ENABLE) {
			effect.getEffected().getMoveController().abortMove();
		}
		if (effect.getEffected() instanceof Npc) {
			((NpcAI2) effect.getEffected().getAi2()).onCreatureEvent(AIEventType.ATTACK, effect.getEffector());
		}
		PacketSendUtility.broadcastPacketAndReceive(effect.getEffected(),
				new SM_TARGET_IMMOBILIZE(effect.getEffected()));

		if (resistchance < 100) {
			ActionObserver observer = effect.getActionObserver(position);
			if (observer != null) {
				effect.getEffected().getObserveController().removeObserver(observer);
			}
		}
	}

	class FearTask implements Runnable {

		private Creature effector;
		private Creature effected;

		FearTask(Creature effector, Creature effected) {
			this.effector = effector;
			this.effected = effected;
		}

		@Override
		public void run() {
			if (effected.getEffectController().isUnderFear()) {
				float x = effected.getX();
				float y = effected.getY();
				if (!MathUtil.isNearCoordinates(effected, effector, 40)) {
					return;
				}
				byte moveAwayHeading = PositionUtil.getMoveAwayHeading(effector, effected);
				double radian = Math.toRadians(MathUtil.convertHeadingToDegree(moveAwayHeading));
				float maxDistance = effected.getGameStats().getMovementSpeedFloat();
				float x1 = (float) (Math.cos(radian) * maxDistance);
				float y1 = (float) (Math.sin(radian) * maxDistance);
				byte intentions = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId());
				Vector3f closestCollision = GameWorldServices.geoService().getClosestCollision(effected, x + x1, y + y1,
						effected.getZ(), true, intentions);
				if (effected.isFlying()) {
					closestCollision.setZ(effected.getZ());
				}
				if (effected instanceof Npc) {
					((Npc) effected).getMoveController().resetMove();
					((Npc) effected).getMoveController().moveToPoint(closestCollision.getX(), closestCollision.getY(),
							closestCollision.getZ());
				} else {
					effected.getMoveController().setNewDirection(closestCollision.getX(), closestCollision.getY(),
							closestCollision.getZ(), moveAwayHeading);
					effected.getMoveController().startMovingToDestination();
				}
			}
		}
	}
}
