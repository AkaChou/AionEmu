package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.lifecycle.GameMovementLoopServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.summons.SummonMode;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_UPDATE;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.summons.SummonsService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 召唤物控制器，管理模式切换、攻击、技能与与主人的距离解除。
 * Summon controller managing mode switches, attacks, skills and distance-based release.
 *
 * @author ATracer
 * @author RotO (Attack-speed hack protection) modified by Sippolo
 */
public class SummonController extends CreatureController<Summon> {

	/** 上次攻击时间戳（毫秒），用于攻速校验。 / Last attack timestamp in ms, used for attack-speed checks. */
	private long lastAttackMilis = 0;
	/** 是否曾被攻击。 / Whether the summon has been attacked. */
	private boolean isAttacked = false;
	/** 使用该技能成功后自动解除召唤；-1 表示不自动解除。 / Skill id after which the summon auto-releases; -1 means none. */
	private int releaseAfterSkill = -1;

	/**
	 * 主人离开可视范围时按距离解除召唤。
	 * Releases the summon by distance when the master leaves visibility range.
	 *
	 * @param object 离开视野的对象 / the object leaving sight
	 * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
	 */
	@Override
	public void notSee(VisibleObject object, boolean isOutOfRange) {
		super.notSee(object, isOutOfRange);
		if (getOwner().getMaster() == null) {
			return;
		}
		if (object.getObjectId() == getOwner().getMaster().getObjectId()) {
			SummonsService.release(getOwner(), UnsummonType.DISTANCE, isAttacked);
		}
	}

	/**
	 * 按指定类型解除召唤。
	 * Releases the summon with the given unsummon type.
	 *
	 * @param unsummonType 解除召唤类型 / unsummon type
	 */
	public void release(final UnsummonType unsummonType) {
		SummonsService.release(getOwner(), unsummonType, isAttacked);
	}

	/**
	 * 获取所有者召唤物。
	 * Gets the owner summon.
	 *
	 * summon
	 */
	@Override
	public Summon getOwner() {
		return (Summon) super.getOwner();
	}

	/**
	 * 切换到休息模式。
	 * Switches to rest mode.
	 */
	public void restMode() {
		SummonsService.restMode(getOwner());
	}

	/**
	 * 切换到未知/默认模式。
	 * Switches to the unknown/default mode.
	 */
	public void setUnkMode() {
		SummonsService.setUnkMode(getOwner());
	}

	/**
	 * 切换到守卫模式。
	 * Switches to guard mode.
	 */
	public void guardMode() {
		SummonsService.guardMode(getOwner());
	}

	/**
	 * 切换到攻击模式（目标须为生物）。
	 * Switches to attack mode (target must be a creature).
	 *
	 * target object id
	 */
	public void attackMode(int targetObjId) {
		VisibleObject obj = getOwner().getKnownList().getObject(targetObjId);
		if (obj != null && obj instanceof Creature) {
			SummonsService.attackMode(getOwner());
		}
	}

	/**
	 * 攻击目标，含攻速反作弊校验。
	 * Attacks a target with attack-speed anti-cheat checks.
	 *
	 * attack target
	 * @param time 攻击时间参数 / attack timing parameter
	 */
	@Override
	public void attackTarget(Creature target, int time) {

		Player master = getOwner().getMaster();

		if (!RestrictionsManager.canAttack(master, target)) {
			return;
		}

		int attackSpeed = getOwner().getGameStats().getAttackSpeed().getCurrent();

		long milis = System.currentTimeMillis();

		if (milis - lastAttackMilis + 300 < attackSpeed) {
			/**
	 * 权宜处理 / Hack!
	 */
			return;
		}
		lastAttackMilis = milis;

		super.attackTarget(target, time);
	}

	/**
	 * 受到攻击时广播伤害并更新主人面板。
	 * On being attacked, broadcasts damage and updates the master's panel.
	 *
	 * attacker
	 * skill id
	 * @param type 伤害类型 / damage type
	 * damage amount
	 * @param notifyAttack 是否通知攻击 / whether to notify attack
	 * @param log 伤害日志类型 / damage log type
	 */
	@Override
	public void onAttack(Creature creature, int skillId, TYPE type, int damage, boolean notifyAttack, LOG log) {
		if (getOwner().getLifeStats().isAlreadyDead()) {
			return;
		}

		// 临时 / temp
		if (getOwner().getMode() == SummonMode.RELEASE) {
			return;
		}

		super.onAttack(creature, skillId, type, damage, notifyAttack, log);
		PacketSendUtility.broadcastPacket(getOwner(), new SM_ATTACK_STATUS(getOwner(), creature, TYPE.REGULAR, 0, damage, log));
		PacketSendUtility.sendPacket(getOwner().getMaster(), new SM_SUMMON_UPDATE(getOwner()));
	}

