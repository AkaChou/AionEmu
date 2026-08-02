package com.aionemu.gameserver.services.summons;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.controllers.SummonController;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.summons.SummonMode;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_OWNER_REMOVE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_PANEL;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_PANEL_REMOVE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.spawnengine.VisibleObjectSpawner;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 召唤物服务，处理召唤创建、模式切换与解散流程。
 * Summons service handling summon creation, mode switching and release flow.
 */
public class SummonsService {

	/**
	 * 为玩家创建召唤物；若已有召唤物则提示并返回。
	 * Create a summon for the player; if one already exists, notify and return.
	 *
	 * Master player
	 * Summon NPC id
	 * Summon skill id
	 * Skill level
	 * @param time 存活时间 / Lifetime
	 */
	public static final void createSummon(Player master, int npcId, int skillId, int skillLevel, int time) {
		if (master.getSummon() != null) {
			PacketSendUtility.sendPacket(master, new SM_SYSTEM_MESSAGE(1300072, new Object[0]));
			return;
		}
		Summon summon = VisibleObjectSpawner.spawnSummon(master, npcId, skillId, skillLevel, time);
		if (summon.getAi2().getName().equals("siege_weapon")) {
			summon.getAi2().onGeneralEvent(AIEventType.SPAWNED);
		}
		master.setSummon(summon);
		PacketSendUtility.sendPacket(master, new SM_SUMMON_PANEL(summon));
		PacketSendUtility.broadcastPacket(summon, new SM_EMOTION(summon, EmotionType.START_EMOTE2));
		PacketSendUtility.broadcastPacket(summon, new SM_SUMMON_UPDATE(summon));
	}

