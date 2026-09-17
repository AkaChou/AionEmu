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
			effected.getAi2().onCreatureEvent(AIEventType.ATTACK, effect.getEffector());
		}
		PacketSendUtility.broadcastPacketAndReceive(effected, new SM_TARGET_IMMOBILIZE(effected));
	}

    private record ConfuseTask(Creature effected) implements Runnable {

        /** 每次重选方向的最大尝试次数 / Maximum direction attempts per tick. */
        private static final int MAX_DIRECTION_ATTEMPTS = 8;

        @Override
        public void run() {
            if (!effected.getEffectController().isConfused()) {
                return;
            }
            // 平台/悬崖边缘的方向落在没有地面的空中时，客户端会拒绝该位移并把角色拉回原位，
            // 表现为“跑出去又瞬间回到起点”的反复循环。这里先做地面校验，取第一个可站立的方向。
            // A direction over a platform/cliff edge has no ground: the client rejects the move and snaps the
            // character back, looping until the effect ends. Validate the ground and take the first standable one.
            for (int attempt = 0; attempt < MAX_DIRECTION_ATTEMPTS; attempt++) {
                if (moveToRandomDirection()) {
                    return;
                }
            }
        }

        /**
         * 随机选一个方向并在目标点可站立时启动移动。
         * Picks a random direction and starts moving when its destination is standable.
         *
         * @return 已启动移动返回 true / true when a move was started
         */
        private boolean moveToRandomDirection() {
            float angle = Rnd.get() * 360f;
            double radian = Math.toRadians(angle);
            float distance = effected.getGameStats().getMovementSpeedFloat();
            float targetX = effected.getX() + (float) Math.cos(radian) * distance;
            float targetY = effected.getY() + (float) Math.sin(radian) * distance;
            byte intentions = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId());
            Vector3f destination = GameWorldServices.geoService().getClosestCollision(effected, targetX, targetY,
                    effected.getZ(), true, intentions);
            if (!GameWorldServices.pathService().hasStandableGround(effected, destination.getX(), destination.getY(),
                    destination.getZ())) {
                return false;
            }
            byte heading = MathUtil.convertDegreeToHeading(angle);
            if (effected instanceof Npc) {
                ((Npc) effected).getMoveController().resetMove();
                ((Npc) effected).getMoveController().moveToPoint(destination.getX(), destination.getY(), destination.getZ());
            } else {
                effected.getMoveController().setNewDirection(destination.getX(), destination.getY(), destination.getZ(), heading);
                effected.getMoveController().startMovingToDestination();
            }
            return true;
        }
    }
}