	/**
	 * 召唤物死亡时解除召唤，并可能将仇恨转给主人。
	 * On summon death, releases the summon and may transfer hate to the master.
	 *
	 * @param lastAttacker 最后攻击者 / last attacker
	 */
	@Override
	public void onDie(final Creature lastAttacker) {
		if (lastAttacker == null) {
			throw new IllegalArgumentException("lastAttacker");
		}
		super.onDie(lastAttacker);
		SummonsService.release(getOwner(), UnsummonType.UNSPECIFIED, isAttacked);
		Summon owner = getOwner();
		final Player master = getOwner().getMaster();
		PacketSendUtility.broadcastPacket(owner,
				new SM_EMOTION(owner, EmotionType.DIE, 0, lastAttacker.equals(owner) ? 0 : lastAttacker.getObjectId()));

		if (!master.equals(lastAttacker) && !owner.equals(lastAttacker) && !master.getLifeStats().isAlreadyDead()
				&& !lastAttacker.getLifeStats().isAlreadyDead()) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

				@Override
				public void run() {
					lastAttacker.getAggroList().addHate(master, 1);
				}
			}, 1000);
		}
	}

	/**
	 * 使用召唤物技能；成功后可按配置自动解除。
	 * Uses a summon skill; may auto-release after a successful cast.
	 *
	 * skill id
	 * skill target
	 */
	public void useSkill(int skillId, Creature target) {
		Creature creature = getOwner();
		boolean petHasSkill = DataManager.PET_SKILL_DATA.petHasSkill(getOwner().getObjectTemplate().getTemplateId(),
				skillId);
		if (!petHasSkill) {
			// 黑客！） / hackers!)
			return;
		}
		Skill skill = GameEngineServices.skillEngine().getSkill(creature, skillId, 1, target);
		if (skill != null) {
			// 技能成功时，按需处理自动释放 / If skill succeeds, handle automatic release if expected
			if (skill.useSkill() && skillId == releaseAfterSkill) {
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

					@Override
					public void run() {
						SummonsService.release(getOwner(), UnsummonType.UNSPECIFIED, isAttacked);
					}
				}, 1000);
			}
			setReleaseAfterSkill(-1);
		}
	}

	/**
	 * 设置使用后自动解除的技能 ID（通常为终极技）。
	 * Sets the skill id after which the summon auto-releases (typically an ultra skill).
	 *
	 * @param skillId 技能 ID，-1 表示取消 / skill id, -1 to clear
	 */
	public void setReleaseAfterSkill(int skillId) {
		this.releaseAfterSkill = skillId;
	}

	/**
	 * 开始移动时通知观察者并加入移动任务管理。
	 * On move start, notifies observers and registers with the move task manager.
	 */
	@Override
	public void onStartMove() {
		super.onStartMove();
		getOwner().getMoveController().setInMove(true);
		getOwner().getObserveController().notifyMoveObservers();
		GameMovementLoopServices.playerMoveTaskManager().addPlayer(getOwner());
	}

	/**
	 * 停止移动时从任务管理移除并通知观察者。
	 * On move stop, unregisters from the task manager and notifies observers.
	 */
	@Override
	public void onStopMove() {
		super.onStopMove();
		GameMovementLoopServices.playerMoveTaskManager().removePlayer(getOwner());
		getOwner().getObserveController().notifyMoveObservers();
		getOwner().getMoveController().setInMove(false);
	}

	/**
	 * 移动过程中通知移动观察者。
	 * Notifies move observers during movement.
	 */
	@Override
	public void onMove() {
		getOwner().getObserveController().notifyMoveObservers();
		super.onMove();
	}

	/**
	 * 获取召唤物主人。
	 * Gets the summon's master.
	 *
	 * master player
	 */
	protected Player getMaster() {
		return getOwner().getMaster();
	}
}
