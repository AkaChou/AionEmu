package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_IMMOBILIZE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import java.util.concurrent.ScheduledFuture;

/**
 * 混乱效果：使目标失去自主控制并随机移动。
 * Confuse effect: removes target control and forces random movement.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConfuseEffect")
public class ConfuseEffect extends EffectTemplate {

	/**
	 * 移除隐身并将混乱加入目标效果控制器。
	 * Removes hide effects and attaches confuse to the target controller.
	 *
	 * @param effect 运行时效果 / runtime effect
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
	 * 按混乱抗性计算是否命中。
	 * Calculates hit using confuse resistance.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.CONFUSE_RESISTANCE, null);
	}

	/**
	 * 启动混乱并周期性选择随机移动方向。
	 * Starts confuse and periodically chooses a random movement direction.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(Effect effect) {
		Creature effected = effect.getEffected();
		effected.getController().cancelCurrentSkill();
		effect.setAbnormal(AbnormalState.CONFUSE.getId());
		effected.getEffectController().setAbnormal(AbnormalState.CONFUSE.getId());
		effected.getController().stopMoving();

		if (effected instanceof Npc) {
			((NpcAI2) effected.getAi2()).setStateIfNot(AIState.FEAR);
		}
		if (GeoDataConfig.FEAR_ENABLE) {
			ScheduledFuture<?> task = GameThreadPoolServices.threadPoolManager()
					.scheduleAtFixedRate(new ConfuseTask(effected), 0, 1000);
			effect.setPeriodicTask(task, position);
		}
	}

	@Override
	public void endEffect(Effect effect) {
		Creature effected = effect.getEffected();
		effected.getEffectController().unsetAbnormal(AbnormalState.CONFUSE.getId());
		effected.getMoveController().abortMove();
		if (effected instanceof Npc) {
			((NpcAI2) effected.getAi2()).onCreatureEvent(AIEventType.ATTACK, effect.getEffector());
		}
		PacketSendUtility.broadcastPacketAndReceive(effected, new SM_TARGET_IMMOBILIZE(effected));
	}

	private static class ConfuseTask implements Runnable {
		private final Creature effected;

		private ConfuseTask(Creature effected) {
			this.effected = effected;
		}

		@Override
		public void run() {
			if (!effected.getEffectController().isConfused()) {
				return;
			}
			float angle = Rnd.get() * 360f;
			double radian = Math.toRadians(angle);
			float distance = effected.getGameStats().getMovementSpeedFloat();
			float targetX = effected.getX() + (float) Math.cos(radian) * distance;
			float targetY = effected.getY() + (float) Math.sin(radian) * distance;
			byte intentions = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId());
			Vector3f destination = GameWorldServices.geoService().getClosestCollision(effected, targetX, targetY,
					effected.getZ(), true, intentions);
			byte heading = MathUtil.convertDegreeToHeading(angle);
			if (effected instanceof Npc) {
				((Npc) effected).getMoveController().resetMove();
				((Npc) effected).getMoveController().moveToPoint(destination.getX(), destination.getY(), destination.getZ());
			} else {
				effected.getMoveController().setNewDirection(destination.getX(), destination.getY(), destination.getZ(), heading);
				effected.getMoveController().startMovingToDestination();
			}
		}
	}
}