	/**
	 * 传送开始时临时隐藏召唤物，但保留主人关系和剩余有效期。
	 * Temporarily hides a summon when teleport starts while retaining ownership and lifetime.
	 */
	public static void suspendForTeleport(Player master) {
		Summon summon = master.getSummon();
		if (summon == null) {
			return;
		}
		synchronized (summon) {
			if (summon.isSpawned()) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(summon);
			}
		}
	}

	/**
	 * 地图加载完成后恢复仍有效的召唤物，并重新同步客户端召唤面板。
	 * Restores a still-valid summon after map loading and resynchronizes its client panel.
	 */
	public static void restoreAfterTeleport(Player master) {
		Summon summon = master.getSummon();
		if (summon == null) {
			return;
		}
		synchronized (summon) {
			if (summon.getMode() == SummonMode.RELEASE) {
				return;
			}
			if (summon.isExpired()) {
				doMode(SummonMode.RELEASE, summon, UnsummonType.UNSPECIFIED);
				return;
			}
			if (!summon.isSpawned()) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(summon, master.getWorldId(),
						master.getInstanceId(), master.getX(), master.getY(), master.getZ(), master.getHeading());
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().spawn(summon);
			}
			PacketSendUtility.sendPacket(master, new SM_SUMMON_PANEL(summon));
			PacketSendUtility.sendPacket(master, new SM_SUMMON_UPDATE(summon));
		}
	}

	/** 到期并解散召唤物，与过图恢复使用同一把对象锁。 / Expires a summon under the same lock used by teleport restoration. */
	public static void expire(Summon summon) {
		if (summon == null) {
			return;
		}
		synchronized (summon) {
			doMode(SummonMode.RELEASE, summon, UnsummonType.UNSPECIFIED);
		}
	}

	/**
	 * 释放/解散召唤物，按原因发送消息并安排延迟删除任务。
	 * Release/unsummon the summon, send reason messages and schedule delayed deletion.
	 *
	 * Summon
	 * Unsummon reason
	 * @param isAttacked 主人是否处于被攻击状态 / Whether the master is under attack
	 */
	public static final void release(Summon summon, UnsummonType unsummonType, boolean isAttacked) {
		if (summon.getMode() == SummonMode.RELEASE)
			return;
		summon.getController().cancelCurrentSkill();
		summon.setMode(SummonMode.RELEASE);
		Player master = summon.getMaster();
		switch (unsummonType) {
		case COMMAND:
			PacketSendUtility.sendPacket(master,
					SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_UNSUMMON_FOLLOWER(summon.getNameId()));
			PacketSendUtility.sendPacket(master, new SM_SUMMON_UPDATE(summon));
			break;
		case DISTANCE:
			PacketSendUtility.sendPacket(master, SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_UNSUMMON_BY_TOO_DISTANCE);
			PacketSendUtility.sendPacket(master, new SM_SUMMON_UPDATE(summon));
			break;
		case UNSPECIFIED:
		case LOGOUT:
			break;
		}
		summon.getObserveController().notifySummonReleaseObservers();
		summon.setReleaseTask(GameThreadPoolServices.threadPoolManager()
				.schedule(new ReleaseSummonTask(summon, unsummonType, isAttacked), 5000));
	}

	/**
	 * 将召唤物切换为休息模式并触发恢复任务。
	 * Switch the summon to rest mode and trigger restore task.
	 *
	 * Summon
	 */
	public static final void restMode(final Summon summon) {
		summon.getController().cancelCurrentSkill();
		summon.setMode(SummonMode.REST);
		Player master = summon.getMaster();
		PacketSendUtility.sendPacket(master, SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_REST_MODE(summon.getNameId()));
		PacketSendUtility.sendPacket(master, new SM_SUMMON_UPDATE(summon));
		summon.getLifeStats().triggerRestoreTask();
	}

	/**
	 * 将召唤物切换为未知/过渡模式。
	 * Switch the summon to unknown/transitional mode.
	 *
	 * Summon
	 */
	public static final void setUnkMode(final Summon summon) {
		summon.setMode(SummonMode.UNK);
		Player master = summon.getMaster();
		PacketSendUtility.sendPacket(master, new SM_SUMMON_UPDATE(summon));
	}

	/**
	 * 将召唤物切换为守卫模式并触发恢复任务。
	 * Switch the summon to guard mode and trigger restore task.
	 *
	 * Summon
	 */
	public static final void guardMode(final Summon summon) {
		summon.getController().cancelCurrentSkill();
		summon.setMode(SummonMode.GUARD);
		Player master = summon.getMaster();
		PacketSendUtility.sendPacket(master, SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_GUARD_MODE(summon.getNameId()));
		PacketSendUtility.sendPacket(master, new SM_SUMMON_UPDATE(summon));
		summon.getLifeStats().triggerRestoreTask();
	}

	/**
	 * 将召唤物切换为攻击模式并取消恢复任务。
	 * Switch the summon to attack mode and cancel restore task.
	 *
	 * Summon
	 */
	public static final void attackMode(final Summon summon) {
		summon.setMode(SummonMode.ATTACK);
		Player master = summon.getMaster();
		PacketSendUtility.sendPacket(master, SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_ATTACK_MODE(summon.getNameId()));
		PacketSendUtility.sendPacket(master, new SM_SUMMON_UPDATE(summon));
		summon.getLifeStats().cancelRestoreTask();
	}

	/**
	 * 按模式驱动召唤物行为（无目标、无解散类型）。
	 * Drive summon behavior by mode (no target, no unsummon type).
	 *
	 * Target mode
	 * Summon
	 */
	public static final void doMode(SummonMode summonMode, Summon summon) {
		doMode(summonMode, summon, 0, null);
	}

	/**
	 * 按模式驱动召唤物行为（指定解散类型）。
	 * Drive summon behavior by mode with an unsummon type.
	 *
	 * Target mode
	 * Summon
	 * Unsummon reason
	 */
	public static final void doMode(SummonMode summonMode, Summon summon, UnsummonType unsummonType) {
		doMode(summonMode, summon, 0, unsummonType);
	}

	/**
	 * 按模式驱动召唤物行为：休息/攻击/守卫/释放等。
	 * Drive summon behavior by mode: rest/attack/guard/release, etc.
	 *
	 * Target mode
	 * Summon
	 * @param targetObjId 攻击目标对象 ID / Attack target object id
	 * @param unsummonType 解散原因（释放模式时使用） / Unsummon reason (used in release mode)
	 */
	public static final void doMode(SummonMode summonMode, Summon summon, int targetObjId, UnsummonType unsummonType) {
		if (summon.getLifeStats().isAlreadyDead()) {
			return;
		}
		if (unsummonType != null && unsummonType.equals(UnsummonType.COMMAND)
				&& !summonMode.equals(SummonMode.RELEASE)) {
			summon.cancelReleaseTask();
		}
		SummonController summonController = summon.getController();
		if (summonController == null) {
			return;
		}
		if (summon.getMaster() == null) {
			summon.getController().onDelete();
			return;
		}
		switch (summonMode) {
		case REST:
			summonController.restMode();
			break;
		case ATTACK:
			summonController.attackMode(targetObjId);
			break;
		case GUARD:
			summonController.guardMode();
			break;
		case RELEASE:
			if (unsummonType != null) {
				summonController.release(unsummonType);
			}
			break;
		case UNK:
			break;
		}
	}

	/**
	 * 延迟释放召唤物的任务：删除实体、清空主人引用，并按原因处理仇恨转移。
	 * Delayed release task: delete entity, clear master reference, and transfer hate by reason.
	 */
	public static class ReleaseSummonTask implements Runnable {
		private Summon owner;
		private UnsummonType unsummonType;
		private Player master;
		private VisibleObject target;
		private boolean isAttacked;

		/**
		 * 构造延迟释放任务。
		 * Construct a delayed release task.
		 *
		 * Summon owner
		 * Unsummon reason
		 * @param isAttacked 主人是否被攻击 / Whether the master is under attack
		 */
		public ReleaseSummonTask(Summon owner, UnsummonType unsummonType, boolean isAttacked) {
			this.owner = owner;
			this.unsummonType = unsummonType;
			master = owner.getMaster();
			target = master.getTarget();
			this.isAttacked = isAttacked;
		}

		/**
		 * 执行释放：删除召唤物并同步面板；必要时延迟把仇恨转给主人。
		 * Execute release: delete summon and sync panel; optionally delay-transfer hate to the master.
		 */
		@Override
		public void run() {
			owner.getController().delete();
			owner.setMaster(null);
			master.setSummon(null);
			switch (unsummonType) {
			case COMMAND:
			case DISTANCE:
			case UNSPECIFIED:
				PacketSendUtility.sendPacket(master, SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_UNSUMMONED(owner.getNameId()));
				PacketSendUtility.sendPacket(master, new SM_SUMMON_OWNER_REMOVE(owner.getObjectId()));
				PacketSendUtility.sendPacket(master, new SM_SUMMON_PANEL_REMOVE());
				if (target instanceof Creature) {
					final Creature lastAttacker = (Creature) target;
					if (!master.getLifeStats().isAlreadyDead() && !lastAttacker.getLifeStats().isAlreadyDead()
							&& isAttacked) {
						GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							@Override
							public void run() {
								lastAttacker.getAggroList().addHate(master, 1);
							}
						}, 1000);
					}
				}
				break;
			case LOGOUT:
				break;
			}
		}
	}
}
